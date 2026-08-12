package com.swasthai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.swasthai.app.core.theme.SwasthAITheme
import com.swasthai.app.core.utils.NetworkMonitor
import com.swasthai.app.data.local.datastore.UserPreferences
import com.swasthai.app.domain.model.UserRole
import com.swasthai.app.navigation.SwasthAINavHost
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Main entry Activity for SwasthAI.
 *
 * Uses edge-to-edge rendering with the splash screen API.
 * The theme dynamically switches between Citizen (blue) and
 * Health Worker (green) based on the logged-in user's role.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferences: UserPreferences

    @Inject
    lateinit var networkMonitor: NetworkMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            val userRole by userPreferences.userRoleFlow.collectAsState(initial = null)

            SwasthAITheme(
                userRole = userRole ?: UserRole.CITIZEN
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    SwasthAINavHost(networkMonitor = networkMonitor)
                }
            }
        }
    }
}
