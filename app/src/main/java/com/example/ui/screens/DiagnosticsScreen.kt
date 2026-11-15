package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.DeviceEnvironmentInfo
import com.example.data.HookBindState
import com.example.data.HookDescriptor
import com.example.data.RuntimeDiagnostics
import com.example.data.VerificationTier
import com.example.ui.theme.AmberCandidate
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.CobaltPrimary
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

@Composable
fun DiagnosticsScreen(
    diagnostics: RuntimeDiagnostics,
    deviceInfo: DeviceEnvironmentInfo,
    hookDescriptors: List<HookDescriptor>,
    onRefreshDiagnostics: () -> Unit,
    onClearSafetyBreaker: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Runtime Telemetry Summary
            Surface(
                shape = MaterialTheme.shapes.large,
                color = ObsidianSurface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ObsidianOutline, MaterialTheme.shapes.large)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.BugReport, contentDescription = null, tint = CyanAccent)
                            Spacer(Modifier.width(8.dp))
                            Text("SystemUI & LSPosed Probe", style = MaterialTheme.typography.titleMedium)
                        }
                        OutlinedButton(
                            onClick = onRefreshDiagnostics,
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("refresh_diagnostics_button")
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Refresh diagnostics")
                            Spacer(Modifier.width(6.dp))
                            Text("Refresh")
                        }
                    }

                    TelemetryLine("Device / Firmware", "${deviceInfo.manufacturer} ${deviceInfo.model} · ${deviceInfo.oneUiHint}")
                    TelemetryLine("Android OS", "Android ${deviceInfo.androidRelease} (API ${deviceInfo.sdkInt})")
                    TelemetryLine(
                        "Display Modes",
                        deviceInfo.supportedRefreshRatesHz.joinToString(", ") { "${it.toInt()} Hz" }
                    )
                    TelemetryLine(
                        "Self-Hook Status",
                        if (diagnostics.moduleActiveInLsposed) "Active in LSPosed" else "Inactive (Companion Mode)"
                    )
                    TelemetryLine(
                        "SystemUI Hooks",
                        if (diagnostics.systemUiHooked) "${diagnostics.activeHookCount} bound in com.android.systemui" else "Awaiting com.android.systemui scope"
                    )
                    if (diagnostics.lastFrameDurationMs > 0f) {
                        TelemetryLine("Last Hook Frame", "${"%.2f".format(diagnostics.lastFrameDurationMs)} ms")
                    }

                    if (diagnostics.circuitBreakerTripped) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CoralContainer
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Safety Circuit Breaker Tripped: ${diagnostics.circuitBreakerReason}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = CoralAlert
                                )
                                Button(
                                    onClick = onClearSafetyBreaker,
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Text("Reset Safety Breaker")
                                }
                            }
                        }
                    }
                }
            }

            // Honest Hook Verification Matrix
            Text(
                text = "Hook Verification Matrix",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Standard AOSP APIs are verified against Android SDK/AOSP SystemUI. Samsung One UI classes marked Candidate require runtime probing or inspection of your One UI 9 /system_ext/priv-app/SystemUI/SystemUI.apk if any show Class Absent.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            hookDescriptors.forEach { hook ->
                HookDescriptorCard(hook)
            }

            // Recovery & Safe Mode Instructions
            Surface(
                shape = MaterialTheme.shapes.large,
                color = ObsidianSurface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ObsidianOutline, MaterialTheme.shapes.large)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Security, contentDescription = null, tint = EmeraldVerified)
                        Spacer(Modifier.width(8.dp))
                        Text("Module Recovery & Crash Prevention", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        text = "1. Automatic Safety Guard: Any unhandled hook exception or invisible expanded panel (>600ms) automatically trips the circuit breaker and restores stock Samsung animations.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = "2. Manual Disable: Toggle off Master Switch or tap 'Stock Samsung' in Motion Lab to bypass all transforms immediately without rebooting.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = "3. KernelSU Safe Recovery: If SystemUI ever misbehaves after a firmware update, disable 'Ultra Motion' inside LSPosed Manager and restart SystemUI (or reboot holding Volume Down for KernelSU safe mode).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun TelemetryLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun HookDescriptorCard(hook: HookDescriptor) {
    val isVerifiedAosp = hook.tier == VerificationTier.VERIFIED_AOSP_API
    val tierColor = if (isVerifiedAosp) EmeraldVerified else AmberCandidate
    val tierBg = if (isVerifiedAosp) EmeraldContainer else AmberContainer

    val bindColor = when (hook.bindState) {
        HookBindState.HOOKED -> EmeraldVerified
        HookBindState.BYPASSED_SAFETY -> CoralAlert
        HookBindState.NOT_FOUND -> TextSecondary
        HookBindState.STANDBY -> CobaltPrimary
    }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = ObsidianSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ObsidianOutline, MaterialTheme.shapes.medium)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = RoundedCornerShape(8.dp), color = tierBg) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isVerifiedAosp) Icons.Rounded.VerifiedUser else Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            tint = tierColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = hook.tier.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = tierColor
                        )
                    }
                }

                Surface(shape = RoundedCornerShape(8.dp), color = ObsidianSurfaceElevated) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (hook.bindState == HookBindState.HOOKED) {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = bindColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            text = hook.bindState.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = bindColor
                        )
                    }
                }
            }

            Text(
                text = hook.targetClass,
                style = MaterialTheme.typography.labelSmall,
                color = CyanAccent
            )
            Text(
                text = "Method: ${hook.targetMethod}",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Text(
                text = hook.purpose,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}
