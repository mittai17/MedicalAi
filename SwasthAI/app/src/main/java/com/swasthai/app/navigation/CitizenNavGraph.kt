package com.swasthai.app.navigation

import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.swasthai.app.feature.citizen.alerts.AlertsRemindersScreen
import com.swasthai.app.feature.citizen.connect.ConnectProvidersScreen
import com.swasthai.app.feature.citizen.dashboard.CitizenDashboardScreen
import com.swasthai.app.feature.citizen.diagnosis.ScreeningResultScreen
import com.swasthai.app.feature.citizen.profile.CitizenProfileScreen
import com.swasthai.app.feature.citizen.records.HealthRecordsScreen
import com.swasthai.app.feature.citizen.records.ShareReportScreen
import com.swasthai.app.feature.citizen.screening.ImageCheckScreen
import com.swasthai.app.feature.citizen.screening.ScreeningViewModel
import com.swasthai.app.feature.citizen.screening.SymptomCheckScreen
import com.swasthai.app.feature.citizen.screening.VitalsInputScreen
import com.swasthai.app.feature.citizen.screening.VoiceAssistantScreen
import com.swasthai.app.feature.citizen.tips.HealthTipsScreen

/**
 * Citizen navigation sub-graph.
 *
 * All placeholder screens replaced with real implementations.
 * The ScreeningViewModel is scoped to the Activity so the full
 * screening wizard (SymptomCheck → VitalsInput → ImageCheck → Result)
 * shares one instance across destinations.
 */
fun NavGraphBuilder.citizenNavGraph(navController: NavHostController) {

    // ── Dashboard ──
    composable(Screen.CitizenDashboard.route) {
        CitizenDashboardScreen(
            onSymptomCheck = { navController.navigate(Screen.SymptomCheck.route) },
            onVoiceAssistant = { navController.navigate(Screen.VoiceAssistant.route) },
            onImageCheck = { navController.navigate(Screen.ImageCheck.route) },
            onHealthTips = { navController.navigate(Screen.HealthTips.route) },
            onConnectProviders = { navController.navigate(Screen.ConnectProviders.route) },
            onAlertsReminders = { navController.navigate(Screen.AlertsReminders.route) },
            onProfile = { navController.navigate(Screen.CitizenProfile.route) },
            onScreeningDetail = { id ->
                navController.navigate(Screen.RecordDetail.createRoute(id))
            }
        )
    }

    // ── Symptom Check (shares ScreeningViewModel with Vitals + Result) ──
    composable(Screen.SymptomCheck.route) { backStackEntry ->
        val viewModel: ScreeningViewModel = hiltViewModel(
            LocalContext.current as ViewModelStoreOwner
        )
        SymptomCheckScreen(
            onBack = { navController.popBackStack() },
            onNavigateToVitals = {
                navController.navigate(Screen.VitalsInput.route)
            },
            onNavigateToResult = {
                navController.navigate(Screen.ScreeningResult.route) {
                    popUpTo(Screen.SymptomCheck.route) { inclusive = true }
                }
            },
            viewModel = viewModel
        )
    }

    // ── Voice Assistant ──
    composable(Screen.VoiceAssistant.route) { backStackEntry ->
        val viewModel: ScreeningViewModel = hiltViewModel(
            LocalContext.current as ViewModelStoreOwner
        )
        VoiceAssistantScreen(
            onBack = { navController.popBackStack() },
            onNavigateToResult = {
                navController.navigate(Screen.ScreeningResult.route) {
                    popUpTo(Screen.VoiceAssistant.route) { inclusive = true }
                }
            },
            viewModel = viewModel
        )
    }

    // ── Image Check ──
    composable(Screen.ImageCheck.route) { backStackEntry ->
        val viewModel: ScreeningViewModel = hiltViewModel(
            LocalContext.current as ViewModelStoreOwner
        )
        ImageCheckScreen(
            onBack = { navController.popBackStack() },
            onNavigateToResult = {
                navController.navigate(Screen.ScreeningResult.route) {
                    popUpTo(Screen.ImageCheck.route) { inclusive = true }
                }
            },
            viewModel = viewModel
        )
    }

    // ── Vitals Input ──
    composable(Screen.VitalsInput.route) { backStackEntry ->
        val viewModel: ScreeningViewModel = hiltViewModel(
            LocalContext.current as ViewModelStoreOwner
        )
        VitalsInputScreen(
            onBack = { navController.popBackStack() },
            onNavigateToResult = {
                navController.navigate(Screen.ScreeningResult.route) {
                    popUpTo(Screen.VitalsInput.route) { inclusive = true }
                }
            },
            viewModel = viewModel
        )
    }

    // ── Screening Result ──
    composable(Screen.ScreeningResult.route) { backStackEntry ->
        val viewModel: ScreeningViewModel = hiltViewModel(
            LocalContext.current as ViewModelStoreOwner
        )
        ScreeningResultScreen(
            onBack = { navController.popBackStack() },
            onFindNearbyCenter = { navController.navigate(Screen.ConnectProviders.route) },
            onCallHealthWorker = { navController.navigate(Screen.ConnectProviders.route) },
            onSaveReport = { screeningId ->
                navController.navigate(Screen.ShareReport.createRoute(screeningId))
            },
            onNewScreening = {
                navController.navigate(Screen.CitizenDashboard.route) {
                    popUpTo(Screen.CitizenDashboard.route) { inclusive = true }
                }
            },
            viewModel = viewModel
        )
    }

    // ── Health Records ──
    composable(Screen.HealthRecords.route) {
        HealthRecordsScreen(
            onBack = { navController.popBackStack() },
            onScreeningDetail = { id ->
                navController.navigate(Screen.RecordDetail.createRoute(id))
            },
            onShareReport = { id ->
                navController.navigate(Screen.ShareReport.createRoute(id))
            }
        )
    }

    // ── Record Detail (reuse HealthRecords for now) ──
    composable(
        route = Screen.RecordDetail.route,
        arguments = listOf(navArgument("screeningId") { type = NavType.StringType })
    ) { backStackEntry ->
        val screeningId = backStackEntry.arguments?.getString("screeningId") ?: ""
        HealthRecordsScreen(
            onBack = { navController.popBackStack() },
            onScreeningDetail = {},
            onShareReport = { id ->
                navController.navigate(Screen.ShareReport.createRoute(id))
            }
        )
    }

    // ── Share Report ──
    composable(
        route = Screen.ShareReport.route,
        arguments = listOf(navArgument("screeningId") { type = NavType.StringType })
    ) { backStackEntry ->
        val screeningId = backStackEntry.arguments?.getString("screeningId") ?: ""
        ShareReportScreen(
            screeningId = screeningId,
            onBack = { navController.popBackStack() }
        )
    }

    // ── Connect & Providers ──
    composable(Screen.ConnectProviders.route) {
        ConnectProvidersScreen(
            onBack = { navController.popBackStack() }
        )
    }

    // ── Alerts & Reminders ──
    composable(Screen.AlertsReminders.route) {
        AlertsRemindersScreen(
            onBack = { navController.popBackStack() }
        )
    }

    // ── Health Tips ──
    composable(Screen.HealthTips.route) {
        HealthTipsScreen(
            onBack = { navController.popBackStack() }
        )
    }

    // ── Citizen Profile ──
    composable(Screen.CitizenProfile.route) {
        CitizenProfileScreen(
            onBack = { navController.popBackStack() },
            onLogout = {
                navController.navigate(Screen.Login.route) {
                    popUpTo(Screen.CitizenDashboard.route) { inclusive = true }
                }
            }
        )
    }
}
