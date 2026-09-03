package com.swasthai.app.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.work.WorkManager
import com.swasthai.app.data.local.datastore.UserPreferences
import com.swasthai.app.feature.citizen.aichat.AIChatScreen
import com.swasthai.app.feature.citizen.alerts.AlertsRemindersScreen
import com.swasthai.app.feature.citizen.connect.ConnectProvidersScreen
import com.swasthai.app.feature.citizen.connect.OnlineConsultationScreen
import com.swasthai.app.feature.citizen.dashboard.CitizenDashboardScreen
import com.swasthai.app.feature.citizen.diagnosis.ScreeningResultScreen
import com.swasthai.app.feature.citizen.profile.CitizenProfileScreen
import com.swasthai.app.feature.citizen.profile.EditProfileScreen
import com.swasthai.app.feature.citizen.records.HealthRecordsScreen
import com.swasthai.app.feature.citizen.records.RecordDetailScreen
import com.swasthai.app.feature.citizen.records.ShareReportScreen
import com.swasthai.app.feature.citizen.screening.HealthCheckHubScreen
import com.swasthai.app.feature.citizen.screening.HealthCheckMode
import com.swasthai.app.feature.citizen.screening.ScreeningViewModel
import com.swasthai.app.feature.citizen.screening.VitalsInputScreen
import com.swasthai.app.feature.citizen.tips.HealthTipsScreen
import com.swasthai.app.sync.SyncWorker

/**
 * Citizen main shell.
 *
 * Hosts a persistent bottom navigation bar across the primary destinations
 * (Home, Records, Connect, AI). Tab switches save/restore state so each
 * section keeps its own back stack and scroll position. Alerts and Profile
 * are reached from the Home top bar as pushed screens (no bottom bar).
 * Detail flows (screening wizard, record detail, share report, tips,
 * edit profile) are pushed on top and temporarily hide the bottom bar.
 *
 * The shell mounts inside the root [NavHost] at [Screen.CitizenHome];
 * [logout] pops the whole shell back to role selection.
 *
 * Background refresh is paused while the AI console is in focus so the
 * tiny on-device working memory stays unloaded for chat use.
 */
