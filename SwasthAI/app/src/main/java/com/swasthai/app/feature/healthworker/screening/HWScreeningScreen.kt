package com.swasthai.app.feature.healthworker.screening

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.swasthai.app.core.components.*
import com.swasthai.app.feature.citizen.screening.*

/**
 * HW Screening Screen (Screen 19).
 *
 * Reuses the citizen ScreeningViewModel.
 * Health worker selects method (Symptom / Voice / Image / Vitals),
 * then the standard screening wizard runs with patient context shown.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HWScreeningScreen(
    patientId: String,
    patientName: String,
    onBack: () -> Unit,
    onNavigateToResult: () -> Unit,
    viewModel: ScreeningViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.currentStep) {
        if (uiState.currentStep == ScreeningStep.RESULT) onNavigateToResult()
    }

    Scaffold(
        topBar = {
            SwasthAITopBar(
                title = "Screen: $patientName",
                showBackButton = true,
                onBackClick = {
                    if (uiState.currentStep == ScreeningStep.SYMPTOM_SELECTION) onBack()
                    else viewModel.goToPreviousStep()
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Step indicator
            StepIndicator(
                totalSteps = 3,
                currentStep = when (uiState.currentStep) {
                    ScreeningStep.SYMPTOM_SELECTION -> 1
                    ScreeningStep.DURATION_SELECTION -> 2
                    else -> 3
                },
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )

            when (uiState.currentStep) {
                ScreeningStep.SYMPTOM_SELECTION, ScreeningStep.DURATION_SELECTION -> {
                    SymptomCheckScreen(
                        onBack = onBack,
                        onNavigateToVitals = { viewModel.goToNextStep() },
                        onNavigateToResult = onNavigateToResult,
                        viewModel = viewModel
                    )
                }
                ScreeningStep.VITALS_INPUT -> {
                    VitalsInputScreen(
                        onBack = viewModel::goToPreviousStep,
                        onNavigateToResult = onNavigateToResult,
                        viewModel = viewModel
                    )
                }
                ScreeningStep.PROCESSING -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(56.dp))
                            Text("Running AI Diagnosis…", style = MaterialTheme.typography.titleMedium)
                            Text("Analysing ${patientName}'s symptoms", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                else -> {}
            }

            uiState.errorMessage?.let {
                Snackbar(modifier = Modifier.padding(16.dp), action = {
                    TextButton(onClick = viewModel::clearError) { Text("Dismiss") }
                }) { Text(it) }
            }
        }
    }
}
