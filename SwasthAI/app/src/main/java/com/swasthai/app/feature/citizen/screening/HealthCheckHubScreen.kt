package com.swasthai.app.feature.citizen.screening

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import com.swasthai.app.core.components.SwasthAITopBar
import com.swasthai.app.feature.citizen.voice.VoiceCommandHubContent

/**
 * The three ways a citizen can run a health check, in one place.
 */
enum class HealthCheckMode(val label: String, val icon: ImageVector) {
    SYMPTOMS("Symptoms", Icons.Filled.EditNote),
    PHOTO("Photo", Icons.Filled.CameraAlt),
    VOICE("Voice", Icons.Filled.Mic);

    companion object {
        /** Resolves the `mode` segment of the health_check route. */
        fun from(route: String): HealthCheckMode = when (route) {
            "photo" -> PHOTO
            "voice" -> VOICE
            else -> SYMPTOMS
        }
    }
}

/**
 * Unified "Health Check" hub.
 *
 * Combines the typed symptom check, the photo/image scan and the voice
 * assistant under a single screen. All three run against the same
 * activity-scoped [ScreeningViewModel], so a check started in one method can
 * be finished in another (the result screen is shared). Switching methods
 * starts a clean check if a previous one already completed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthCheckHubScreen(
    onNavigateToVitals: () -> Unit,
    onNavigateToResult: () -> Unit,
    onNavigateToConnect: () -> Unit = {},
    onNavigateToRecords: () -> Unit = {},
    initialMode: HealthCheckMode = HealthCheckMode.SYMPTOMS,
    viewModel: ScreeningViewModel = hiltViewModel(
        LocalContext.current as ViewModelStoreOwner
    )
) {
    var inputMethod by rememberSaveable(initialMode) { mutableStateOf(initialMode) }

    // Start each method on a clean check: drop stale data left behind by a
    // different method or a completed run in the shared ScreeningViewModel.
    LaunchedEffect(inputMethod) {
        if (viewModel.uiState.value.currentStep != ScreeningStep.SYMPTOM_SELECTION) {
            viewModel.resetScreening()
        }
    }

    Scaffold(
        topBar = {
            SwasthAITopBar(title = "Health Check")
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(selectedTabIndex = inputMethod.ordinal) {
                HealthCheckMode.entries.forEach { mode ->
                    Tab(
                        selected = inputMethod == mode,
                        onClick = { inputMethod = mode },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = mode.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = mode.label,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    )
                }
            }

            when (inputMethod) {
                HealthCheckMode.SYMPTOMS -> SymptomCheckContent(
                    onNavigateToVitals = onNavigateToVitals,
                    onNavigateToResult = onNavigateToResult,
                    viewModel = viewModel,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                )

                HealthCheckMode.PHOTO -> ImageCheckContent(
                    onNavigateToResult = onNavigateToResult,
                    viewModel = viewModel,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                )

                HealthCheckMode.VOICE -> VoiceCommandHubContent(
                    onNavigateToScreeningResult = onNavigateToResult,
                    onNavigateToConnect = onNavigateToConnect,
                    onNavigateToRecords = onNavigateToRecords,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                )
            }
        }
    }
}