package com.swasthai.app.feature.healthworker.screening

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
import com.swasthai.app.core.components.SwasthAIPrimaryButton
import com.swasthai.app.core.components.SwasthAITopBar

/**
 * Schedule Follow-up Screen (Screen 21).
 *
 * Date picker, time, visit type, notes, assigned CHW.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleFollowUpScreen(
    patientName: String,
    onBack: () -> Unit,
    onScheduled: () -> Unit
) {
    var selectedDate by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") }
    var visitType by remember { mutableStateOf("Home Visit") }
    var notes by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            SwasthAITopBar(
                title = "Schedule Follow-up",
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
            Text("For: $patientName", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            // Date
            ElevatedCard(shape = RoundedCornerShape(14.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Visit Date & Time", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = selectedDate,
                        onValueChange = { selectedDate = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Date (DD/MM/YYYY)") },
                        leadingIcon = { Icon(Icons.Filled.CalendarToday, null) },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = selectedTime,
                        onValueChange = { selectedTime = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Time (HH:MM)") },
                        leadingIcon = { Icon(Icons.Filled.Schedule, null) },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }

            // Visit type
            ElevatedCard(shape = RoundedCornerShape(14.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Visit Type", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Home Visit", "PHC Visit", "Teleconsult").forEach { type ->
                            FilterChip(
                                selected = visitType == type,
                                onClick = { visitType = type },
                                label = { Text(type) },
                                leadingIcon = if (visitType == type) {
                                    { Icon(Icons.Filled.Check, null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                }
            }

            // Notes
            ElevatedCard(shape = RoundedCornerShape(14.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Notes", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Additional notes") },
                        placeholder = { Text("e.g. bring medication list, check BP…") },
                        leadingIcon = { Icon(Icons.Filled.Notes, null) },
                        shape = RoundedCornerShape(12.dp),
                        minLines = 3,
                        maxLines = 5
                    )
                }
            }

            SwasthAIPrimaryButton(
                text = if (isSaving) "Scheduling…" else "Schedule Follow-up",
                onClick = {
                    isSaving = true
                    onScheduled()
                },
                enabled = selectedDate.isNotBlank() && !isSaving,
                leadingIcon = Icons.Filled.CalendarMonth
            )
        }
    }
}

/**
 * Refer to PHC Screen (Screen 22).
 *
 * Facility selector, urgency, reason, transport arrangement.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferToPHCScreen(
    patientName: String,
    onBack: () -> Unit,
    onReferred: () -> Unit
) {
    var selectedFacility by remember { mutableStateOf("") }
    var urgency by remember { mutableStateOf("Normal") }
    var reason by remember { mutableStateOf("") }
    var transport by remember { mutableStateOf("Self") }
    var isSaving by remember { mutableStateOf(false) }

    val facilities = listOf("PHC Block A", "CHC District Hospital", "Sub-District Hospital", "Referral Hospital")

    Scaffold(
        topBar = {
            SwasthAITopBar(
                title = "Refer to PHC / Hospital",
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
            Text("Referral for: $patientName", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            // Facility selector
            ElevatedCard(shape = RoundedCornerShape(14.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select Facility", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    facilities.forEach { facility ->
                        val selected = selectedFacility == facility
                        Surface(
                            onClick = { selectedFacility = facility },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(
                                if (selected) 2.dp else 1.dp,
                                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.LocalHospital, null, tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
                                    Text(facility, style = MaterialTheme.typography.bodyMedium, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                                }
                                if (selected) Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            // Urgency
            ElevatedCard(shape = RoundedCornerShape(14.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Urgency Level", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Normal", "Urgent", "Emergency").forEach { u ->
                            FilterChip(
                                selected = urgency == u,
                                onClick = { urgency = u },
                                label = { Text(u) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = when (u) {
                                        "Emergency" -> com.swasthai.app.core.theme.SwasthAIColors.RiskHighBackground
                                        "Urgent" -> com.swasthai.app.core.theme.SwasthAIColors.RiskModerateBackground
                                        else -> MaterialTheme.colorScheme.primaryContainer
                                    }
                                )
                            )
                        }
                    }
                }
            }

            // Reason
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Reason for Referral") },
                leadingIcon = { Icon(Icons.Filled.Notes, null) },
                shape = RoundedCornerShape(12.dp),
                minLines = 3
            )

            SwasthAIPrimaryButton(
                text = if (isSaving) "Submitting…" else "Submit Referral",
                onClick = {
                    isSaving = true
                    onReferred()
                },
                enabled = selectedFacility.isNotBlank() && !isSaving,
                leadingIcon = Icons.Filled.Send
            )
        }
    }
}