@Composable
fun CitizenMainScreen(
    rootNavController: NavHostController,
    logout: () -> Unit
) {
    val tabNavController = rememberNavController()
    val context = LocalContext.current

    // Background refresh pause during AI sessions.
    val userPreferences = remember { UserPreferences(context.applicationContext) }
    val autoBackgroundRefresh by userPreferences.autoBackgroundRefreshFlow
        .collectAsStateWithLifecycle(initialValue = true)
    val workManager = remember(context) { WorkManager.getInstance(context) }

    val backStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val onTab = CitizenBottomNav.entries.any { tab ->
        currentDestination?.hierarchy?.any { it.route == tab.route } == true
    }

    // When the AI console is the current tab, cancel the periodic sync so
    // work is not competing with chat; re-enqueue on exit if the pref allows.
    LaunchedEffect(currentDestination?.route, autoBackgroundRefresh, workManager) {
        val isAiFocused = currentDestination?.route == Screen.AIChat.route
        if (isAiFocused) {
            workManager.cancelUniqueWork(SyncWorker.WORK_NAME)
        } else if (autoBackgroundRefresh) {
            SyncWorker.enqueuePeriodicSync(workManager)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Bottom),
        bottomBar = {
            if (onTab) {
                CitizenBottomTabBar(
                    currentRoute = currentDestination?.route,
                    onTabSelected = { route -> tabNavController.navigateToTab(route) }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = tabNavController,
            startDestination = Screen.CitizenDashboard.route,
            modifier = Modifier.padding(padding)
        ) {
            // ── Primary tabs ──
            composable(Screen.CitizenDashboard.route) {
                CitizenDashboardScreen(
                    onSymptomCheck = { tabNavController.navigateToTab(Screen.HealthCheck.createRoute("symptoms")) },
                    onVoiceCommand = { tabNavController.navigateToTab(Screen.HealthCheck.createRoute("voice")) },
                    onImageCheck = { tabNavController.navigateToTab(Screen.HealthCheck.createRoute("photo")) },
                    onHealthTips = { tabNavController.navigate(Screen.HealthTips.route) },
                    onConnectProviders = { tabNavController.navigateToTab(Screen.ConnectProviders.route) },
                    onAlertsReminders = { tabNavController.navigate(Screen.AlertsReminders.route) },
                    onProfile = { tabNavController.navigate(Screen.CitizenProfile.route) },
                    onViewAllRecords = { tabNavController.navigateToTab(Screen.HealthRecords.route) },
                    onScreeningDetail = { id ->
                        tabNavController.navigate(Screen.RecordDetail.createRoute(id))
                    }
                )
            }

            composable(Screen.HealthRecords.route) {
                HealthRecordsScreen(
                    onBack = { tabNavController.popBackStack() },
                    onScreeningDetail = { id ->
                        tabNavController.navigate(Screen.RecordDetail.createRoute(id))
                    },
                    onShareReport = { id ->
                        tabNavController.navigate(Screen.ShareReport.createRoute(id))
                    }
                )
            }

            composable(Screen.ConnectProviders.route) {
                ConnectProvidersScreen(
                    onBack = { tabNavController.popBackStack() },
                    onOpenConsultation = { tabNavController.navigate(Screen.OnlineConsultation.route) }
                )
            }

            composable(Screen.OnlineConsultation.route) {
                OnlineConsultationScreen(
                    onBack = { tabNavController.popBackStack() }
                )
            }

            composable(Screen.AIChat.route) {
                AIChatScreen(
                    onBack = { tabNavController.popBackStack() },
                    onNavigateToReport = { screeningId ->
                        tabNavController.navigate(Screen.ShareReport.createRoute(screeningId))
                    }
                )
            }

            // ── Pushed screens (reached from Home top bar, no bottom bar) ──
            composable(Screen.AlertsReminders.route) {
                AlertsRemindersScreen(
                    onBack = { tabNavController.popBackStack() }
                )
            }

            composable(Screen.CitizenProfile.route) {
                CitizenProfileScreen(
                    onBack = { tabNavController.popBackStack() },
                    onLogout = logout,
                    onEditProfile = { tabNavController.navigate(Screen.EditProfile.route) }
                )
            }

            // ── Unified Health Check hub (text + photo + voice) ──
            composable(
                route = Screen.HealthCheck.route,
                arguments = listOf(
                    navArgument("mode") {
                        type = NavType.StringType
                        defaultValue = "symptoms"
                    }
                )
            ) { backStackEntry ->
                val viewModel: ScreeningViewModel = hiltViewModel(
                    LocalContext.current as ViewModelStoreOwner
                )
                val mode = backStackEntry.arguments?.getString("mode") ?: "symptoms"
                HealthCheckHubScreen(
                    initialMode = HealthCheckMode.from(mode),
                    onNavigateToVitals = { tabNavController.navigate(Screen.VitalsInput.route) },
                    onNavigateToResult = {
                        tabNavController.navigate(Screen.ScreeningResult.route) {
                            popUpTo(Screen.HealthCheck.route) { inclusive = true }
                        }
                    },
                    onNavigateToConnect = {
                        tabNavController.navigateToTab(Screen.ConnectProviders.route)
                    },
                    onNavigateToRecords = {
                        tabNavController.navigateToTab(Screen.HealthRecords.route)
                    },
                    viewModel = viewModel
                )
            }

            // ── Vitals & result (pushed on top of the Health Check hub) ──
            composable(Screen.VitalsInput.route) {
                val viewModel: ScreeningViewModel = hiltViewModel(
                    LocalContext.current as ViewModelStoreOwner
                )
                VitalsInputScreen(
                    onBack = { tabNavController.popBackStack() },
                    onNavigateToResult = {
                        tabNavController.navigate(Screen.ScreeningResult.route) {
                            popUpTo(Screen.VitalsInput.route) { inclusive = true }
                        }
                    },
                    viewModel = viewModel
                )
            }

            composable(Screen.ScreeningResult.route) {
                val viewModel: ScreeningViewModel = hiltViewModel(
                    LocalContext.current as ViewModelStoreOwner
                )
                ScreeningResultScreen(
                    onBack = { tabNavController.popBackStack() },
                    onFindNearbyCenter = { tabNavController.navigateToTab(Screen.ConnectProviders.route) },
                    onCallHealthWorker = { tabNavController.navigateToTab(Screen.ConnectProviders.route) },
                    onSaveReport = { screeningId ->
                        tabNavController.navigate(Screen.ShareReport.createRoute(screeningId))
                    },
                    onNewScreening = {
                        tabNavController.popBackStack(Screen.CitizenDashboard.route, inclusive = false)
                    },
                    viewModel = viewModel
                )
            }

            // ── Records detail ──
            composable(
                route = Screen.RecordDetail.route,
                arguments = listOf(navArgument("screeningId") { type = NavType.StringType })
            ) { backStackEntry ->
                val screeningId = backStackEntry.arguments?.getString("screeningId").orEmpty()
                RecordDetailScreen(
                    screeningId = screeningId,
                    onBack = { tabNavController.popBackStack() },
                    onShareReport = { id ->
                        tabNavController.navigate(Screen.ShareReport.createRoute(id))
                    }
                )
            }

            composable(
                route = Screen.ShareReport.route,
                arguments = listOf(navArgument("screeningId") { type = NavType.StringType })
            ) { backStackEntry ->
                val screeningId = backStackEntry.arguments?.getString("screeningId").orEmpty()
                ShareReportScreen(
                    screeningId = screeningId,
                    onBack = { tabNavController.popBackStack() }
                )
            }

            // ── Secondary pushes ──
            composable(Screen.HealthTips.route) {
                HealthTipsScreen(
                    onBack = { tabNavController.popBackStack() }
                )
            }

            composable(Screen.EditProfile.route) {
                EditProfileScreen(
                    onBack = { tabNavController.popBackStack() }
                )
            }
        }
    }
}

/**
 * Switches to a primary tab while saving/restoring each tab's own back
 * stack and scroll state. `launchSingleTop` prevents duplicate entries.
 */
private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun CitizenBottomTabBar(
    currentRoute: String?,
    onTabSelected: (String) -> Unit
) {
    NavigationBar {
        CitizenBottomNav.entries.forEachIndexed { index, tab ->
            val selected = currentRoute == tab.route
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(tab.route) },
                icon = {
                    Icon(
                        imageVector = tab.iconVector(selected),
                        contentDescription = tab.label
                    )
                },
                label = {
                    Text(
                        text = tab.label,
                        maxLines = 1
                    )
                },
                colors = if (tab == CitizenBottomNav.HEALTH) {
                    NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = MaterialTheme.colorScheme.onPrimary,
                        indicatorColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedTextColor = MaterialTheme.colorScheme.primary
                    )
                } else {
                    NavigationBarItemDefaults.colors()
                }
            )
        }
    }
}

private fun CitizenBottomNav.iconVector(selected: Boolean): ImageVector = when (this) {
        CitizenBottomNav.HOME -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
        CitizenBottomNav.RECORDS -> if (selected) Icons.Filled.Description else Icons.Outlined.Description
        CitizenBottomNav.HEALTH -> if (selected) Icons.Filled.HealthAndSafety else Icons.Outlined.HealthAndSafety
        CitizenBottomNav.CONNECT -> if (selected) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder
        CitizenBottomNav.AI -> if (selected) Icons.Filled.AutoAwesome else Icons.Filled.AutoAwesome
    }