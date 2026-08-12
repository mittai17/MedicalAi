package com.swasthai.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.work.WorkManager
import com.swasthai.app.core.utils.NetworkMonitor
import com.swasthai.app.domain.model.UserRole
import com.swasthai.app.feature.auth.LoginScreen
import com.swasthai.app.feature.auth.RegisterScreen
import com.swasthai.app.feature.onboarding.LanguageSelectionScreen
import com.swasthai.app.feature.onboarding.RoleSelectionScreen
import com.swasthai.app.feature.onboarding.SplashScreen
import com.swasthai.app.feature.onboarding.WelcomeScreen
import com.swasthai.app.sync.SyncWorker

/**
 * Root NavHost for SwasthAI.
 *
 * Manages navigation flow:
 * Splash → Welcome → Language → Role → Login → Dashboard (Citizen or HW)
 *
 * Citizen and Health Worker sub-graphs are nested for clean separation.
 */
@Composable
fun SwasthAINavHost(
    navController: NavHostController = rememberNavController(),
    networkMonitor: NetworkMonitor
) {
    val context = LocalContext.current

    // Bootstrap periodic background sync
    LaunchedEffect(Unit) {
        SyncWorker.enqueuePeriodicSync(WorkManager.getInstance(context))
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        // ── Onboarding ──
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToWelcome = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToDashboard = { role ->
                    val destination = when (role) {
                        UserRole.HEALTH_WORKER -> Screen.HWDashboard.route
                        else -> Screen.CitizenDashboard.route
                    }
                    navController.navigate(destination) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onGetStarted = {
                    navController.navigate(Screen.LanguageSelection.route)
                }
            )
        }

        composable(Screen.LanguageSelection.route) {
            LanguageSelectionScreen(
                onContinue = {
                    navController.navigate(Screen.RoleSelection.route)
                }
            )
        }

        composable(Screen.RoleSelection.route) {
            RoleSelectionScreen(
                onContinue = {
                    navController.navigate(Screen.Login.route)
                }
            )
        }

        // ── Auth ──
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = { role ->
                    val destination = when (role) {
                        UserRole.HEALTH_WORKER -> Screen.HWDashboard.route
                        else -> Screen.CitizenDashboard.route
                    }
                    navController.navigate(destination) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onRegister = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = { role ->
                    val destination = when (role) {
                        UserRole.HEALTH_WORKER -> Screen.HWDashboard.route
                        else -> Screen.CitizenDashboard.route
                    }
                    navController.navigate(destination) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── Citizen Nav Graph ──
        citizenNavGraph(navController)

        // ── Health Worker Nav Graph ──
        healthWorkerNavGraph(navController, networkMonitor)
    }
}
