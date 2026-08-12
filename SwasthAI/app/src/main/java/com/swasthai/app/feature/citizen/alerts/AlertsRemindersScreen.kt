package com.swasthai.app.feature.citizen.alerts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.swasthai.app.core.components.SwasthAITopBar
import com.swasthai.app.core.theme.SwasthAIColors

/**
 * Alerts & Reminders Screen (Screen 13 from Flow1 wireframe).
 *
 * Two tabs: All / Reminders
 * Cards for: Medicine Reminder, Follow-up, Hydration, Sync Reminder
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsRemindersScreen(
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            SwasthAITopBar(
                title = "Alerts & Reminders",
                showBackButton = true,
                onBackClick = onBack,
                actions = {
                    IconButton(onClick = { /* TODO: Add reminder */ }) {
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

            val alerts = buildAlerts()
            val displayAlerts = if (selectedTab == 0) alerts else alerts.filter { it.isReminder }

            if (displayAlerts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.NotificationsOff, null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.outlineVariant)
                        Text("No alerts", style = MaterialTheme.typography.titleMedium)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(displayAlerts.size) { index ->
                        AlertCard(alert = displayAlerts[index])
                    }
                }
            }
        }
    }
}

private data class AlertItem(
    val icon: ImageVector,
    val iconColor: Color,
    val title: String,
    val subtitle: String,
    val time: String,
    val isReminder: Boolean,
    val badgeColor: Color? = null
)

private fun buildAlerts() = listOf(
    AlertItem(
        icon = Icons.Filled.Medication,
        iconColor = Color(0xFF2563EB),
        title = "Medicine Reminder",
        subtitle = "Take Paracetamol 500mg — 1 tablet",
        time = "Every day at 8:00 AM",
        isReminder = true,
        badgeColor = Color(0xFF2563EB)
    ),
    AlertItem(
        icon = Icons.Filled.Schedule,
        iconColor = Color(0xFF7C3AED),
        title = "Follow-up Reminder",
        subtitle = "Your follow-up with Dr. Sharma is tomorrow",
        time = "Tomorrow, 10:30 AM",
        isReminder = true,
        badgeColor = Color(0xFF7C3AED)
    ),
    AlertItem(
        icon = Icons.Filled.WaterDrop,
        iconColor = Color(0xFF0891B2),
        title = "Hydration Reminder",
        subtitle = "Drink 8 glasses of water daily",
        time = "Every 2 hours",
        isReminder = true
    ),
    AlertItem(
        icon = Icons.Filled.Sync,
        iconColor = SwasthAIColors.SyncPending,
        title = "Sync Reminder",
        subtitle = "You have 3 unsynced records",
        time = "Last synced 2 days ago",
        isReminder = false,
        badgeColor = SwasthAIColors.SyncPending
    ),
    AlertItem(
        icon = Icons.Filled.Vaccines,
        iconColor = SwasthAIColors.RiskLow,
        title = "Vaccination Due",
        subtitle = "Your annual flu vaccination is due",
        time = "This month",
        isReminder = true,
        badgeColor = SwasthAIColors.RiskLow
    )
)

@Composable
private fun AlertCard(alert: AlertItem) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = alert.iconColor.copy(alpha = 0.15f),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        alert.icon,
                        contentDescription = null,
                        tint = alert.iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = alert.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (alert.badgeColor != null) {
                        Badge(containerColor = alert.badgeColor.copy(alpha = 0.2f)) {
                            Text("•", color = alert.badgeColor)
                        }
                    }
                }
                Text(
                    text = alert.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = alert.time,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // Dismiss button
            IconButton(
                onClick = { },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Filled.Close, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
            }
        }
    }
}
