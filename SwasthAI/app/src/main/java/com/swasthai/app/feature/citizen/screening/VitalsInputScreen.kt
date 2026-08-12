package com.swasthai.app.feature.citizen.screening

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.swasthai.app.core.components.SwasthAIPrimaryButton
import com.swasthai.app.core.components.SwasthAITopBar

/**
 * Vitals Input Screen — matches the Vitals section in Flow1 wireframe.
 *
 * Collects:
 *  - Body Temperature (°C)
 *  - Pulse Rate (bpm)
 *  - SpO₂ (%)
 *  - Blood Pressure (mmHg)
 *  - Weight (kg)
 *  - Height (cm)
 *
 * Vitals are optional — user can skip and go directly to diagnosis.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VitalsInputScreen(
    onBack: () -> Unit,
    onNavigateToResult: () -> Unit,
    viewModel: ScreeningViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.currentStep) {
        when (uiState.currentStep) {
            ScreeningStep.RESULT -> onNavigateToResult()
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            SwasthAITopBar(
                title = "Enter Vitals",
                showBackButton = true,
                onBackClick = onBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Text(
                text = "Record Vital Signs",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Vitals help improve diagnosis accuracy. All fields are optional.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // ── Vitals input cards ──

            VitalsCard(
                icon = Icons.Filled.DeviceThermostat,
                title = "Body Temperature",
                subtitle = "Normal: 36.1 – 37.2 °C"
            ) {
                VitalTextField(
                    value = uiState.temperature,
                    onValueChange = viewModel::updateTemperature,
                    label = "Temperature",
                    suffix = "°C",
                    placeholder = "36.5"
                )
            }

            VitalsCard(
                icon = Icons.Filled.Favorite,
                title = "Pulse Rate",
                subtitle = "Normal: 60 – 100 bpm"
            ) {
                VitalTextField(
                    value = uiState.pulse,
                    onValueChange = viewModel::updatePulse,
                    label = "Pulse Rate",
                    suffix = "bpm",
                    placeholder = "72"
                )
            }

            VitalsCard(
                icon = Icons.Filled.Air,
                title = "SpO₂ (Oxygen Saturation)",
                subtitle = "Normal: ≥ 95%"
            ) {
                VitalTextField(
                    value = uiState.spo2,
                    onValueChange = viewModel::updateSpo2,
                    label = "SpO₂",
                    suffix = "%",
                    placeholder = "98"
                )
            }

            VitalsCard(
                icon = Icons.Filled.MonitorHeart,
                title = "Blood Pressure",
                subtitle = "Normal: 120/80 mmHg"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VitalTextField(
                        value = uiState.bloodPressureSystolic,
                        onValueChange = viewModel::updateBPSystolic,
                        label = "Systolic",
                        suffix = "mmHg",
                        placeholder = "120",
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "/",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    VitalTextField(
                        value = uiState.bloodPressureDiastolic,
                        onValueChange = viewModel::updateBPDiastolic,
                        label = "Diastolic",
                        suffix = "mmHg",
                        placeholder = "80",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Weight & Height side by side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ElevatedCard(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Scale,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text("Weight", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        }
                        VitalTextField(
                            value = uiState.weight,
                            onValueChange = viewModel::updateWeight,
                            label = "Weight",
                            suffix = "kg",
                            placeholder = "65"
                        )
                    }
                }
                ElevatedCard(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Height,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text("Height", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        }
                        VitalTextField(
                            value = uiState.height,
                            onValueChange = viewModel::updateHeight,
                            label = "Height",
                            suffix = "cm",
                            placeholder = "170"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action buttons
            SwasthAIPrimaryButton(
                text = "Analyse Now",
                onClick = { viewModel.goToNextStep() },
                leadingIcon = Icons.Filled.Psychology
            )

            OutlinedButton(
                onClick = { viewModel.skipVitalsAndDiagnose() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Skip Vitals",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.errorMessage?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun VitalsCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VitalTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    suffix: String,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        suffix = { Text(suffix) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = RoundedCornerShape(12.dp)
    )
}
