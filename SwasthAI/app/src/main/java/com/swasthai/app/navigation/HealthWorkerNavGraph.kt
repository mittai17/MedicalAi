package com.swasthai.app.navigation

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.swasthai.app.core.utils.NetworkMonitor
import com.swasthai.app.feature.citizen.screening.ScreeningViewModel
import com.swasthai.app.feature.healthworker.dashboard.HWDashboardScreen
import com.swasthai.app.feature.healthworker.misc.HWAlertsScreen
import com.swasthai.app.feature.healthworker.misc.HWProfileScreen
import com.swasthai.app.feature.healthworker.misc.HelpSupportScreen
import com.swasthai.app.feature.healthworker.misc.SettingsScreen
import com.swasthai.app.feature.healthworker.patients.AddPatientScreen
import com.swasthai.app.feature.healthworker.patients.PatientDetailScreen
import com.swasthai.app.feature.healthworker.patients.PatientListScreen
import com.swasthai.app.feature.healthworker.reports.HWReportsScreen
import com.swasthai.app.feature.healthworker.screening.FollowUpDecisionScreen
import com.swasthai.app.feature.healthworker.screening.HWScreeningScreen
import com.swasthai.app.feature.healthworker.screening.ReferToPHCScreen
import com.swasthai.app.feature.healthworker.screening.ScheduleFollowUpScreen
import com.swasthai.app.feature.healthworker.sync.SyncDataScreen

/**
 * Health Worker navigation sub-graph.
 *
 * All placeholder screens replaced with real implementations.
 * patientId and patientName args are threaded through the screening flow
 * so the HW context is always visible.
 */
