package com.swasthai.app.feature.citizen.alerts

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.core.content.ContextCompat
import com.swasthai.app.core.components.SwasthAITopBar
import com.swasthai.app.core.theme.SwasthAIColors
import com.swasthai.app.domain.model.Reminder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Alerts & Reminders — real data: persisted reminders, follow-up alerts
 * derived from past flagged screenings, and the actual offline sync status.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsRemindersScreen(
    onBack: () -> Unit,
    viewModel: AlertsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* grant result handled by the OS-supplied notification permission */ }

    Scaffold(
        topBar = {
            SwasthAITopBar(
                title = "Alerts & Reminders",
                showBackButton = true,
                onBackClick = onBack,
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Reminder")
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
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("All") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Reminders") }
                )
            }

            if (uiState.loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            val showFollowUps = selectedTab == 0
            val hasFollowUps = uiState.followUps.isNotEmpty()
            val hasReminders = uiState.reminders.isNotEmpty()

            if (!hasFollowUps && !hasReminders && selectedTab == 0) {
                EmptyState(icon = Icons.Filled.NotificationsOff, text = "No alerts yet")
                return@Column
            }
            if (selectedTab == 1 && !hasReminders) {
                EmptyState(icon = Icons.Filled.NotificationsOff, text = "No reminders yet")
                return@Column
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Sync card.
                if (selectedTab == 0) {
                    item {
                        SyncCard(
                            syncCount = uiState.syncCount,
                            isSyncing = uiState.isSyncing,
                            message = uiState.lastSyncMessage,
                            lastSyncTimestamp = uiState.lastSyncTimestamp,
                            onSync = { viewModel.syncNow() }
                        )
                    }
                }

                // Real follow-up alerts.
                if (showFollowUps) {
                    uiState.followUps.forEach { alert ->
                        item(key = "followup_${alert.screeningId}") {
                            FollowUpCard(
                                alert = alert,
                                onDismiss = { viewModel.dismissFollowUp(alert.screeningId) }
                            )
                        }
                    }
                }

                // Persisted reminders.
                uiState.reminders.forEach { reminder ->
                    item(key = "reminder_${reminder.id}") {
                        ReminderCard(
                            reminder = reminder,
                            onDismiss = { viewModel.removeReminder(reminder.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddReminderDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { title, note, time ->
                viewModel.addReminder(title, note, time)
                showAddDialog = false
                // Ask for the runtime permission only when the reminder is real
                // and notifications aren't granted yet (Android 13+).
                val appContext = context.applicationContext
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(
                        appContext,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        )
    }
}

@Composable
private fun EmptyState(icon: ImageVector, text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.outlineVariant)
            Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SyncCard(
    syncCount: Int?,
    isSyncing: Boolean,
    message: String?,
    lastSyncTimestamp: Long,
    onSync: () -> Unit
) {
    ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SwasthAIColors.SyncPending.copy(alpha = 0.15f),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isSyncing) {
                        CircularProgressIndicator(strokeWidth = 3.dp, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Filled.Sync, null, tint = SwasthAIColors.SyncPending, modifier = Modifier.size(24.dp))
                    }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Offline Sync", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    text = message
                        ?: if (syncCount != null && syncCount > 0) "$syncCount record(s) waiting to sync"
                        else (if (syncCount == 0) "All records synced" else "Checking sync status…"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (lastSyncTimestamp > 0L) {
                    val time = SimpleDateFormat("hh:mm a", Locale.getDefault())
                        .format(Date(lastSyncTimestamp))
                    Text(
                        text = "Last synced $time",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            if (!isSyncing) {
                TextButton(onClick = onSync) { Text("Sync") }
            }
        }
    }
}

@Composable
private fun FollowUpCard(alert: FollowUpAlert, onDismiss: () -> Unit) {
    val riskColor = when (alert.riskLevel) {
        com.swasthai.app.domain.model.RiskLevel.MODERATE -> SwasthAIColors.RiskModerate
        com.swasthai.app.domain.model.RiskLevel.HIGH -> SwasthAIColors.RiskHigh
        else -> SwasthAIColors.RiskLow
    }
    val dateStr = SimpleDateFormat("dd MMM",
        Locale.getDefault()).format(Date(alert.date))
    AlertCard(
        icon = Icons.Filled.Schedule,
        iconColor = riskColor,
        title = "Follow-up: ${alert.disease}",
        subtitle = alert.hint,
        time = "From screening on $dateStr",
        onDismiss = onDismiss
    )
}

@Composable
private fun ReminderCard(reminder: Reminder, onDismiss: () -> Unit) {
    val timeLabel = if (reminder.timeInMillis > 0) {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(reminder.timeInMillis))
    } else "Keep in mind"
    AlertCard(
        icon = Icons.Filled.Notifications,
        iconColor = MaterialTheme.colorScheme.primary,
        title = reminder.title,
        subtitle = reminder.note.ifBlank { "Reminder" },
        time = timeLabel,
        onDismiss = onDismiss
    )
}

@Composable
private fun AlertCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    time: String,
    onDismiss: () -> Unit
) {
    ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = iconColor.copy(alpha = 0.15f),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = iconColor, modifier = Modifier.size(24.dp))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddReminderDialog(
    onDismiss: () -> Unit,
    onAdd: (title: String, note: String, time: Long) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedMillis by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pickedDateUtcMillis by remember { mutableStateOf<Long?>(null) }
    var pickedHour by remember { mutableStateOf(9) }
    var pickedMinute by remember { mutableStateOf(0) }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = pickedDateUtcMillis)
    val timePickerState = rememberTimePickerState(initialHour = pickedHour, initialMinute = pickedMinute)

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickedDateUtcMillis = datePickerState.selectedDateMillis
                        showDatePicker = false
                        showTimePicker = true
                    }
                ) { Text("Next") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Pick time") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickedHour = timePickerState.hour
                        pickedMinute = timePickerState.minute
                        val date = pickedDateUtcMillis
                        val millis = if (date != null && date > 0L) {
                            combineDateAndTime(date, pickedHour, pickedMinute)
                        } else {
                            inOneHour()
                        }
                        selectedMillis = millis
                        showTimePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Reminder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Reminder") },
                    placeholder = { Text("e.g. Take medicine at night") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    singleLine = true
                )
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Schedule, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        selectedMillis?.let {
                            SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(it))
                        } ?: "Pick date & time"
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onAdd(title, note, selectedMillis ?: inOneHour())
                }
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/** Merges a DatePicker UTC-midnight value with a local hour/minute. */
private fun combineDateAndTime(dateUtcMillis: Long, hour: Int, minute: Int): Long {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = dateUtcMillis
    }
    return Calendar.getInstance().apply {
        set(Calendar.YEAR, utc.get(Calendar.YEAR))
        set(Calendar.MONTH, utc.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, utc.get(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

/** Default reminder time — one hour from now (minute-rounded). */
private fun inOneHour(): Long {
    return Calendar.getInstance().apply {
        add(Calendar.HOUR_OF_DAY, 1)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}