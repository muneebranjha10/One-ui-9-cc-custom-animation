package com.example.data

/**
 * Immutable configuration shared between the companion Settings UI and the
 * LSPosed SystemUI hook via XSharedPreferences + Direct-Boot ConfigBridgeProvider.
 */
data class MotionConfig(
    val selectedPresetId: ControlCenterPresetId = ControlCenterPresetId.STYLE_01_CORNER_CASCADE,
    val masterEnabled: Boolean = true,
    val openingEnabled: Boolean = true,
    val closingEnabled: Boolean = true,
    val speedMultiplier: Float = DEFAULT_SPEED_MULTIPLIER,
    val springIntensity: Float = DEFAULT_SPRING_INTENSITY,
    val safeRecoveryTripped: Boolean = false,
    val lastCrashReason: String = "",
    val configVersion: Long = 1L
) {
    val activePreset: AnimationPreset
        get() = if (!masterEnabled || selectedPresetId == ControlCenterPresetId.STOCK_SAMSUNG) {
            AnimationPresetRegistry.stockSamsungPreset
        } else {
            AnimationPresetRegistry.findById(selectedPresetId)
        }

    val isCustomMotionActive: Boolean
        get() = masterEnabled &&
            !safeRecoveryTripped &&
            selectedPresetId != ControlCenterPresetId.STOCK_SAMSUNG

    val clampedSpeed: Float
        get() = speedMultiplier.coerceIn(MIN_SPEED, MAX_SPEED)

    val clampedSpring: Float
        get() = springIntensity.coerceIn(MIN_SPRING, MAX_SPRING)

    companion object {
        const val PREFS_NAME = "ultra_motion_prefs"

        const val KEY_SELECTED_PRESET_ID = "selected_preset_id"
        const val KEY_MASTER_ENABLED = "master_enabled"
        const val KEY_OPENING_ENABLED = "opening_enabled"
        const val KEY_CLOSING_ENABLED = "closing_enabled"
        const val KEY_SPEED_MULTIPLIER = "speed_multiplier"
        const val KEY_SPRING_INTENSITY = "spring_intensity"
        const val KEY_SAFE_RECOVERY_TRIPPED = "safe_recovery_tripped"
        const val KEY_LAST_CRASH_REASON = "last_crash_reason"
        const val KEY_CONFIG_VERSION = "config_version"

        const val DEFAULT_SPEED_MULTIPLIER = 1.0f
        const val DEFAULT_SPRING_INTENSITY = 1.0f
        const val MIN_SPEED = 0.50f
        const val MAX_SPEED = 2.00f
        const val MIN_SPRING = 0.00f
        const val MAX_SPRING = 1.50f

        val REFERENCE_VIDEO_DEFAULTS = MotionConfig()

        val ORIGINAL_SAMSUNG_RESTORE = MotionConfig(
            selectedPresetId = ControlCenterPresetId.STOCK_SAMSUNG,
            masterEnabled = false,
            openingEnabled = false,
            closingEnabled = false,
            speedMultiplier = DEFAULT_SPEED_MULTIPLIER,
            springIntensity = DEFAULT_SPRING_INTENSITY
        )
    }
}
