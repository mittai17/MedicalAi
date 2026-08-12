package com.swasthai.app.feature.citizen.screening

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.swasthai.app.core.components.*
import com.swasthai.app.domain.model.RiskLevel

/**
 * Multi-step Symptom Check Screen (Screens 05 & 06 from Flow1 wireframe).
 *
 * Step 1: Select symptoms + free text for other
 * Step 2: Duration selection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SymptomCheckScreen(
    onBack: () -> Unit,
    onNavigateToVitals: () -> Unit,
    onNavigateToResult: () -> Unit,
    viewModel: ScreeningViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Show vitals input or result depending on state
    LaunchedEffect(uiState.currentStep) {
        when (uiState.currentStep) {
            ScreeningStep.VITALS_INPUT -> onNavigateToVitals()
            ScreeningStep.RESULT -> onNavigateToResult()
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            SwasthAITopBar(
                title = "Symptom Check",
                showBackButton = true,
                onBackClick = {
                    if (uiState.currentStep == ScreeningStep.SYMPTOM_SELECTION) {
                        onBack()
                    } else {
                        viewModel.goToPreviousStep()
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Step indicator (1-2-3)
            StepIndicator(
                totalSteps = 3,
                currentStep = when (uiState.currentStep) {
                    ScreeningStep.SYMPTOM_SELECTION -> 1
                    ScreeningStep.DURATION_SELECTION -> 2
                    else -> 3
                },
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )

            AnimatedContent(
                targetState = uiState.currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it } + fadeIn() togetherWith
                            slideOutHorizontally { -it }
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith
                            slideOutHorizontally { it }
                    }
                },
                label = "symptom_step_transition"
            ) { step ->
                when (step) {
                    ScreeningStep.SYMPTOM_SELECTION -> {
                        SymptomSelectionStep(
                            symptoms = uiState.availableSymptoms,
                            otherText = uiState.otherSymptomText,
                            onToggleSymptom = viewModel::toggleSymptom,
                            onOtherTextChange = viewModel::updateOtherSymptomText,
                            onNext = {
                                if (viewModel.hasSelectedSymptoms()) {
                                    viewModel.goToNextStep()
                                }
                            }
                        )
                    }
                    ScreeningStep.DURATION_SELECTION -> {
                        DurationSelectionStep(
                            selectedDuration = uiState.selectedDuration,
                            onSelectDuration = viewModel::selectDuration,
                            onBack = viewModel::goToPreviousStep,
                            onNext = {
                                if (uiState.selectedDuration != null) {
                                    viewModel.goToNextStep()
                                }
                            }
                        )
                    }
                    ScreeningStep.PROCESSING -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(56.dp))
                                Text(
                                    text = "Analyzing your symptoms…",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Our AI engine is working",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    else -> {}
                }
            }

            // Error snackbar
            uiState.errorMessage?.let { error ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = viewModel::clearError) {
                            Text("Dismiss")
                        }
                    }
                ) { Text(error) }
            }
        }
    }
}

// ── Step 1: Symptom Selection ──

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SymptomSelectionStep(
    symptoms: List<SelectableSymptom>,
    otherText: String,
    onToggleSymptom: (String) -> Unit,
    onOtherTextChange: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "What is your main problem?",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
        )
        Text(
            text = "Select all that apply",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Symptom chips grid
        symptoms.forEach { symptom ->
            SymptomCheckItem(
                symptomName = symptom.name,
                isSelected = symptom.isSelected,
                onToggle = { onToggleSymptom(symptom.name) }
            )
        }

        // Other symptoms text field
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Any other symptoms?",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        OutlinedTextField(
            value = otherText,
            onValueChange = onOtherTextChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Describe your other symptoms…") },
            shape = RoundedCornerShape(12.dp),
            maxLines = 3,
            leadingIcon = {
                Icon(Icons.Filled.Edit, contentDescription = null)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))
        SwasthAIPrimaryButton(
            text = "Next →",
            onClick = onNext
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SymptomCheckItem(
    symptomName: String,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    Surface(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = borderColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = symptomName,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.RadioButtonUnchecked,
                    contentDescription = "Unselected",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

// ── Step 2: Duration Selection ──

@Composable
private fun DurationSelectionStep(
    selectedDuration: String?,
    onSelectDuration: (String) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val durations = listOf(
        "Less than 1 day",
        "1 – 3 days",
        "3 – 7 days",
        "More than 7 days"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "How long have you had these symptoms?",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
        )

        durations.forEach { duration ->
            DurationOptionItem(
                label = duration,
                isSelected = selectedDuration == duration,
                onClick = { onSelectDuration(duration) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SwasthAIOutlinedButton(
                text = "← Back",
                onClick = onBack,
                modifier = Modifier.weight(1f)
            )
            SwasthAIPrimaryButton(
                text = "Next →",
                onClick = onNext,
                enabled = selectedDuration != null,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun DurationOptionItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = borderColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
