package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.MotionViewModel
import com.example.ui.screens.DiagnosticsScreen
import com.example.ui.screens.MotionLabScreen
import com.example.ui.screens.TimelineScreen
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.UltraMotionTheme

private enum class AppDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    PRESETS("motion_lab", "Animations", Icons.Rounded.Tune, Icons.Outlined.Tune, "nav_motion_lab"),
    TIMELINE("timeline", "Timelines", Icons.Rounded.Timeline, Icons.Outlined.Timeline, "nav_timeline"),
    DIAGNOSTICS("diagnostics", "Hooks & Logs", Icons.Rounded.BugReport, Icons.Outlined.BugReport, "nav_diagnostics")
}

class MainActivity : ComponentActivity() {

    private val viewModel: MotionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UltraMotionTheme {
                UltraMotionAppShell(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshDiagnostics()
    }
}

@Composable
private fun UltraMotionAppShell(viewModel: MotionViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val config by viewModel.config.collectAsStateWithLifecycle()
    val previewPreset by viewModel.previewPreset.collectAsStateWithLifecycle()
    val diagnostics by viewModel.diagnostics.collectAsStateWithLifecycle()
    val deviceInfo by viewModel.deviceInfo.collectAsStateWithLifecycle()
    val hookDescriptors by viewModel.hookDescriptors.collectAsStateWithLifecycle()
    val isRestarting by viewModel.isRestartingSystemUi.collectAsStateWithLifecycle()
    val restartFeedback by viewModel.restartFeedback.collectAsStateWithLifecycle()

    val navigateTo: (AppDestination) -> Unit = { dest ->
        navController.navigate(dest.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = ObsidianBg,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!useRail) {
                    NavigationBar(containerColor = ObsidianSurface) {
                        AppDestination.entries.forEach { dest ->
                            val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(dest) },
                                icon = {
                                    Icon(
                                        if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(containerColor = ObsidianSurface) {
                        AppDestination.entries.forEach { dest ->
                            val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(dest) },
                                icon = {
                                    Icon(
                                        if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = AppDestination.PRESETS.route,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(AppDestination.PRESETS.route) {
                        MotionLabScreen(
                            config = config,
                            presets = viewModel.allVideoPresets(),
                            previewPreset = previewPreset,
                            diagnostics = diagnostics,
                            deviceInfo = deviceInfo,
                            isRestartingSystemUi = isRestarting,
                            restartFeedback = restartFeedback,
                            onSelectPreset = viewModel::selectPreset,
                            onOpenPreview = viewModel::openPresetPreview,
                            onDismissPreview = viewModel::dismissPresetPreview,
                            onOpeningChanged = viewModel::setOpeningEnabled,
                            onClosingChanged = viewModel::setClosingEnabled,
                            onSpeedChanged = viewModel::setSpeedMultiplier,
                            onSpringChanged = viewModel::setSpringIntensity,
                            onResetDefaults = viewModel::resetToVideoDefaults,
                            onClearBreaker = viewModel::clearSafetyBreaker,
                            onConfirmApplyAndSoftRestart = viewModel::confirmApplyAndSoftRestart,
                            onDismissRestartFeedback = viewModel::dismissRestartFeedback
                        )
                    }
                    composable(AppDestination.TIMELINE.route) {
                        TimelineScreen(config = config)
                    }
                    composable(AppDestination.DIAGNOSTICS.route) {
                        DiagnosticsScreen(
                            diagnostics = diagnostics,
                            deviceInfo = deviceInfo,
                            hookDescriptors = hookDescriptors,
                            onRefreshDiagnostics = viewModel::refreshDiagnostics,
                            onClearSafetyBreaker = viewModel::clearSafetyBreaker
                        )
                    }
                }
            }
        }
    }
}
