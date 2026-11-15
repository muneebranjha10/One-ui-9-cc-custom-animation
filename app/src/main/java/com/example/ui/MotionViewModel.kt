package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AnimationPreset
import com.example.data.AnimationPresetRegistry
import com.example.data.ConfigRepository
import com.example.data.ControlCenterPresetId
import com.example.data.DeviceEnvironmentInfo
import com.example.data.HookDescriptor
import com.example.data.MotionConfig
import com.example.data.RuntimeDiagnostics
import com.example.data.SystemControlHelper
import com.example.xposed.HookVerificationRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SoftRestartFeedback {
    data class Success(val title: String, val message: String) : SoftRestartFeedback
    data class Failure(
        val title: String,
        val error: String,
        val troubleshooting: String
    ) : SoftRestartFeedback
}

class MotionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ConfigRepository(application)

    val config: StateFlow<MotionConfig> = repository.config
    val diagnostics: StateFlow<RuntimeDiagnostics> = repository.diagnostics

    val hookDescriptors: StateFlow<List<HookDescriptor>> = diagnostics
        .map { HookVerificationRegistry.resolveWithDiagnostics(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HookVerificationRegistry.baselineHooks
        )

    private val _deviceInfo = MutableStateFlow(SystemControlHelper.inspectDevice(application))
    val deviceInfo: StateFlow<DeviceEnvironmentInfo> = _deviceInfo.asStateFlow()

    private val _previewPreset = MutableStateFlow<AnimationPreset?>(null)
    val previewPreset: StateFlow<AnimationPreset?> = _previewPreset.asStateFlow()

    private val _restartFeedback = MutableStateFlow<SoftRestartFeedback?>(null)
    val restartFeedback: StateFlow<SoftRestartFeedback?> = _restartFeedback.asStateFlow()

    private val _isRestartingSystemUi = MutableStateFlow(false)
    val isRestartingSystemUi: StateFlow<Boolean> = _isRestartingSystemUi.asStateFlow()

    private var lastRestartAttemptMs = 0L

    fun selectPreset(presetId: ControlCenterPresetId) {
        repository.selectPreset(presetId, commitSync = true)
    }

    fun openPresetPreview(preset: AnimationPreset) {
        _previewPreset.value = preset
    }

    fun dismissPresetPreview() {
        _previewPreset.value = null
    }

    fun setMasterEnabled(enabled: Boolean) {
        repository.updateConfig(commitSync = true) { it.copy(masterEnabled = enabled) }
    }

    fun setOpeningEnabled(enabled: Boolean) {
        repository.updateConfig(commitSync = true) { it.copy(openingEnabled = enabled) }
    }

    fun setClosingEnabled(enabled: Boolean) {
        repository.updateConfig(commitSync = true) { it.copy(closingEnabled = enabled) }
    }

    fun setSpeedMultiplier(speed: Float) {
        val snapped = (kotlin.math.round(speed * 20f) / 20f)
            .coerceIn(MotionConfig.MIN_SPEED, MotionConfig.MAX_SPEED)
        repository.updateConfig { it.copy(speedMultiplier = snapped) }
    }

    fun setSpringIntensity(intensity: Float) {
        val snapped = (kotlin.math.round(intensity * 20f) / 20f)
            .coerceIn(MotionConfig.MIN_SPRING, MotionConfig.MAX_SPRING)
        repository.updateConfig { it.copy(springIntensity = snapped) }
    }

    fun resetToVideoDefaults() {
        repository.resetToVideoDefaults()
    }

    fun restoreOriginalSamsungAnimation() {
        repository.restoreOriginalSamsungAnimation()
    }

    fun clearSafetyBreaker() {
        repository.clearSafetyBreaker()
    }

    fun refreshDiagnostics() {
        _deviceInfo.value = SystemControlHelper.inspectDevice(getApplication())
        repository.refreshDiagnostics()
    }

    /**
     * Step 1: Synchronously commit selected preset so SystemUI reads it on restart.
     * Step 2: Prevent rapid/looping restarts via cooldown guard.
     * Step 3: Trigger su killall com.android.systemui (ONLY after explicit user dialog confirmation).
     */
    fun confirmApplyAndSoftRestart() {
        if (_isRestartingSystemUi.value) return
        val now = System.currentTimeMillis()
        if (now - lastRestartAttemptMs < RESTART_COOLDOWN_MS) {
            _restartFeedback.value = SoftRestartFeedback.Failure(
                title = "Restart Cooldown Active",
                error = "SystemUI restart was just triggered. Wait a few seconds to prevent restart loops.",
                troubleshooting = "Your preset (${config.value.activePreset.title}) is already saved."
            )
            return
        }
        lastRestartAttemptMs = now

        // Flush config synchronously before restarting SystemUI
        val saved = repository.selectPreset(config.value.selectedPresetId, commitSync = true)

        viewModelScope.launch {
            _isRestartingSystemUi.value = true
            val result = SystemControlHelper.restartSystemUiWithRoot()
            _restartFeedback.value = when (result) {
                is SystemControlHelper.CommandResult.Success -> SoftRestartFeedback.Success(
                    title = "Applied: ${saved.activePreset.title}",
                    message = result.message
                )
                is SystemControlHelper.CommandResult.Failure -> SoftRestartFeedback.Failure(
                    title = "Preset Saved · Restart Needs Root",
                    error = result.error,
                    troubleshooting = result.troubleshooting
                )
            }
            _isRestartingSystemUi.value = false
        }
    }

    fun dismissRestartFeedback() {
        _restartFeedback.value = null
    }

    fun allVideoPresets(): List<AnimationPreset> = AnimationPresetRegistry.videoPresets

    companion object {
        private const val RESTART_COOLDOWN_MS = 4000L
    }
}
