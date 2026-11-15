package com.example.xposed

import com.example.data.HookBindState
import com.example.data.HookDescriptor
import com.example.data.RuntimeDiagnostics
import com.example.data.VerificationTier

/**
 * Honest verification ledger of all SystemUI hook targets.
 * Explicitly separates standard AOSP / Android SDK APIs from candidate Samsung
 * One UI classes that must be verified at runtime or against One UI 9 SystemUI.apk.
 */
object HookVerificationRegistry {

    val baselineHooks: List<HookDescriptor> = listOf(
        HookDescriptor(
            id = "aosp_qs_fragment",
            targetClass = "com.android.systemui.qs.QSFragment",
            targetMethod = "setQsExpansion(float, float, float, float)",
            tier = VerificationTier.VERIFIED_AOSP_API,
            purpose = "Tracks live Quick Settings expansion fraction (0f..1f) and attaches tier animator to QS view root."
        ),
        HookDescriptor(
            id = "aosp_qs_impl",
            targetClass = "com.android.systemui.qs.QSImpl",
            targetMethod = "setQsExpansion(float, float, float, float)",
            tier = VerificationTier.VERIFIED_AOSP_API,
            purpose = "Android 14-17 AOSP QSImpl replacement for QSFragment; delivers continuous QS expansion callbacks."
        ),
        HookDescriptor(
            id = "aosp_shade_npvc",
            targetClass = "com.android.systemui.shade.NotificationPanelViewController",
            targetMethod = "onPanelExpansionChanged / updateExpansion",
            tier = VerificationTier.VERIFIED_AOSP_API,
            purpose = "Detects drag vs fling tracking state and shade velocity without touching keyguard or auth methods."
        ),
        HookDescriptor(
            id = "aosp_shade_qs_controller",
            targetClass = "com.android.systemui.shade.QuickSettingsControllerImpl",
            targetMethod = "setExpansionHeight(float) / onHeightChanged",
            tier = VerificationTier.VERIFIED_AOSP_API,
            purpose = "Provides QS container height and tracking state on modern AOSP SystemUI builds."
        ),
        HookDescriptor(
            id = "oneui_sec_qs_impl",
            targetClass = "com.android.systemui.qs.SecQSImpl",
            targetMethod = "setQsExpansion / getQsPanelController",
            tier = VerificationTier.CANDIDATE_ONE_UI,
            purpose = "Samsung One UI Quick Panel implementation (requires runtime/APK verification on One UI 9)."
        ),
        HookDescriptor(
            id = "oneui_sec_panel_notifier",
            targetClass = "com.android.systemui.shade.SecPanelExpansionStateNotifier",
            targetMethod = "notifyPanelExpansionState / registerCallback",
            tier = VerificationTier.CANDIDATE_ONE_UI,
            purpose = "Samsung One UI split Control Center / Notification Shade state notifier (requires One UI 9 APK verification)."
        ),
        HookDescriptor(
            id = "oneui_sec_qs_animator",
            targetClass = "com.android.systemui.qs.animator.SecQsTransitionAnimator",
            targetMethod = "onPanelExpansionChanged / updateAnimators",
            tier = VerificationTier.CANDIDATE_ONE_UI,
            purpose = "Samsung One UI Quick Panel tile transition coordinator (requires One UI 9 APK verification)."
        )
    )

    fun resolveWithDiagnostics(diag: RuntimeDiagnostics): List<HookDescriptor> {
        val discoveredSet = diag.discoveredClasses.toSet()
        val missingSet = diag.missingCandidateClasses.toSet()
        return baselineHooks.map { hook ->
            val state = when {
                diag.circuitBreakerTripped -> HookBindState.BYPASSED_SAFETY
                hook.targetClass in discoveredSet -> HookBindState.HOOKED
                hook.targetClass in missingSet -> HookBindState.NOT_FOUND
                else -> HookBindState.STANDBY
            }
            hook.copy(bindState = state)
        }
    }
}
