package com.swasthai.app.feature.healthworker.patients

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.swasthai.app.core.components.SwasthAIPrimaryButton
import com.swasthai.app.core.components.SwasthAITopBar

/**
 * Add Patient Screen (Screen 17).
 *
 * Collects: Full Name*, Age*, Gender*, Village, Phone, Aadhaar Number
 * Saves locally (offline-first). On success navigates to PatientDetail.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPatientScreen(
    onBack: () -> Unit,
    onPatientAdded: (String) -> Unit,
    viewModel: PatientViewModel = hiltViewModel()
) {
    val uiState by viewModel.addUiState.collectAsState()

    // Navigate when patient is saved
    LaunchedEffect(uiState.savedPatientId) {
        uiState.savedPatientId?.let { onPatientAdded(it) }
    }

    Scaffold(
        topBar = {
            SwasthAITopBar(
                title = "Add New Patient",
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Text("Patient Registration", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Fields marked * are required",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // ── Full Name ──
            FormField(
                label = "Full Name *",
                value = uiState.name,
                onValueChange = viewModel::updateName,
                placeholder = "Enter patient's full name",
                leadingIcon = Icons.Filled.Person
            )

            // ── Age ──
            FormField(
                label = "Age *",
                value = uiState.age,
                onValueChange = viewModel::updateAge,
                placeholder = "e.g. 35",
                leadingIcon = Icons.Filled.Cake,
                keyboardType = KeyboardType.Number,
                suffix = "yrs"
            )

            // ── Gender ──
            Text("Gender *", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Male", "Female", "Other").forEach { gender ->
                    FilterChip(
                        selected = uiState.gender == gender,
                        onClick = { viewModel.updateGender(gender) },
                        label = { Text(gender) },
                        leadingIcon = if (uiState.gender == gender) {
                            { Icon(Icons.Filled.Check, null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            // ── Village / Area ──
            FormField(
                label = "Village / Area",
                value = uiState.village,
                onValueChange = viewModel::updateVillage,
                placeholder = "Enter village or locality",
                leadingIcon = Icons.Filled.LocationOn
            )

            // ── Phone ──
            FormField(
                label = "Phone Number",
                value = uiState.phone,
                onValueChange = viewModel::updatePhone,
                placeholder = "10-digit mobile number",
                leadingIcon = Icons.Filled.Phone,
                keyboardType = KeyboardType.Phone
            )

            // ── Aadhaar ──
            FormField(
                label = "Aadhaar Number",
                value = uiState.aadhar,
                onValueChange = viewModel::updateAadhar,
                placeholder = "12-digit Aadhaar",
                leadingIcon = Icons.Filled.Badge,
                keyboardType = KeyboardType.Number
            )

            // Error
            uiState.error?.let { error ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Error, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            SwasthAIPrimaryButton(
                text = if (uiState.isSaving) "Saving…" else "Register Patient",
                onClick = viewModel::savePatient,
                enabled = !uiState.isSaving && viewModel.isAddPatientFormValid(),
                leadingIcon = Icons.Filled.PersonAdd
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    suffix: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(leadingIcon, null) },
        suffix = suffix?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(12.dp)
    )
}
