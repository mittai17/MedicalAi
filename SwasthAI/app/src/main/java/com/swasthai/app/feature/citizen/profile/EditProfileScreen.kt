package com.swasthai.app.feature.citizen.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.swasthai.app.core.components.SwasthAIPrimaryButton
import com.swasthai.app.core.components.SwasthAITopBar

/**
 * Edits the patient profile that personalises screenings.
 *
 * The age, sex and conditions are passed to the on-device reasoning engine
 * so risk assessment accounts for the person (e.g. hypertension + high BP).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    viewModel: com.swasthai.app.feature.onboarding.OnboardingViewModel = hiltViewModel()
) {
    val userName by viewModel.userName.collectAsState(initial = null)
    val userPhone by viewModel.userPhone.collectAsState(initial = null)
    val userAge by viewModel.userAge.collectAsState(initial = null)
    val userSex by viewModel.userSex.collectAsState(initial = null)
    val userConditions by viewModel.userConditions.collectAsState(initial = emptyList())

    var name by remember(userName) { mutableStateOf(userName.orEmpty()) }
    var phone by remember(userPhone) { mutableStateOf(userPhone.orEmpty()) }
    var age by remember(userAge) { mutableStateOf(userAge?.toString().orEmpty()) }
    var sex by remember(userSex) { mutableStateOf(userSex.orEmpty()) }
    var conditions by remember(userConditions) { mutableStateOf(userConditions) }

    val conditionOptions = listOf(
        "hypertension" to "High Blood Pressure",
        "diabetes" to "Diabetes",
        "asthma" to "Asthma",
        "copd" to "COPD",
        "heart disease" to "Heart Disease"
    )

    Scaffold(
        topBar = {
            SwasthAITopBar(
                title = "Edit Profile",
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
            Text(
                "This information personalises your screenings and risk assessment.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full name") },
                leadingIcon = { Icon(Icons.Filled.Person, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone") },
                leadingIcon = { Icon(Icons.Filled.Phone, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = age,
                onValueChange = { input -> age = input.filter { it.isDigit() }.take(3) },
                label = { Text("Age") },
                leadingIcon = { Icon(Icons.Filled.Route, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Text("Sex", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Female", "Male", "Other").forEach { option ->
                    FilterChip(
                        selected = sex.equals(option, ignoreCase = true),
                        onClick = { sex = option },
                        label = { Text(option) }
                    )
                }
            }

            Text("Chronic conditions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
            FlowRowOfConditions(
                conditions = conditions,
                options = conditionOptions,
                onToggle = { key, selected ->
                    conditions = if (selected) {
                        (conditions + key).distinct()
                    } else {
                        conditions.filterNot { it == key }
                    }
                }
            )

            SwasthAIPrimaryButton(
                text = "Save & Continue",
                onClick = {
                    val parsedAge = age.toIntOrNull()
                    viewModel.updateProfile(
                        name = name.ifBlank { null },
                        phone = phone.ifBlank { null },
                        age = parsedAge,
                        sex = sex.ifBlank { null },
                        conditions = conditions
                    )
                    onBack()
                },
                leadingIcon = Icons.Filled.Check
            )
        }
    }
}

@Composable
private fun FlowRowOfConditions(
    conditions: List<String>,
    options: List<Pair<String, String>>,
    onToggle: (key: String, selected: Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (key, label) ->
                    FilterChip(
                        selected = conditions.contains(key),
                        onClick = { onToggle(key, !conditions.contains(key)) },
                        label = { Text(label) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}