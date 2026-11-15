package com.example.data

enum class VerificationTier(val label: String) {
    VERIFIED_AOSP_API("Verified AOSP / Android API"),
    CANDIDATE_ONE_UI("Candidate One UI (Needs APK Verify)")
}

enum class HookBindState(val label: String) {
    STANDBY("Standby (Not in SystemUI)"),
    HOOKED("Hooked & Active"),
    NOT_FOUND("Class Absent (Skipped Safely)"),
    BYPASSED_SAFETY("Bypassed by Safety Guard")
}

data class HookDescriptor(
    val id: String,
    val targetClass: String,
    val targetMethod: String,
    val tier: VerificationTier,
    val purpose: String,
    val bindState: HookBindState = HookBindState.STANDBY
)

data class RuntimeDiagnostics(
    val moduleActiveInLsposed: Boolean = false,
    val systemUiHooked: Boolean = false,
    val hookedPackage: String = "com.android.systemui",
    val activeHookCount: Int = 0,
    val discoveredClasses: List<String> = emptyList(),
    val missingCandidateClasses: List<String> = emptyList(),
    val displayRefreshHz: Float = 120f,
    val lastFrameDurationMs: Float = 0f,
    val circuitBreakerTripped: Boolean = false,
    val circuitBreakerReason: String = "",
    val lastUpdatedEpochMs: Long = 0L
)
