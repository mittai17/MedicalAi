package com.swasthai.app.feature.healthworker.dashboard

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.swasthai.app.core.components.*
import com.swasthai.app.core.theme.SwasthAIColors
import com.swasthai.app.domain.model.Patient
import com.swasthai.app.domain.model.Referral
import java.text.SimpleDateFormat
import java.util.*

/**
 * Health Worker Dashboard Screen (Screen 15).
 *
 * Displays:
 *  - Greeting + connection badge
 *  - Stats row: patients, referrals, screenings today, alerts
 *  - Quick action grid: Add Patient, New Screening, Referrals, Reports, Sync, Settings
 *  - Pending referrals list
 *  - Recent patients list
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HWDashboardScreen(
    onAddPatient: () -> Unit,
    onPatientList: () -> Unit,
    onPatientDetail: (String) -> Unit,
    onReports: () -> Unit,
    onSyncData: () -> Unit,
    onAlerts: () -> Unit,
    onProfile: () -> Unit,
    onSettings: () -> Unit,
    viewModel: HWDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            SwasthAITopBar(
                title = "SwasthAI",
                isOnline = uiState.isOnline,
                actions = {
                    if (uiState.criticalAlerts > 0) {
                        BadgedBox(badge = {
                            Badge { Text(uiState.criticalAlerts.toString()) }
                        }) {
                            IconButton(onClick = onAlerts) {
                                Icon(Icons.Filled.Notifications, "Alerts")
                            }
                        }
                    }
                    IconButton(onClick = onProfile) {
                        Icon(Icons.Filled.AccountCircle, "Profile", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        },
        bottomBar = {
            SwasthAIBottomBar(
                items = hwBottomNavItems,
                selectedRoute = "hw_dashboard",
                onItemClick = { route ->
                    when (route) {
                        "patient_list" -> onPatientList()
                        "hw_reports" -> onReports()
                        "hw_alerts" -> onAlerts()
                        "hw_profile" -> onProfile()
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddPatient,
                icon = { Icon(Icons.Filled.PersonAdd, null) },
                text = { Text("Add Patient") }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Hero section
            item { HWHeroSection(workerName = uiState.workerName, isOnline = uiState.isOnline) }

            // Stats row
            item {
                StatsRow(
                    totalPatients = uiState.totalPatients,
                    pendingReferrals = uiState.pendingReferrals,
                    screeningsToday = uiState.screeningsToday,
                    criticalAlerts = uiState.criticalAlerts
                )
            }

            // Quick actions
            item {
                HWQuickActionsSection(
                    onAddPatient = onAddPatient,
                    onPatientList = onPatientList,
                    onReports = onReports,
                    onSyncData = onSyncData,
                    onSettings = onSettings
                )
            }

            // Pending referrals
            if (uiState.pendingReferralList.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Pending Referrals",
                        count = uiState.pendingReferrals,
                        onSeeAll = onReports
                    )
                }
                items(uiState.pendingReferralList) { referral ->
                    PendingReferralCard(
                        referral = referral,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            // Recent patients
            item {
                SectionHeader(
                    title = "Recent Patients",
                    count = uiState.totalPatients,
                    onSeeAll = onPatientList
                )
            }

            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                }
            } else if (uiState.recentPatients.isEmpty()) {
                item { EmptyPatientsCard(onAddPatient = onAddPatient) }
            } else {
                items(uiState.recentPatients) { patient ->
                    RecentPatientCard(
                        patient = patient,
                        onClick = { onPatientDetail(patient.id) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

// ── Hero ──

@Composable
private fun HWHeroSection(workerName: String, isOnline: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(SwasthAIColors.HWPrimary, SwasthAIColors.HWPrimary.copy(alpha = 0.8f))
                )
            )
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Welcome back,",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = workerName.ifBlank { "Health Worker" },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                ConnectionStatusBadge(isOnline = isOnline)
            }
            Text(
                text = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date()),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.75f)
            )
        }
    }
}

// ── Stats Row ──

@Composable
private fun StatsRow(
    totalPatients: Int,
    pendingReferrals: Int,
    screeningsToday: Int,
    criticalAlerts: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(label = "Patients", value = totalPatients.toString(), icon = Icons.Filled.People, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
        StatCard(label = "Referrals", value = pendingReferrals.toString(), icon = Icons.Filled.TransferWithinAStation, color = SwasthAIColors.RiskModerate, modifier = Modifier.weight(1f))
        StatCard(label = "Today", value = screeningsToday.toString(), icon = Icons.Filled.Today, color = SwasthAIColors.RiskLow, modifier = Modifier.weight(1f))
        StatCard(label = "Critical", value = criticalAlerts.toString(), icon = Icons.Filled.Warning, color = SwasthAIColors.RiskHigh, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    ElevatedCard(modifier = modifier, shape = RoundedCornerShape(14.dp)) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── Quick Actions ──

@Composable
private fun HWQuickActionsSection(
    onAddPatient: () -> Unit,
    onPatientList: () -> Unit,
    onReports: () -> Unit,
    onSyncData: () -> Unit,
    onSettings: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Quick Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
        val rows = listOf(
            listOf(
                Triple(Icons.Filled.PersonAdd, "Add Patient", onAddPatient),
                Triple(Icons.Filled.People, "Patient List", onPatientList),
                Triple(Icons.Filled.Analytics, "Reports", onReports)
            ),
            listOf(
                Triple(Icons.Filled.Sync, "Sync Data", onSyncData),
                Triple(Icons.Filled.Settings, "Settings", onSettings),
                Triple(Icons.Filled.Help, "Help", {
                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:104")))
                })
            )
        )
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (icon, label, action) ->
                    QuickActionItem(icon = icon, label = label, onClick = action, modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ── Section Header ──

@Composable
private fun SectionHeader(title: String, count: Int, onSeeAll: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                Text(
                    count.toString(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        TextButton(onClick = onSeeAll) { Text("See All") }
    }
}

// ── Pending Referral Card ──

@Composable
private fun PendingReferralCard(referral: Referral, modifier: Modifier = Modifier) {
    val priorityColor = if (referral.priority == "HIGH") SwasthAIColors.RiskHigh else SwasthAIColors.RiskModerate
    ElevatedCard(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = CircleShape, color = priorityColor.copy(alpha = 0.15f), modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.TransferWithinAStation, null, tint = priorityColor, modifier = Modifier.size(20.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(referral.patientName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("Referral to ${referral.facilityName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(shape = RoundedCornerShape(8.dp), color = priorityColor.copy(alpha = 0.15f)) {
                Text(
                    referral.priority,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = priorityColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ── Recent Patient Card ──

@Composable
private fun RecentPatientCard(patient: Patient, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ElevatedCard(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = patient.name.firstOrNull()?.toString()?.uppercase() ?: "P",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(patient.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "${patient.age ?: "?"} yrs • ${patient.gender ?: "—"} • ${patient.village ?: "—"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
        }
    }
}

// ── Empty state ──

@Composable
private fun EmptyPatientsCard(onAddPatient: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Filled.Group, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            Text("No patients yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Add your first patient to get started", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onAddPatient, shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Filled.PersonAdd, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add First Patient")
            }
        }
    }
}
