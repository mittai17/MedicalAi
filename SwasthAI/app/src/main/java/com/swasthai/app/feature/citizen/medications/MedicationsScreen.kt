package com.swasthai.app.feature.citizen.medications

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swasthai.app.domain.model.Medication
import com.swasthai.app.domain.model.MedicationDose
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationsScreen(
    onBack: () -> Unit,
    viewModel: MedicationsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("💊 Medications") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.setAddMedicineFlowActive(true) }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Medication")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Today's Medication Progress
            item {
                Text(
                    text = "Today's Medication",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                val taken = uiState.todayProgress.first
                val total = uiState.todayProgress.second
                val progress = if (total > 0) taken.toFloat() / total.toFloat() else 0f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$taken / $total doses taken",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Reminders Section
            if (uiState.upcomingReminders.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Upcoming Reminders",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(uiState.upcomingReminders) { (med, dose) ->
                    ReminderCard(
                        medication = med, 
                        dose = dose,
                        onTaken = { viewModel.markDoseTaken(dose.id) },
                        onSnooze = { viewModel.snoozeDose(dose.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Current Medicines Section
            item {
                Text(
                    text = "Current Medicines",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            if (uiState.currentMedicines.isEmpty()) {
                item {
                    Text("No medicines added.", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                items(uiState.currentMedicines) { medicine ->
                    MedicineCard(
                        medication = medicine,
                        onClick = { viewModel.selectMedicine(medicine) }
                    )
                }
            }
        }

        if (uiState.isAddMedicineFlowActive) {
            AddMedicineDialog(
                onDismiss = { viewModel.setAddMedicineFlowActive(false) },
                onSave = { name, dosage, frequency, reminderTimes, startDate, endDate, foodTiming ->
                    viewModel.addMedicine(name, dosage, frequency, reminderTimes, startDate, endDate, foodTiming)
                }
            )
        }
        
        uiState.selectedMedicine?.let { med ->
            MedicineDetailDialog(
                medication = med,
                onDismiss = { viewModel.clearSelectedMedicine() },
                onDelete = { viewModel.deleteMedicine(med.id) }
            )
        }
    }
}

@Composable
fun ReminderCard(medication: Medication, dose: MedicationDose, onTaken: () -> Unit, onSnooze: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Schedule, contentDescription = "Schedule", tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("${medication.name} ${medication.dosage}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text("${formatTime(dose.scheduledTimeMillis)} • ${getDayLabel(dose.scheduledTimeMillis)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (medication.foodTiming.isNotBlank()) {
                    Text(medication.foodTiming, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row {
                IconButton(onClick = onSnooze) {
                    Icon(Icons.Filled.Snooze, contentDescription = "Snooze", tint = MaterialTheme.colorScheme.secondary)
                }
                IconButton(onClick = onTaken) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = "Mark Taken", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun MedicineCard(medication: Medication, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.MedicalServices, contentDescription = "Medicine", tint = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("${medication.name} (${medication.dosage})", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(medication.frequency, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMedicineDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, List<Long>, Long, Long?, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("Once daily") }
    var foodTiming by remember { mutableStateOf("After food") }
    
    var durationSelection by remember { mutableStateOf("No duration (1 day)") }
    var customDurationDays by remember { mutableStateOf("") }
    val durationOptionsList = listOf("No duration (1 day)", "3 days", "7 days", "Custom")
    
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
    
    val frequencyOptions = listOf("Once daily", "Twice daily", "Three times daily", "Custom")
    val foodOptions = listOf("Before food", "With food", "After food", "No specific time")
    
    // Store reminder times as milliseconds offset from start of day
    val defaultTime1 = 8 * 3600 * 1000L // 8 AM
    val defaultTime2 = 20 * 3600 * 1000L // 8 PM
    val defaultTime3 = 14 * 3600 * 1000L // 2 PM
    
    val time1 by remember { mutableStateOf(defaultTime1) }
    val time2 by remember { mutableStateOf(defaultTime2) }
    val time3 by remember { mutableStateOf(defaultTime3) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Add Medicine", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                
                if (showDatePicker) {
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = { showDatePicker = false }) { Text("OK") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                        }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }

                Text(
                    text = "Reminder Date: ${formatDate(datePickerState.selectedDateMillis ?: System.currentTimeMillis())}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                        .padding(vertical = 8.dp)
                )
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Medicine Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = dosage,
                    onValueChange = { dosage = it },
                    label = { Text("Dosage (e.g. 500mg)") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text("Duration", style = MaterialTheme.typography.labelLarge)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    durationOptionsList.forEach { opt ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { durationSelection = opt }) {
                            RadioButton(selected = durationSelection == opt, onClick = { durationSelection = opt })
                            Text(opt)
                        }
                    }
                    if (durationSelection == "Custom") {
                        OutlinedTextField(
                            value = customDurationDays,
                            onValueChange = { customDurationDays = it },
                            label = { Text("Duration (days)") },
                            modifier = Modifier.fillMaxWidth().padding(start = 32.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }
                
                Text("Frequency", style = MaterialTheme.typography.labelLarge)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    frequencyOptions.forEach { opt ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { frequency = opt }) {
                            RadioButton(selected = frequency == opt, onClick = { frequency = opt })
                            Text(opt)
                        }
                    }
                }
                
                Text("Reminder Times", style = MaterialTheme.typography.labelLarge)
                Text("Time 1: 8:00 AM", style = MaterialTheme.typography.bodyMedium)
                if (frequency == "Twice daily" || frequency == "Three times daily" || frequency == "Custom") {
                    Text("Time 2: 8:00 PM", style = MaterialTheme.typography.bodyMedium)
                }
                if (frequency == "Three times daily" || frequency == "Custom") {
                    Text("Time 3: 2:00 PM", style = MaterialTheme.typography.bodyMedium)
                }

                HorizontalDivider()
                
                Text("Food Timing", style = MaterialTheme.typography.labelLarge)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    foodOptions.forEach { opt ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { foodTiming = opt }) {
                            RadioButton(selected = foodTiming == opt, onClick = { foodTiming = opt })
                            Text(opt)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val times = mutableListOf(time1)
                            if (frequency == "Twice daily") times.add(time2)
                            if (frequency == "Three times daily") { times.add(time3); times.add(time2) }
                            if (frequency == "Custom") { times.add(time2) }
                            
                            val daysToAdd = when (durationSelection) {
                                "3 days" -> 3
                                "7 days" -> 7
                                "Custom" -> customDurationDays.toIntOrNull() ?: 1
                                else -> 1
                            }
                            
                            val startDate = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                            val endDate = if (daysToAdd > 1) startDate + (daysToAdd - 1) * 24 * 60 * 60 * 1000L else startDate
                            
                            onSave(name, dosage, frequency, times.sorted(), startDate, endDate, foodTiming)
                        },
                        enabled = name.isNotBlank() && dosage.isNotBlank()
                    ) {
                        Text("Save Medicine")
                    }
                }
            }
        }
    }
}

@Composable
fun MedicineDetailDialog(
    medication: Medication,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(medication.name, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Dosage: ${medication.dosage}")
                Text("Frequency: ${medication.frequency}")
                if (medication.foodTiming.isNotBlank()) {
                    Text("Food Timing: ${medication.foodTiming}")
                }
                Text("Start Date: ${formatDate(medication.startDate)}")
                medication.endDate?.let {
                    Text("End Date: ${formatDate(it)}")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDelete()
                    onDismiss()
                },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete")
            }
        }
    )
}

private fun formatTime(millis: Long): String {
    val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
    return formatter.format(Date(millis))
}

private fun formatDate(millis: Long): String {
    val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return formatter.format(Date(millis))
}

private fun getDayLabel(millis: Long): String {
    val calendar = Calendar.getInstance()
    val today = calendar.get(Calendar.DAY_OF_YEAR)
    val todayYear = calendar.get(Calendar.YEAR)
    
    calendar.timeInMillis = millis
    val doseDay = calendar.get(Calendar.DAY_OF_YEAR)
    val doseYear = calendar.get(Calendar.YEAR)
    
    return when {
        todayYear == doseYear && today == doseDay -> "Today"
        todayYear == doseYear && doseDay == today + 1 -> "Tomorrow"
        todayYear == doseYear && doseDay == today - 1 -> "Yesterday"
        else -> formatDate(millis)
    }
}
