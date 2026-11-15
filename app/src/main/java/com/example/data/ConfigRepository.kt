package com.example.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persists user settings across SystemUI restarts, device reboots, and locked-device
 * startup using:
 * 1) LSPosed New XSharedPreferences (MODE_WORLD_READABLE with safe MODE_PRIVATE fallback)
 * 2) Device-Protected Storage mirror for Direct Boot ConfigBridgeProvider reads
 * 3) Synchronous commit() before soft-restarting SystemUI
 */
class ConfigRepository(context: Context) {

    private val appContext = context.applicationContext
    private val deviceProtectedContext: Context =
        appContext.createDeviceProtectedStorageContext()

    private val prefs: SharedPreferences = openCompatiblePreferences(appContext)
    private val directBootPrefs: SharedPreferences =
        deviceProtectedContext.getSharedPreferences(MotionConfig.PREFS_NAME, Context.MODE_PRIVATE)
    private val diagPrefs: SharedPreferences =
        deviceProtectedContext.getSharedPreferences(DIAG_PREFS_NAME, Context.MODE_PRIVATE)

    private val _config = MutableStateFlow(readConfigFromPrefs())
    val config: StateFlow<MotionConfig> = _config.asStateFlow()

    private val _diagnostics = MutableStateFlow(readDiagnosticsFromPrefs())
    val diagnostics: StateFlow<RuntimeDiagnostics> = _diagnostics.asStateFlow()

    fun selectPreset(presetId: ControlCenterPresetId, commitSync: Boolean = true): MotionConfig {
        return updateConfig(commitSync = commitSync) { current ->
            if (presetId == ControlCenterPresetId.STOCK_SAMSUNG) {
                current.copy(
                    selectedPresetId = ControlCenterPresetId.STOCK_SAMSUNG,
                    masterEnabled = false
                )
            } else {
                current.copy(
                    selectedPresetId = presetId,
                    masterEnabled = true,
                    openingEnabled = true,
                    closingEnabled = true
                )
            }
        }
    }

    fun updateConfig(
        commitSync: Boolean = false,
        transform: (MotionConfig) -> MotionConfig
    ): MotionConfig {
        val next = transform(_config.value).let {
            it.copy(
                speedMultiplier = it.clampedSpeed,
                springIntensity = it.clampedSpring,
                configVersion = System.currentTimeMillis()
            )
        }
        writeConfigToPrefs(next, commitSync = commitSync)
        _config.value = next
        return next
    }

    fun resetToVideoDefaults() {
        updateConfig(commitSync = true) {
            MotionConfig.REFERENCE_VIDEO_DEFAULTS.copy(
                safeRecoveryTripped = false,
                lastCrashReason = ""
            )
        }
    }

    fun restoreOriginalSamsungAnimation() {
        updateConfig(commitSync = true) {
            MotionConfig.ORIGINAL_SAMSUNG_RESTORE.copy(
                safeRecoveryTripped = false,
                lastCrashReason = ""
            )
        }
    }

    fun clearSafetyBreaker() {
        updateConfig(commitSync = true) {
            it.copy(
                safeRecoveryTripped = false,
                lastCrashReason = ""
            )
        }
        diagPrefs.edit()
            .putBoolean(DIAG_KEY_BREAKER_TRIPPED, false)
            .putString(DIAG_KEY_BREAKER_REASON, "")
            .commit()
        refreshDiagnostics()
    }

    fun refreshDiagnostics() {
        _diagnostics.value = readDiagnosticsFromPrefs()
    }

    fun recordSystemUiDiagnostics(
        discoveredClasses: List<String>,
        missingClasses: List<String>,
        activeHookCount: Int,
        refreshHz: Float,
        frameMs: Float,
        breakerTripped: Boolean,
        breakerReason: String
    ) {
        diagPrefs.edit()
            .putBoolean(DIAG_KEY_SYSTEMUI_HOOKED, true)
            .putInt(DIAG_KEY_ACTIVE_HOOK_COUNT, activeHookCount)
            .putString(DIAG_KEY_DISCOVERED_CLASSES, discoveredClasses.joinToString("\n"))
            .putString(DIAG_KEY_MISSING_CLASSES, missingClasses.joinToString("\n"))
            .putFloat(DIAG_KEY_REFRESH_HZ, refreshHz)
            .putFloat(DIAG_KEY_LAST_FRAME_MS, frameMs)
            .putBoolean(DIAG_KEY_BREAKER_TRIPPED, breakerTripped)
            .putString(DIAG_KEY_BREAKER_REASON, breakerReason)
            .putLong(DIAG_KEY_LAST_UPDATED_MS, System.currentTimeMillis())
            .apply()

        if (breakerTripped) {
            updateConfig(commitSync = true) {
                it.copy(
                    masterEnabled = false,
                    safeRecoveryTripped = true,
                    lastCrashReason = breakerReason
                )
            }
        }
        refreshDiagnostics()
    }

