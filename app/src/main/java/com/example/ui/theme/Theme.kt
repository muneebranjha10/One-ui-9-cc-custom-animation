package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val UltraMotionColorScheme = darkColorScheme(
    primary = CobaltPrimary,
    onPrimary = ObsidianBg,
    primaryContainer = CobaltPrimaryContainer,
    onPrimaryContainer = TextPrimary,
    secondary = CyanAccent,
    onSecondary = ObsidianBg,
    secondaryContainer = CyanContainer,
    onSecondaryContainer = CyanAccent,
    tertiary = EmeraldVerified,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = ObsidianOutline,
    outlineVariant = ObsidianOutline,
    error = CoralAlert,
    errorContainer = CoralContainer,
    onErrorContainer = CoralAlert
)

private val UltraMotionShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp)
)

@Composable
fun UltraMotionTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = UltraMotionColorScheme,
        typography = UltraMotionTypography,
        shapes = UltraMotionShapes,
        content = content
    )
}