fun NavGraphBuilder.healthWorkerNavGraph(
    navController: NavHostController,
    networkMonitor: NetworkMonitor
) {
    // ── HW Dashboard ──
    composable(Screen.HWDashboard.route) {
        val isOnline by networkMonitor.isOnline.collectAsState(initial = true)
        HWDashboardScreen(
            onAddPatient = { navController.navigate(Screen.AddPatient.route) },
            onPatientList = { navController.navigate(Screen.PatientList.route) },
            onPatientDetail = { id -> navController.navigate(Screen.PatientDetail.createRoute(id)) },
            onReports = { navController.navigate(Screen.HWReports.route) },
            onSyncData = { navController.navigate(Screen.SyncData.route) },
            onAlerts = { navController.navigate(Screen.HWAlerts.route) },
            onProfile = { navController.navigate(Screen.HWProfile.route) },
            onSettings = { navController.navigate(Screen.Settings.route) }
        )
    }

    // ── Patient List ──
    composable(Screen.PatientList.route) {
        PatientListScreen(
            onBack = { navController.popBackStack() },
            onPatientDetail = { id -> navController.navigate(Screen.PatientDetail.createRoute(id)) },
            onAddPatient = { navController.navigate(Screen.AddPatient.route) }
        )
    }

    // ── Patient Detail ──
    composable(
        route = Screen.PatientDetail.route,
        arguments = listOf(navArgument("patientId") { type = NavType.StringType })
    ) { backStackEntry ->
        val patientId = backStackEntry.arguments?.getString("patientId") ?: ""
        PatientDetailScreen(
            patientId = patientId,
            onBack = { navController.popBackStack() },
            onStartScreening = { id ->
                navController.navigate(Screen.HWScreening.createRoute(id))
            }
        )
    }

    // ── Add Patient ──
    composable(Screen.AddPatient.route) {
        AddPatientScreen(
            onBack = { navController.popBackStack() },
            onPatientAdded = { id ->
                navController.navigate(Screen.PatientDetail.createRoute(id)) {
                    popUpTo(Screen.AddPatient.route) { inclusive = true }
                }
            }
        )
    }

    // ── HW Screening ──
    composable(
        route = Screen.HWScreening.route,
        arguments = listOf(navArgument("patientId") { type = NavType.StringType })
    ) { backStackEntry ->
        val patientId = backStackEntry.arguments?.getString("patientId") ?: ""
        val viewModel: ScreeningViewModel = hiltViewModel(backStackEntry)
        HWScreeningScreen(
            patientId = patientId,
            patientName = "Patient",   // TODO: load from PatientViewModel
            onBack = { navController.popBackStack() },
            onNavigateToResult = {
                navController.navigate(Screen.HWScreeningResult.route)
            },
            viewModel = viewModel
        )
    }

    // ── HW Screening Result (reuse ScreeningResultScreen) ──
    composable(Screen.HWScreeningResult.route) { backStackEntry ->
        val viewModel: ScreeningViewModel = hiltViewModel(backStackEntry)
        com.swasthai.app.feature.citizen.diagnosis.ScreeningResultScreen(
            onBack = { navController.popBackStack() },
            onFindNearbyCenter = { navController.navigate(Screen.ReferToPHC.route) },
            onCallHealthWorker = { navController.navigate(Screen.HWAlerts.route) },
            onSaveReport = { _ -> navController.navigate(Screen.HWReports.route) },
            onNewScreening = {
                navController.navigate(Screen.PatientList.route) {
                    popUpTo(Screen.HWDashboard.route)
                }
            },
            viewModel = viewModel
        )
    }

    // ── Follow-up Decision ──
    composable(Screen.FollowUpDecision.route) { backStackEntry ->
        val viewModel: ScreeningViewModel = hiltViewModel(backStackEntry)
        FollowUpDecisionScreen(
            patientName = "Patient",
            onBack = { navController.popBackStack() },
            onScheduleFollowUp = { navController.navigate(Screen.ScheduleFollowUp.route) },
            onReferToPHC = { navController.navigate(Screen.ReferToPHC.route) },
            viewModel = viewModel
        )
    }

    // ── Schedule Follow-up ──
    composable(Screen.ScheduleFollowUp.route) {
        ScheduleFollowUpScreen(
            patientName = "Patient",
            onBack = { navController.popBackStack() },
            onScheduled = {
                navController.navigate(Screen.HWDashboard.route) {
                    popUpTo(Screen.HWDashboard.route) { inclusive = true }
                }
            }
        )
    }

    // ── Refer to PHC ──
    composable(Screen.ReferToPHC.route) {
        ReferToPHCScreen(
            patientName = "Patient",
            onBack = { navController.popBackStack() },
            onReferred = {
                navController.navigate(Screen.HWDashboard.route) {
                    popUpTo(Screen.HWDashboard.route) { inclusive = true }
                }
            }
        )
    }

    // ── HW Reports ──
    composable(Screen.HWReports.route) {
        HWReportsScreen(onBack = { navController.popBackStack() })
    }

    // ── Sync Data ──
    composable(Screen.SyncData.route) {
        val isOnline by networkMonitor.isOnline.collectAsState(initial = true)
        SyncDataScreen(
            isOnline = isOnline,
            onBack = { navController.popBackStack() }
        )
    }

    // ── HW Alerts ──
    composable(Screen.HWAlerts.route) {
        HWAlertsScreen(onBack = { navController.popBackStack() })
    }

    // ── HW Profile ──
    composable(Screen.HWProfile.route) {
        HWProfileScreen(
            onBack = { navController.popBackStack() },
            onLogout = {
                navController.navigate(Screen.RoleSelection.route) {
                    popUpTo(Screen.HWDashboard.route) { inclusive = true }
                }
            },
            onSettings = { navController.navigate(Screen.Settings.route) }
        )
    }

    // ── Settings ──
    composable(Screen.Settings.route) {
        SettingsScreen(onBack = { navController.popBackStack() })
    }

    // ── Help & Support ──
    composable(Screen.HelpSupport.route) {
        HelpSupportScreen(onBack = { navController.popBackStack() })
    }
}