    /**
     * Reads from primary world-readable prefs if available, falling back to Device-Protected
     * storage during Direct Boot / locked-device startup.
     */
    private fun readConfigFromPrefs(): MotionConfig {
        val source = if (prefs.contains(MotionConfig.KEY_CONFIG_VERSION)) prefs else directBootPrefs
        val presetKey = source.getString(
            MotionConfig.KEY_SELECTED_PRESET_ID,
            ControlCenterPresetId.STYLE_01_CORNER_CASCADE.storageKey
        )
        return MotionConfig(
            selectedPresetId = ControlCenterPresetId.fromKey(presetKey),
            masterEnabled = source.getBoolean(MotionConfig.KEY_MASTER_ENABLED, true),
            openingEnabled = source.getBoolean(MotionConfig.KEY_OPENING_ENABLED, true),
            closingEnabled = source.getBoolean(MotionConfig.KEY_CLOSING_ENABLED, true),
            speedMultiplier = source.getFloat(
                MotionConfig.KEY_SPEED_MULTIPLIER,
                MotionConfig.DEFAULT_SPEED_MULTIPLIER
            ),
            springIntensity = source.getFloat(
                MotionConfig.KEY_SPRING_INTENSITY,
                MotionConfig.DEFAULT_SPRING_INTENSITY
            ),
            safeRecoveryTripped = source.getBoolean(MotionConfig.KEY_SAFE_RECOVERY_TRIPPED, false),
            lastCrashReason = source.getString(MotionConfig.KEY_LAST_CRASH_REASON, "") ?: "",
            configVersion = source.getLong(MotionConfig.KEY_CONFIG_VERSION, 1L)
        )
    }

    @SuppressLint("ApplySharedPref")
    private fun writeConfigToPrefs(cfg: MotionConfig, commitSync: Boolean) {
        val applyOrCommit: SharedPreferences.Editor.() -> Unit = {
            putString(MotionConfig.KEY_SELECTED_PRESET_ID, cfg.selectedPresetId.storageKey)
            putBoolean(MotionConfig.KEY_MASTER_ENABLED, cfg.masterEnabled)
            putBoolean(MotionConfig.KEY_OPENING_ENABLED, cfg.openingEnabled)
            putBoolean(MotionConfig.KEY_CLOSING_ENABLED, cfg.closingEnabled)
            putFloat(MotionConfig.KEY_SPEED_MULTIPLIER, cfg.clampedSpeed)
            putFloat(MotionConfig.KEY_SPRING_INTENSITY, cfg.clampedSpring)
            putBoolean(MotionConfig.KEY_SAFE_RECOVERY_TRIPPED, cfg.safeRecoveryTripped)
            putString(MotionConfig.KEY_LAST_CRASH_REASON, cfg.lastCrashReason)
            putLong(MotionConfig.KEY_CONFIG_VERSION, cfg.configVersion)
            if (commitSync) commit() else apply()
        }
        prefs.edit().applyOrCommit()
        directBootPrefs.edit().applyOrCommit()
    }

    private fun readDiagnosticsFromPrefs(): RuntimeDiagnostics {
        val discoveredRaw = diagPrefs.getString(DIAG_KEY_DISCOVERED_CLASSES, "") ?: ""
        val missingRaw = diagPrefs.getString(DIAG_KEY_MISSING_CLASSES, "") ?: ""
        return RuntimeDiagnostics(
            moduleActiveInLsposed = ModuleStatusProbe.isModuleActive(),
            systemUiHooked = diagPrefs.getBoolean(DIAG_KEY_SYSTEMUI_HOOKED, false),
            activeHookCount = diagPrefs.getInt(DIAG_KEY_ACTIVE_HOOK_COUNT, 0),
            discoveredClasses = discoveredRaw.split('\n').filter { it.isNotBlank() },
            missingCandidateClasses = missingRaw.split('\n').filter { it.isNotBlank() },
            displayRefreshHz = diagPrefs.getFloat(DIAG_KEY_REFRESH_HZ, 120f),
            lastFrameDurationMs = diagPrefs.getFloat(DIAG_KEY_LAST_FRAME_MS, 0f),
            circuitBreakerTripped = diagPrefs.getBoolean(DIAG_KEY_BREAKER_TRIPPED, false),
            circuitBreakerReason = diagPrefs.getString(DIAG_KEY_BREAKER_REASON, "") ?: "",
            lastUpdatedEpochMs = diagPrefs.getLong(DIAG_KEY_LAST_UPDATED_MS, 0L)
        )
    }

    companion object {
        private const val DIAG_PREFS_NAME = "ultra_motion_diag"
        private const val DIAG_KEY_SYSTEMUI_HOOKED = "systemui_hooked"
        private const val DIAG_KEY_ACTIVE_HOOK_COUNT = "active_hook_count"
        private const val DIAG_KEY_DISCOVERED_CLASSES = "discovered_classes"
        private const val DIAG_KEY_MISSING_CLASSES = "missing_classes"
        private const val DIAG_KEY_REFRESH_HZ = "refresh_hz"
        private const val DIAG_KEY_LAST_FRAME_MS = "last_frame_ms"
        private const val DIAG_KEY_BREAKER_TRIPPED = "breaker_tripped"
        private const val DIAG_KEY_BREAKER_REASON = "breaker_reason"
        private const val DIAG_KEY_LAST_UPDATED_MS = "last_updated_ms"

        @Suppress("DEPRECATION")
        @SuppressLint("WorldReadableFiles")
        fun openCompatiblePreferences(context: Context): SharedPreferences {
            return try {
                context.getSharedPreferences(MotionConfig.PREFS_NAME, Context.MODE_WORLD_READABLE)
            } catch (_: SecurityException) {
                context.getSharedPreferences(MotionConfig.PREFS_NAME, Context.MODE_PRIVATE)
            }
        }
    }
}

object ModuleStatusProbe {
    @JvmStatic
    fun isModuleActive(): Boolean = false
}
