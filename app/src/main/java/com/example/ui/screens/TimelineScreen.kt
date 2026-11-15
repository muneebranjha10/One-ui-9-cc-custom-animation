package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.data.AnimationPreset
import com.example.data.AnimationPresetRegistry
import com.example.data.ControlCenterPresetId
import com.example.data.MotionConfig
import com.example.motion.SpringPhysicsSolver
import com.example.ui.theme.AmberCandidate
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.TextSecondary

@Composable
fun TimelineScreen(
    config: MotionConfig,
    modifier: Modifier = Modifier
) {
    val initialPreset = if (config.selectedPresetId == ControlCenterPresetId.STOCK_SAMSUNG) {
        AnimationPresetRegistry.videoPresets.first()
    } else {
        config.activePreset
    }
    var inspectedId by remember(config.selectedPresetId) { mutableStateOf(initialPreset.id) }
    val preset = AnimationPresetRegistry.findById(inspectedId)

    val openCurvePoints = remember(preset.id, config.clampedSpeed, config.clampedSpring) {
        sampleCurve(open = true, preset = preset, config = config)
    }
    val closeCurvePoints = remember(preset.id, config.clampedSpeed) {
        sampleCurve(open = false, preset = preset, config = config)
    }

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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "60fps Video Animation Timelines",
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnimationPresetRegistry.videoPresets.forEachIndexed { idx, item ->
                    FilterChip(
                        selected = item.id == inspectedId,
                        onClick = { inspectedId = item.id },
                        label = { Text("Style 0${idx + 1}") }
                    )
                }
            }

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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Timeline, contentDescription = null, tint = CyanAccent)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(text = preset.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Video segment ${preset.videoTimestamp}",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanAccent
                            )
                        }
                    }

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    ) {
                        val w = size.width
                        val h = size.height
                        val baselineY = h * 0.85f
                        val topY = h * 0.15f
                        val spanY = baselineY - topY

                        for (g in 0..3) {
                            val gy = topY + spanY * (g / 3f)
                            drawLine(
                                color = Color(0xFF1F2A3D),
                                start = Offset(0f, gy),
                                end = Offset(w, gy),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        drawLine(
                            color = EmeraldVerified.copy(alpha = 0.45f),
                            start = Offset(0f, topY),
                            end = Offset(w, topY),
                            strokeWidth = 1.5.dp.toPx()
                        )

                        val openPath = Path()
                        openCurvePoints.forEachIndexed { idx, value ->
                            val x = (idx / (openCurvePoints.size - 1f)) * w
                            val y = baselineY - value * spanY
                            if (idx == 0) openPath.moveTo(x, y) else openPath.lineTo(x, y)
                        }
                        drawPath(
                            path = openPath,
                            color = CyanAccent,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )

                        val closePath = Path()
                        closeCurvePoints.forEachIndexed { idx, value ->
                            val x = (idx / (closeCurvePoints.size - 1f)) * w
                            val y = baselineY - value * spanY
                            if (idx == 0) closePath.moveTo(x, y) else closePath.lineTo(x, y)
                        }
                        drawPath(
                            path = closePath,
                            color = AmberCandidate,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        LegendPill(color = CyanAccent, label = "Opening (~${preset.openDurationMs}ms)")
                        LegendPill(color = AmberCandidate, label = "Closing (~${preset.closeDurationMs}ms)")
                    }
                }
            }

            DetailBlock(
                heading = "Opening Behavior",
                badge = "k=${preset.openStiffness.toInt()} · ζ=${preset.openDampingRatio} · +${preset.openTierStaggerMs}ms/tier",
                body = preset.openingSummary,
                accent = CyanAccent
            )

            DetailBlock(
                heading = "Closing Behavior",
                badge = "k=${preset.closeStiffness.toInt()} · ζ=${preset.closeDampingRatio} · alpha@${(preset.closeAlphaCutoff * 100).toInt()}%",
                body = preset.closingSummary,
                accent = AmberCandidate
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DetailBlock(heading: String, badge: String, body: String, accent: Color) {
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
                Text(text = heading, style = MaterialTheme.typography.titleMedium, color = accent)
                Surface(shape = RoundedCornerShape(8.dp), color = ObsidianSurfaceElevated) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = CobaltPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Text(text = body, style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
        }
    }
}

@Composable
private fun LegendPill(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = color,
            modifier = Modifier.height(8.dp).width(16.dp)
        ) {}
        Spacer(Modifier.width(6.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

private fun sampleCurve(open: Boolean, preset: AnimationPreset, config: MotionConfig): List<Float> {
    val solver = SpringPhysicsSolver()
    solver.updateConfig(
        config.copy(
            selectedPresetId = preset.id,
            masterEnabled = true,
            openingEnabled = true,
            closingEnabled = true
        )
    )
    solver.snapToState(!open)
    solver.animateTo(open = open)
    val points = mutableListOf<Float>()
    val totalSteps = 54
    val dt = 1f / 120f
    for (i in 0..totalSteps) {
        val snap = solver.step(dt)
        points.add(snap.tiers[2].progress)
    }
    return points
}
