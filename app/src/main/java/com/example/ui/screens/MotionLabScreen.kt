package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AnimationPreset
import com.example.data.AnimationPresetRegistry
import com.example.data.ControlCenterPresetId
import com.example.data.DeviceEnvironmentInfo
import com.example.data.MotionConfig
import com.example.data.RuntimeDiagnostics
import com.example.ui.SoftRestartFeedback
import com.example.ui.components.PresetPreviewDialog
import com.example.ui.theme.AmberCandidate
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CobaltPrimaryContainer
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.CoralContainer
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun MotionLabScreen(
    config: MotionConfig,
    presets: List<AnimationPreset>,
    previewPreset: AnimationPreset?,
    diagnostics: RuntimeDiagnostics,
    deviceInfo: DeviceEnvironmentInfo,
    isRestartingSystemUi: Boolean,
    restartFeedback: SoftRestartFeedback?,
    onSelectPreset: (ControlCenterPresetId) -> Unit,
    onOpenPreview: (AnimationPreset) -> Unit,
    onDismissPreview: () -> Unit,
    onOpeningChanged: (Boolean) -> Unit,
    onClosingChanged: (Boolean) -> Unit,
    onSpeedChanged: (Float) -> Unit,
    onSpringChanged: (Float) -> Unit,
    onResetDefaults: () -> Unit,
    onClearBreaker: () -> Unit,
    onConfirmApplyAndSoftRestart: () -> Unit,
    onDismissRestartFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showRestartConfirmDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                StatusHeroCard(
                    config = config,
                    diagnostics = diagnostics,
                    deviceInfo = deviceInfo,
                    onClearBreaker = onClearBreaker
                )

                Text(
                    text = "Reference Video Animations",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "5 distinct Control Center animations extracted from the 60fps reference video. Select one active preset, preview its motion, then tap Apply & Soft Restart.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                presets.forEach { preset ->
                    val isSelected = config.masterEnabled && config.selectedPresetId == preset.id
                    PresetCard(
                        preset = preset,
                        selected = isSelected,
                        onSelect = { onSelectPreset(preset.id) },
                        onPreview = { onOpenPreview(preset) }
                    )
                }

                // Separate Stock Samsung Animation option
                StockSamsungCard(
                    selected = !config.masterEnabled || config.selectedPresetId == ControlCenterPresetId.STOCK_SAMSUNG,
                    onSelect = { onSelectPreset(ControlCenterPresetId.STOCK_SAMSUNG) }
                )

                // Fine-Tuning Controls
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = ObsidianSurface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, ObsidianOutline, MaterialTheme.shapes.large)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Tune, contentDescription = null, tint = CyanAccent)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Physics Fine-Tuning",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            TextButton(
                                onClick = onResetDefaults,
                                modifier = Modifier.testTag("reset_defaults_button")
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Reset Defaults")
                            }
                        }

                        ToggleRow(
                            title = "Enable Opening Animation",
                            subtitle = "Apply selected preset when pulling down Control Center",
                            checked = config.openingEnabled,
                            enabled = config.isCustomMotionActive,
                            onCheckedChange = onOpeningChanged,
                            testTag = "opening_animation_switch"
                        )

                        ToggleRow(
                            title = "Enable Closing Animation",
                            subtitle = "Apply selected preset's closing curve when dismissing",
                            checked = config.closingEnabled,
                            enabled = config.isCustomMotionActive,
                            onCheckedChange = onClosingChanged,
                            testTag = "closing_animation_switch"
                        )

                        HorizontalDivider(color = ObsidianOutline)

                        SliderControl(
                            label = "Animation Speed",
                            valueText = "${"%.2f".format(config.clampedSpeed)}×",
                            value = config.clampedSpeed,
                            valueRange = MotionConfig.MIN_SPEED..MotionConfig.MAX_SPEED,
                            enabled = config.isCustomMotionActive,
                            onValueChange = onSpeedChanged,
                            helperText = "1.00× matches the recorded video timing.",
                            testTag = "speed_slider"
                        )

                        SliderControl(
                            label = "Opening Spring Intensity",
                            valueText = "${(config.clampedSpring * 100).roundToInt()}%",
                            value = config.clampedSpring,
                            valueRange = MotionConfig.MIN_SPRING..MotionConfig.MAX_SPRING,
                            enabled = config.isCustomMotionActive && config.openingEnabled,
                            onValueChange = onSpringChanged,
                            helperText = "Adjusts opening spring overshoot (closing remains clean & bounce-free).",
                            testTag = "spring_slider"
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))
            }

            // Prominent Apply & Soft Restart sticky bottom action bar
            Surface(
                color = ObsidianSurface,
                tonalElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ObsidianOutline, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Active Selection: ${config.activePreset.title}",
                            style = MaterialTheme.typography.labelMedium,
                            color = CyanAccent,
                            maxLines = 1
                        )
                        Text(
                            text = if (deviceInfo.rootBinaryDetected) "su Ready" else "su Check on Tap",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (deviceInfo.rootBinaryDetected) EmeraldVerified else TextSecondary
                        )
                    }

                    Button(
                        onClick = { showRestartConfirmDialog = true },
                        enabled = !isRestartingSystemUi,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CobaltPrimary,
                            contentColor = Color(0xFF041022)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("apply_soft_restart_button")
                    ) {
                        Icon(Icons.Rounded.RestartAlt, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isRestartingSystemUi) "Saving & Restarting SystemUI..." else "Apply & Soft Restart",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (previewPreset != null) {
        PresetPreviewDialog(
            preset = previewPreset,
            baseConfig = config,
            onSelectPreset = { onSelectPreset(previewPreset.id) },
            onDismiss = onDismissPreview
        )
    }

    if (showRestartConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRestartConfirmDialog = false },
            title = { Text("Apply & Soft Restart SystemUI?") },
            text = {
                Text(
                    "Your selected animation (${config.activePreset.title}) will be saved and loaded by the LSPosed module.\n\n" +
                        "Restarting only com.android.systemui via KernelSU root will cause your status bar, navigation interface, and Control Center to briefly disappear for 2–3 seconds. Your phone will NOT reboot. Continue?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestartConfirmDialog = false
                        onConfirmApplyAndSoftRestart()
                    },
                    modifier = Modifier.testTag("confirm_apply_soft_restart_button")
                ) {
                    Text("Restart SystemUI")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRestartConfirmDialog = false },
                    modifier = Modifier.testTag("cancel_apply_soft_restart_button")
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (restartFeedback != null) {
        val (title, body) = when (restartFeedback) {
            is SoftRestartFeedback.Success -> restartFeedback.title to restartFeedback.message
            is SoftRestartFeedback.Failure -> restartFeedback.title to
                "${restartFeedback.error}\n\nTroubleshooting:\n${restartFeedback.troubleshooting}"
        }
        AlertDialog(
            onDismissRequest = onDismissRestartFeedback,
            title = { Text(title) },
            text = { Text(body) },
            confirmButton = {
                TextButton(onClick = onDismissRestartFeedback) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun PresetCard(
    preset: AnimationPreset,
    selected: Boolean,
    onSelect: () -> Unit,
    onPreview: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (selected) CobaltPrimary else ObsidianOutline,
        label = "presetBorder"
    )
    val cardColor by animateColorAsState(
        targetValue = if (selected) CobaltPrimaryContainer.copy(alpha = 0.42f) else ObsidianSurface,
        label = "presetSurface"
    )

    Surface(
        shape = MaterialTheme.shapes.large,
        color = cardColor,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .border(if (selected) 1.5.dp else 1.dp, borderColor, MaterialTheme.shapes.large)
            .clickable(onClick = onSelect)
            .testTag("preset_card_${preset.id.storageKey}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                        contentDescription = if (selected) "Selected" else "Not selected",
                        tint = if (selected) CyanAccent else TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = preset.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${preset.videoTimestamp} · Open ${preset.openDurationMs}ms / Close ${preset.closeDurationMs}ms",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) CyanAccent else TextSecondary
                        )
                    }
                }
            }

            Text(
                text = preset.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (selected) CobaltPrimary.copy(alpha = 0.22f) else ObsidianSurfaceElevated
                ) {
                    Text(
                        text = if (selected) "ACTIVE PRESET" else "TAP TO SELECT",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) CyanAccent else TextSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                OutlinedButton(
                    onClick = onPreview,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("preview_button_${preset.id.storageKey}")
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Preview")
                }
            }
        }
    }
}

