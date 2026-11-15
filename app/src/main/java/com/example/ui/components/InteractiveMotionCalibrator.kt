package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.AnimationPreset
import com.example.data.MotionConfig
import com.example.motion.SpringPhysicsSolver
import com.example.ui.theme.AmberCandidate
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Interactive simulated preview dialog for any selected [AnimationPreset].
 * Uses the exact [SpringPhysicsSolver] parameters of the SystemUI hook while
 * clearly marking itself as an in-app simulated preview.
 */
@Composable
fun PresetPreviewDialog(
    preset: AnimationPreset,
    baseConfig: MotionConfig,
    onSelectPreset: () -> Unit,
    onDismiss: () -> Unit
) {
    val previewConfig = remember(preset, baseConfig) {
        baseConfig.copy(
            selectedPresetId = preset.id,
            masterEnabled = preset.id != com.example.data.ControlCenterPresetId.STOCK_SAMSUNG,
            openingEnabled = true,
            closingEnabled = true
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = MaterialTheme.shapes.large,
            color = ObsidianSurface
        ) {
            Column(
                modifier = Modifier
                    .border(1.dp, ObsidianOutline, MaterialTheme.shapes.large)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = preset.title,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Ref: ${preset.videoTimestamp}",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanAccent
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close preview")
                    }
                }

                // Clear disclosure: simulated preview using real engine math
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = AmberContainer
                ) {
                    Text(
                        text = "SIMULATED PREVIEW · Runs real engine physics (${preset.openDurationMs}ms open / ${preset.closeDurationMs}ms close). Apply & Soft Restart updates live SystemUI.",
                        style = MaterialTheme.typography.labelSmall,
                        color = AmberCandidate,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }

                InteractiveMotionCalibrator(
                    config = previewConfig,
                    autoDemoCycleOnLaunch = true
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Open: ${preset.openingSummary}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = "Close: ${preset.closingSummary}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Close")
                    }
                    Button(
                        onClick = {
                            onSelectPreset()
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("preview_select_preset_button")
                    ) {
                        Text("Select Preset", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun InteractiveMotionCalibrator(
    config: MotionConfig,
    modifier: Modifier = Modifier,
    autoDemoCycleOnLaunch: Boolean = false
) {
    val solver = remember { SpringPhysicsSolver() }
    var snapshot by remember { mutableStateOf(solver.snapshot()) }
    var isOpenTarget by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var measuredFrameMs by remember { mutableFloatStateOf(8.3f) }
    var animNonce by remember { mutableIntStateOf(0) }
    var demoNonce by remember { mutableIntStateOf(0) }

    LaunchedEffect(config) {
        solver.updateConfig(config)
        snapshot = solver.snapshot()
    }

    LaunchedEffect(autoDemoCycleOnLaunch, demoNonce, config.selectedPresetId) {
        if (autoDemoCycleOnLaunch) {
            solver.snapToState(open = false)
            snapshot = solver.snapshot()
            delay(180)
            isOpenTarget = true
            solver.animateTo(open = true)
            snapshot = solver.snapshot()
            animNonce++
            delay(900)
            isOpenTarget = false
            solver.animateTo(open = false)
            snapshot = solver.snapshot()
            animNonce++
        }
    }

    LaunchedEffect(animNonce) {
        var lastNanos = 0L
        while (solver.isRunning) {
            withFrameNanos { now ->
                if (lastNanos > 0L) {
                    val dt = ((now - lastNanos) / 1_000_000_000f).coerceIn(0.001f, 0.034f)
                    measuredFrameMs = dt * 1000f
                    snapshot = solver.step(dt)
                }
                lastNanos = now
            }
        }
    }

    val density = LocalDensity.current.density
    val stageHeightPx = with(LocalDensity.current) { 216.dp.toPx() }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val fpsEstimate = if (measuredFrameMs > 0.5f) (1000f / measuredFrameMs).roundToInt() else 120
            Text(
                text = "Swipe vertically or use controls below",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = ObsidianSurfaceElevated
            ) {
                Text(
                    text = "${snapshot.direction.name} · ${fpsEstimate}Hz",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyanAccent,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(216.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF281938),
                            Color(0xFF47251E),
                            Color(0xFF141826)
                        )
                    )
                )
                .border(1.dp, ObsidianOutline, RoundedCornerShape(20.dp))
                .pointerInput(config) {
                    var lastYDelta = 0f
                    detectVerticalDragGestures(
                        onDragStart = {
                            dragFraction = snapshot.masterProgress.coerceIn(0f, 1f)
                        },
                        onVerticalDrag = { _, dragAmount ->
                            lastYDelta = dragAmount
                            dragFraction = (dragFraction + dragAmount / stageHeightPx).coerceIn(0f, 1f)
                            solver.setInteractiveDrag(dragFraction, dragAmount * 0.08f)
                            snapshot = solver.snapshot()
                        },
                        onDragEnd = {
                            val open = when {
                                lastYDelta > 6f -> true
                                lastYDelta < -6f -> false
                                else -> dragFraction >= 0.42f
                            }
                            isOpenTarget = open
                            solver.animateTo(open, (lastYDelta / 22f).coerceIn(-6f, 6f))
                            snapshot = solver.snapshot()
                            animNonce++
                        },
                        onDragCancel = {
                            val open = dragFraction >= 0.5f
                            isOpenTarget = open
                            solver.animateTo(open)
                            snapshot = solver.snapshot()
                            animNonce++
                        }
                    )
                }
                .testTag("calibrator_stage")
        ) {
            // Home wallpaper dock scaling subtly back during blur
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = snapshot.wallpaperScale
                        scaleY = snapshot.wallpaperScale
                    }
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    repeat(4) { idx ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    listOf(
                                        Color(0xFF34D399),
                                        Color(0xFF60A5FA),
                                        Color(0xFFF472B6),
                                        Color(0xFFFBBF24)
                                    )[idx].copy(alpha = 0.72f)
                                )
                        )
                    }
                }
            }

            // Dynamic blur/scrim layer
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Color(0xFF0A0F1A).copy(alpha = 0.70f * snapshot.backdropBlurProgress)
                    )
            )

            // 5 Spatial Tiers
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val tiers = snapshot.tiers
                TierShell(transform = tiers[0], density = density) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "21:16 · Control Center",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(CyanAccent))
                            Box(Modifier.size(8.dp).clip(CircleShape).background(Color.White.copy(0.6f)))
                        }
                    }
                }

                TierShell(transform = tiers[1], density = density) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GlassTile(Modifier.weight(1f).height(34.dp), CobaltPrimary.copy(0.45f))
                        GlassTile(Modifier.weight(1f).height(34.dp), Color.White.copy(0.18f))
                    }
                }

                TierShell(transform = tiers[2], density = density) {
                    GlassTile(Modifier.fillMaxWidth().height(42.dp), Color.White.copy(0.16f))
                }

                TierShell(transform = tiers[3], density = density) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GlassTile(Modifier.weight(1f).height(24.dp), CyanAccent.copy(0.35f))
                        GlassTile(Modifier.weight(1f).height(24.dp), Color.White.copy(0.24f))
                    }
                }

                TierShell(transform = tiers[4], density = density) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GlassTile(Modifier.weight(1f).height(28.dp), Color.White.copy(0.15f))
                        GlassTile(Modifier.weight(1f).height(28.dp), Color.White.copy(0.15f))
                    }
                }
            }

            if (snapshot.masterProgress < 0.04f && !snapshot.isAnimating) {
                Text(
                    text = "Swipe down or tap Open",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(Color.Black.copy(alpha = 0.52f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = {
                    isOpenTarget = true
                    solver.animateTo(open = true)
                    snapshot = solver.snapshot()
                    animNonce++
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("calibrator_open_button")
            ) {
                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Open", fontWeight = FontWeight.SemiBold)
            }

            FilledTonalButton(
                onClick = {
                    isOpenTarget = false
                    solver.animateTo(open = false)
                    snapshot = solver.snapshot()
                    animNonce++
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("calibrator_close_button")
            ) {
                Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Close", fontWeight = FontWeight.SemiBold)
            }

            OutlinedButton(
                onClick = {
                    if (autoDemoCycleOnLaunch && !solver.isRunning) {
                        demoNonce++
                    } else {
                        isOpenTarget = !isOpenTarget
                        solver.animateTo(open = isOpenTarget)
                        snapshot = solver.snapshot()
                        animNonce++
                    }
                },
                modifier = Modifier
                    .height(48.dp)
                    .testTag("calibrator_interrupt_button")
            ) {
                Icon(
                    if (autoDemoCycleOnLaunch && !solver.isRunning) Icons.Rounded.PlayArrow else Icons.Rounded.SwapVert,
                    contentDescription = "Cycle or reverse animation"
                )
                Spacer(Modifier.width(4.dp))
                Text(if (autoDemoCycleOnLaunch && !solver.isRunning) "Replay" else "Reverse")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Blur: ${(snapshot.backdropBlurProgress * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Text(
                text = "Scale: ${"%.3f".format(snapshot.tiers.last().scale)}x",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Bolt,
                    contentDescription = null,
                    tint = if (config.isCustomMotionActive) CyanAccent else TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    text = if (config.isCustomMotionActive) "Preview Active" else "Stock Bypass",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (config.isCustomMotionActive) CyanAccent else TextSecondary
                )
            }
        }
    }
}

@Composable
private fun TierShell(
    transform: SpringPhysicsSolver.TierTransform,
    density: Float,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                transformOrigin = TransformOrigin(transform.pivotX, transform.pivotY)
                translationX = transform.translationXDp * density
                translationY = transform.translationYDp * density
                scaleX = transform.scale
                scaleY = transform.scale
                alpha = transform.alpha
            }
    ) {
        content()
    }
}

@Composable
private fun GlassTile(modifier: Modifier, color: Color) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color)
            .border(0.75.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(10.dp))
    )
}
