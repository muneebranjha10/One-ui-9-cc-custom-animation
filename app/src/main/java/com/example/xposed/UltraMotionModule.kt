package com.example.xposed

import android.app.AndroidAppHelper
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import com.example.data.ConfigBridgeProvider
import com.example.data.ControlCenterPresetId
import com.example.data.MotionConfig
import com.example.motion.ControlCenterAnimator
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.lang.reflect.Method

/**
 * Genuine LSPosed / Xposed entry point targeting com.android.systemui and
 * self-hooking ModuleStatusProbe inside the companion app.
 */
class UltraMotionModule : IXposedHookLoadPackage {

    @Volatile
    private var circuitBreakerTripped = false

    @Volatile
    private var lastConfigSyncMs = 0L

    @Volatile
    private var cachedConfig = MotionConfig.REFERENCE_VIDEO_DEFAULTS

    private var xSharedPrefs: XSharedPreferences? = null
    private var lastReportedDiagnosticsMs = 0L
    private var latestFrameMs = 0f

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        when (lpparam.packageName) {
            MODULE_PACKAGE_NAME -> hookSelfStatus(lpparam.classLoader)
            SYSTEMUI_PACKAGE_NAME -> hookSystemUi(lpparam.classLoader)
        }
    }

    private fun hookSelfStatus(classLoader: ClassLoader) {
        runCatching {
            val probeClass = XposedHelpers.findClassIfExists(
                "com.example.data.ModuleStatusProbe",
                classLoader
            ) ?: return
            XposedHelpers.findAndHookMethod(
                probeClass,
                "isModuleActive",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.result = true
                    }
                }
            )
        }.onFailure { XposedBridge.log("$TAG: self-hook failed: ${it.message}") }
    }

    private fun hookSystemUi(classLoader: ClassLoader) {
        val probe = SystemUiProbe.inspect(classLoader)
        XposedBridge.log(
            "$TAG: SystemUI probe complete. Found=${probe.discovered.keys}, Missing=${probe.missing}"
        )

        val prefs = XSharedPreferences(MODULE_PACKAGE_NAME, MotionConfig.PREFS_NAME).apply {
            makeWorldReadable()
            reload()
        }
        xSharedPrefs = prefs
        cachedConfig = readXSharedConfig(prefs)

        var activeHookCount = 0
        val animator = ControlCenterAnimator(
            onSafetyTrip = { reason ->
                circuitBreakerTripped = true
                XposedBridge.log("$TAG: SAFETY CIRCUIT BREAKER TRIPPED: $reason")
                pushDiagnosticsAsync(
                    discovered = probe.discovered.keys.toList(),
                    missing = probe.missing,
                    activeHookCount = activeHookCount,
                    breakerTripped = true,
                    breakerReason = reason
                )
            },
            onFrameTelemetry = { frameMs -> latestFrameMs = frameMs }
        )

        // Bind safe expansion & QS view hooks without touching Keyguard, Biometrics, or Notifications
        for ((className, clazz) in probe.discovered) {
            activeHookCount += bindSafeHooksForClass(
                className = className,
                clazz = clazz,
                animator = animator,
                probe = probe
            )
        }

        XposedBridge.log("$TAG: Installed $activeHookCount safe SystemUI hooks.")
        pushDiagnosticsAsync(
            discovered = probe.discovered.keys.toList(),
            missing = probe.missing,
            activeHookCount = activeHookCount,
            breakerTripped = false,
            breakerReason = ""
        )
    }

    private fun bindSafeHooksForClass(
        className: String,
        clazz: Class<*>,
        animator: ControlCenterAnimator,
        probe: SystemUiProbe.ProbeReport
    ): Int {
        var hooksBound = 0
        val methods: Array<Method> = runCatching { clazz.declaredMethods }.getOrElse { emptyArray() }

        for (method in methods) {
            val name = method.name
            val params = method.parameterTypes

            // 1. QS Expansion callbacks: setQsExpansion(float expansion, ...)
            if (name == "setQsExpansion" && params.isNotEmpty() && params[0] == Float::class.javaPrimitiveType) {
                runCatching {
                    XposedBridge.hookMethod(method, object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            safeExecute(animator, probe) {
                                refreshConfigIfNeeded(animator)
                                attachCandidateQsView(param.thisObject, animator)
                                val fraction = (param.args[0] as? Float) ?: return@safeExecute
                                val tracking = detectTrackingState(param.thisObject)
                                animator.onExpansionChanged(fraction.coerceIn(0f, 1f), tracking)
                                maybePushPeriodicDiagnostics(probe)
                            }
                        }
                    })
                    hooksBound++
                }
            }

            // 2. Shade panel expansion callbacks (skipping lockscreen/keyguard states)
            if ((name == "onPanelExpansionChanged" || name == "setExpansionFraction") &&
                params.isNotEmpty()
            ) {
                runCatching {
                    XposedBridge.hookMethod(method, object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            safeExecute(animator, probe) {
                                if (isKeyguardOrLockscreenActive(param.thisObject)) {
                                    animator.stopAndReset()
                                    return@safeExecute
                                }
                                refreshConfigIfNeeded(animator)
                                val fraction = extractFloatArg(param.args) ?: return@safeExecute
                                val tracking = extractBooleanArg(param.args) ?: detectTrackingState(param.thisObject)
                                animator.onExpansionChanged(fraction.coerceIn(0f, 1f), tracking)
                            }
                        }
                    })
                    hooksBound++
                }
            }
        }
        return hooksBound
    }

    private inline fun safeExecute(
        animator: ControlCenterAnimator,
        probe: SystemUiProbe.ProbeReport,
        block: () -> Unit
    ) {
        if (circuitBreakerTripped) return
        try {
            block()
        } catch (t: Throwable) {
            circuitBreakerTripped = true
            animator.stopAndReset()
            val msg = "Hook exception: ${t.javaClass.simpleName}: ${t.message}"
            XposedBridge.log("$TAG: $msg")
            pushDiagnosticsAsync(
                discovered = probe.discovered.keys.toList(),
                missing = probe.missing,
                activeHookCount = 0,
                breakerTripped = true,
                breakerReason = msg
            )
        }
    }

    private fun attachCandidateQsView(target: Any?, animator: ControlCenterAnimator) {
        target ?: return
        val candidateFields = listOf("mView", "view", "mQSPanel", "mContainer", "mQsContainer")
        for (field in candidateFields) {
            val found = runCatching { XposedHelpers.getObjectField(target, field) }.getOrNull()
            if (found is ViewGroup) {
                animator.attachRoot(found)
                return
            }
        }
        val getViewResult = runCatching { XposedHelpers.callMethod(target, "getView") }.getOrNull()
        if (getViewResult is ViewGroup) {
            animator.attachRoot(getViewResult)
        } else if (getViewResult is View) {
            val parent = getViewResult.parent
            if (parent is ViewGroup) animator.attachRoot(parent)
        }
    }

    private fun detectTrackingState(target: Any?): Boolean {
        target ?: return false
        for (field in listOf("mTracking", "mQsTracking", "isTracking")) {
            val value = runCatching { XposedHelpers.getBooleanField(target, field) }.getOrNull()
            if (value == true) return true
        }
        return false
    }

    private fun isKeyguardOrLockscreenActive(target: Any?): Boolean {
        target ?: return false
        for (field in listOf("mKeyguardShowing", "mBouncerShowing", "mDozing")) {
            val value = runCatching { XposedHelpers.getBooleanField(target, field) }.getOrNull()
            if (value == true) return true
        }
        return false
    }

    private fun extractFloatArg(args: Array<Any?>?): Float? {
        args ?: return null
        for (arg in args) {
            if (arg is Float) return arg
        }
        // Fallback: check ShadeExpansionChangeEvent-style object with .fraction
        val first = args.firstOrNull() ?: return null
        return runCatching { XposedHelpers.getFloatField(first, "fraction") }.getOrNull()
    }

    private fun extractBooleanArg(args: Array<Any?>?): Boolean? {
        args ?: return null
        for (arg in args) {
            if (arg is Boolean) return arg
        }
        val first = args.firstOrNull() ?: return null
        return runCatching { XposedHelpers.getBooleanField(first, "tracking") }.getOrNull()
    }

    private fun refreshConfigIfNeeded(animator: ControlCenterAnimator) {
        val now = System.currentTimeMillis()
        if (now - lastConfigSyncMs < 400L) return
        lastConfigSyncMs = now

        val prefs = xSharedPrefs
        if (prefs != null && prefs.hasFileChanged()) {
            prefs.reload()
            cachedConfig = readXSharedConfig(prefs)
        } else {
            // Also try live ContentProvider bridge for instant updates without reboot
            readConfigFromProvider()?.let { cachedConfig = it }
        }
        animator.updateConfig(cachedConfig)
    }

    private fun readXSharedConfig(prefs: XSharedPreferences): MotionConfig {
        val presetKey = prefs.getString(
            MotionConfig.KEY_SELECTED_PRESET_ID,
            ControlCenterPresetId.STYLE_01_CORNER_CASCADE.storageKey
        )
        return MotionConfig(
            selectedPresetId = ControlCenterPresetId.fromKey(presetKey),
            masterEnabled = prefs.getBoolean(MotionConfig.KEY_MASTER_ENABLED, true),
            openingEnabled = prefs.getBoolean(MotionConfig.KEY_OPENING_ENABLED, true),
            closingEnabled = prefs.getBoolean(MotionConfig.KEY_CLOSING_ENABLED, true),
            speedMultiplier = prefs.getFloat(MotionConfig.KEY_SPEED_MULTIPLIER, 1.0f),
            springIntensity = prefs.getFloat(MotionConfig.KEY_SPRING_INTENSITY, 1.0f),
            safeRecoveryTripped = prefs.getBoolean(MotionConfig.KEY_SAFE_RECOVERY_TRIPPED, false),
            configVersion = prefs.getLong(MotionConfig.KEY_CONFIG_VERSION, 1L)
        )
    }

    private fun readConfigFromProvider(): MotionConfig? {
        return runCatching {
            val ctx: Context = AndroidAppHelper.currentApplication() ?: return null
            val uri = Uri.parse("content://$MODULE_PACKAGE_NAME.configprovider")
            val result = ctx.contentResolver.call(uri, ConfigBridgeProvider.METHOD_GET_CONFIG, null, null)
                ?: return null
            val presetKey = result.getString(
                MotionConfig.KEY_SELECTED_PRESET_ID,
                ControlCenterPresetId.STYLE_01_CORNER_CASCADE.storageKey
            )
            MotionConfig(
                selectedPresetId = ControlCenterPresetId.fromKey(presetKey),
                masterEnabled = result.getBoolean(MotionConfig.KEY_MASTER_ENABLED, true),
                openingEnabled = result.getBoolean(MotionConfig.KEY_OPENING_ENABLED, true),
                closingEnabled = result.getBoolean(MotionConfig.KEY_CLOSING_ENABLED, true),
                speedMultiplier = result.getFloat(MotionConfig.KEY_SPEED_MULTIPLIER, 1.0f),
                springIntensity = result.getFloat(MotionConfig.KEY_SPRING_INTENSITY, 1.0f),
                safeRecoveryTripped = result.getBoolean(MotionConfig.KEY_SAFE_RECOVERY_TRIPPED, false),
                configVersion = result.getLong(MotionConfig.KEY_CONFIG_VERSION, 1L)
            )
        }.getOrNull()
    }

    private fun maybePushPeriodicDiagnostics(probe: SystemUiProbe.ProbeReport) {
        val now = System.currentTimeMillis()
        if (now - lastReportedDiagnosticsMs > 5000L) {
            lastReportedDiagnosticsMs = now
            pushDiagnosticsAsync(
                discovered = probe.discovered.keys.toList(),
                missing = probe.missing,
                activeHookCount = probe.discovered.size,
                breakerTripped = circuitBreakerTripped,
                breakerReason = ""
            )
        }
    }

    private fun pushDiagnosticsAsync(
        discovered: List<String>,
        missing: List<String>,
        activeHookCount: Int,
        breakerTripped: Boolean,
        breakerReason: String
    ) {
        runCatching {
            val ctx: Context = AndroidAppHelper.currentApplication() ?: return
            val uri = Uri.parse("content://$MODULE_PACKAGE_NAME.configprovider")
            val bundle = Bundle().apply {
                putStringArrayList(ConfigBridgeProvider.EXTRA_DISCOVERED, ArrayList(discovered))
                putStringArrayList(ConfigBridgeProvider.EXTRA_MISSING, ArrayList(missing))
                putInt(ConfigBridgeProvider.EXTRA_HOOK_COUNT, activeHookCount)
                putFloat(ConfigBridgeProvider.EXTRA_REFRESH_HZ, 120f)
                putFloat(ConfigBridgeProvider.EXTRA_FRAME_MS, latestFrameMs)
                putBoolean(ConfigBridgeProvider.EXTRA_BREAKER_TRIPPED, breakerTripped)
                putString(ConfigBridgeProvider.EXTRA_BREAKER_REASON, breakerReason)
            }
            ctx.contentResolver.call(uri, ConfigBridgeProvider.METHOD_REPORT_DIAGNOSTICS, null, bundle)
        }
    }

    companion object {
        private const val TAG = "UltraMotionCC"
        private const val MODULE_PACKAGE_NAME = "com.aistudio.ultramotion.s911bx"
        private const val SYSTEMUI_PACKAGE_NAME = "com.android.systemui"
    }
}