@Composable
private fun StockSamsungCard(
    selected: Boolean,
    onSelect: () -> Unit
) {
    val borderColor = if (selected) AmberCandidate else ObsidianOutline
    val bgColor = if (selected) AmberContainer.copy(alpha = 0.5f) else ObsidianSurface

    Surface(
        shape = MaterialTheme.shapes.large,
        color = bgColor,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .border(if (selected) 1.5.dp else 1.dp, borderColor, MaterialTheme.shapes.large)
            .clickable(onClick = onSelect)
            .testTag("preset_card_stock_samsung")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.Restore,
                    contentDescription = "Stock Samsung Animation",
                    tint = if (selected) AmberCandidate else TextSecondary
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = AnimationPresetRegistry.stockSamsungPreset.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (selected) AmberCandidate else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = AnimationPresetRegistry.stockSamsungPreset.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusHeroCard(
    config: MotionConfig,
    diagnostics: RuntimeDiagnostics,
    deviceInfo: DeviceEnvironmentInfo,
    onClearBreaker: () -> Unit
) {
    val isTripped = diagnostics.circuitBreakerTripped || config.safeRecoveryTripped
    val isHooked = diagnostics.moduleActiveInLsposed || diagnostics.systemUiHooked

    val badgeColor = when {
        isTripped -> CoralAlert
        isHooked -> EmeraldVerified
        else -> AmberCandidate
    }
    val badgeBg = when {
        isTripped -> CoralContainer
        isHooked -> EmeraldContainer
        else -> AmberContainer
    }
    val statusTitle = when {
        isTripped -> "Safety Circuit Breaker Tripped"
        isHooked -> "LSPosed Module Active in SystemUI"
        else -> "Standby · Enable Module & System UI Scope in LSPosed"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(Brush.linearGradient(listOf(Color(0xFF101B33), Color(0xFF0A0E17))))
            .border(1.dp, ObsidianOutline, MaterialTheme.shapes.large)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CobaltPrimary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Animation, contentDescription = null, tint = CyanAccent)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(text = "One UI 9 Ultra Motion", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "${deviceInfo.model} · Android ${deviceInfo.androidRelease} · ${deviceInfo.currentRefreshHz.roundToInt()} Hz",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            Surface(shape = RoundedCornerShape(12.dp), color = badgeBg) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            if (isHooked && !isTripped) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = statusTitle,
                            style = MaterialTheme.typography.labelMedium,
                            color = badgeColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (isTripped) {
                        TextButton(onClick = onClearBreaker) { Text("Clear", color = CyanAccent) }
                    } else {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(badgeColor))
                    }
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else TextSecondary
            )
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun SliderControl(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
    helperText: String,
    testTag: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Surface(shape = RoundedCornerShape(8.dp), color = ObsidianSurfaceElevated) {
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.labelSmall,
                    color = CyanAccent,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = CyanAccent,
                activeTrackColor = CobaltPrimary,
                inactiveTrackColor = ObsidianOutline
            ),
            modifier = Modifier.testTag(testTag)
        )
        Text(text = helperText, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    }
}
