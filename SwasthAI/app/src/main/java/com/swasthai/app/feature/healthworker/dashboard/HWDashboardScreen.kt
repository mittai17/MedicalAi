package com.swasthai.app.feature.healthworker.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.swasthai.app.core.components.*
import com.swasthai.app.core.theme.SwasthAIColors
import java.text.SimpleDateFormat
import java.util.*

/**
 * Health Worker Dashboard Screen — redesigned for real ASHA workflows.
 *
 * Layout (top → bottom):
 *  1. Hero: greeting + smart sync status
 *  2. Search bar (patient / Reference ID)
 *  3. Today's Work: visits | health checks | follow-ups
 *  4. Stats row: My Patients | Pending Referrals | Today's Visits | Needs Attention
 *  5. Quick Actions: Health Check / Follow-ups / Referrals / Reports / Sync / Nearby
 *  6. Needs Attention
 *  7. Recent Activity
 *  FAB: Add Patient
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
    onNearbyPatients: () -> Unit,
    viewModel: HWDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SwasthAITopBar(
                title = "SwasthAI",
                isOnline = uiState.isOnline,
                actions = {
                    if (uiState.needsAttentionCount > 0) {
                        BadgedBox(badge = {
                            Badge { Text(uiState.needsAttentionCount.toString()) }
                        }) {
                            IconButton(onClick = onAlerts) {
                                Icon(Icons.Filled.Notifications, "Alerts")
                            }
                        }
                    }
                    IconButton(onClick = onProfile) {
                        Icon(
                            Icons.Filled.AccountCircle,
                            "Profile",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                        "hw_reports"   -> onReports()
                        "hw_alerts"    -> onAlerts()
                        "hw_profile"   -> onProfile()
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddPatient,
                icon = { Icon(Icons.Filled.PersonAdd, null) },
                text = { Text("Add Patient") },
                containerColor = SwasthAIColors.HWPrimary,
                contentColor = Color.White
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // ── 1. Hero ─────────────────────────────────────────────────────
            item {
                HWHeroSection(
                    workerName = uiState.workerName,
                    syncStatus = uiState.syncStatus
                )
            }



            // ── 3. Today's Work row ─────────────────────────────────────────
            item {
                TodayWorkRow(
                    healthChecks = uiState.todayHealthChecks,
                    followUps = uiState.todayFollowUps,
                    referrals = uiState.pendingReferrals,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }

            // ── 4. Stats row ────────────────────────────────────────────────
            item { Spacer(modifier = Modifier.height(8.dp)) }
            item {
                StatsRow(
                    totalPatients = uiState.totalPatients,
                    pendingReferrals = uiState.pendingReferrals,
                    needsAttention = uiState.needsAttentionCount
                )
            }

            // ── 5. Quick Actions ────────────────────────────────────────────
            item {
                HWQuickActionsSection(
                    onReports = onReports,
                    onSyncData = onSyncData,
                    onNearbyPatients = onNearbyPatients
                )
            }

            // ── 6. Needs Attention ──────────────────────────────────────────
            item {
                SectionHeader(
                    title = "Needs Attention",
                    badge = uiState.needsAttentionList.size.takeIf { it > 0 },
                    onSeeAll = onAlerts
                )
            }
            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(color = SwasthAIColors.HWPrimary) }
                }
            } else if (uiState.needsAttentionList.isEmpty()) {
                item { AttentionEmptyCard() }
            } else {
                items(uiState.needsAttentionList) { item ->
                    NeedsAttentionCard(
                        item = item,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp)
                    )
                }
            }

            // ── 7. Recent Activity ──────────────────────────────────────────
            item {
                SectionHeader(
                    title = "Recent Activity",
                    badge = null,
                    onSeeAll = onPatientList
                )
            }
            if (uiState.recentActivityList.isEmpty()) {
                item { RecentActivityEmptyCard() }
            } else {
                items(uiState.recentActivityList) { activity ->
                    RecentActivityCard(
                        item = activity,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp)
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(140.dp)) }
        }
    }
}

// ── Hero ──────────────────────────────────────────────────────────────────────

@Composable
private fun HWHeroSection(workerName: String, syncStatus: SyncStatus) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        SwasthAIColors.HWPrimary,
                        SwasthAIColors.HWPrimary.copy(alpha = 0.85f)
                    )
                )
            )
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
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
                SmartSyncBadge(syncStatus = syncStatus)
            }
            Text(
                text = SimpleDateFormat("EEEE, dd MMMM", Locale.getDefault()).format(Date()),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.75f)
            )
        }
    }
}

@Composable
private fun SmartSyncBadge(syncStatus: SyncStatus) {
    val bgColor = if (syncStatus.isOnline) Color(0xFF1B8C4A) else Color(0xFFE65100)
    val icon = if (syncStatus.isOnline) Icons.Filled.Wifi else Icons.Filled.WifiOff
    val label = when {
        syncStatus.isOnline && syncStatus.lastSyncMinutesAgo != null ->
            "Synced ${syncStatus.lastSyncMinutesAgo} min ago"
        syncStatus.isOnline -> "Online"
        syncStatus.pendingCount > 0 -> "${syncStatus.pendingCount} pending"
        else -> "Offline"
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = bgColor.copy(alpha = 0.35f),
        modifier = Modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(13.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontSize = 11.sp
            )
        }
    }
}

// ── Search Bar ────────────────────────────────────────────────────────────────

@Composable
private fun HWSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = {
            Text(
                "Search patient / Reference ID",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingIcon = {
            Icon(
                Icons.Filled.Search, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Close, "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        shape = RoundedCornerShape(14.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedBorderColor = SwasthAIColors.HWPrimary
        )
    )
}

// ── Today's Work ──────────────────────────────────────────────────────────────

@Composable
private fun TodayWorkRow(
    healthChecks: Int,
    followUps: Int,
    referrals: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SwasthAIColors.HWPrimary.copy(alpha = 0.07f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = "TODAY'S WORK",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = SwasthAIColors.HWPrimary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TodayWorkChip(value = healthChecks, label = "Checks", icon = Icons.Filled.HealthAndSafety)
                VerticalDivider(modifier = Modifier.height(36.dp), color = MaterialTheme.colorScheme.outlineVariant)
                TodayWorkChip(value = followUps, label = "Follow-ups", icon = Icons.Filled.EventRepeat)
                VerticalDivider(modifier = Modifier.height(36.dp), color = MaterialTheme.colorScheme.outlineVariant)
                TodayWorkChip(value = referrals, label = "Referrals", icon = Icons.Filled.TransferWithinAStation)
            }
        }
    }
}

@Composable
private fun TodayWorkChip(value: Int, label: String, icon: ImageVector) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, null, tint = SwasthAIColors.HWPrimary, modifier = Modifier.size(18.dp))
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = SwasthAIColors.HWPrimary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── Stats Row ─────────────────────────────────────────────────────────────────

@Composable
private fun StatsRow(
    totalPatients: Int,
    pendingReferrals: Int,
    needsAttention: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(
            label = "My Patients",
            value = totalPatients.toString(),
            icon = Icons.Filled.People,
            color = SwasthAIColors.HWPrimary,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Pending Referrals",
            value = pendingReferrals.toString(),
            icon = Icons.Filled.TransferWithinAStation,
            color = SwasthAIColors.RiskModerate,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Attention",
            value = needsAttention.toString(),
            icon = Icons.Filled.Warning,
            color = if (needsAttention > 0) SwasthAIColors.RiskHigh else SwasthAIColors.RiskLow,
            modifier = Modifier.weight(1f)
        )
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
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = color)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

// ── Quick Actions ─────────────────────────────────────────────────────────────

private data class QA(val emoji: String, val label: String, val sublabel: String, val onClick: () -> Unit)

@Composable
private fun HWQuickActionsSection(
    onReports: () -> Unit,
    onSyncData: () -> Unit,
    onNearbyPatients: () -> Unit
) {
    val actions = listOf(
        QA("📊", "Reports", "Daily summary", onReports),
        QA("🔄", "Sync", "Upload records", onSyncData),
        QA("📍", "Nearby", "Patients nearby", onNearbyPatients)
    )

    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text(
            "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        val rows = actions.chunked(3)
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { qa ->
                    QACard(
                        emoji = qa.emoji,
                        label = qa.label,
                        sublabel = qa.sublabel,
                        onClick = qa.onClick,
                        modifier = Modifier.weight(1f)
                    )
                }
                // Pad if fewer than 3 in last row
                repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun QACard(
    emoji: String,
    label: String,
    sublabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(text = emoji, fontSize = 26.sp)
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = sublabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

// ── Section Header ────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(
    title: String,
    badge: Int?,
    onSeeAll: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (badge != null) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(
                        badge.toString(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        TextButton(onClick = onSeeAll) { Text("See All") }
    }
}

// ── Needs Attention Card ──────────────────────────────────────────────────────

@Composable
private fun NeedsAttentionCard(item: NeedsAttentionItem, modifier: Modifier = Modifier) {
    val (dotColor, bgColor) = when (item.level) {
        AttentionLevel.CRITICAL -> SwasthAIColors.RiskHigh to SwasthAIColors.RiskHigh.copy(alpha = 0.08f)
        AttentionLevel.HIGH -> SwasthAIColors.RiskModerate to SwasthAIColors.RiskModerate.copy(alpha = 0.08f)
        AttentionLevel.MEDIUM -> Color(0xFFF9A825) to Color(0xFFFFF8E1)
    }
    val levelLabel = when (item.level) {
        AttentionLevel.CRITICAL -> "Critical"
        AttentionLevel.HIGH -> "Needs Check"
        AttentionLevel.MEDIUM -> "Medium"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.patientName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    item.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = dotColor.copy(alpha = 0.18f)
            ) {
                Text(
                    levelLabel,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = dotColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AttentionEmptyCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = SwasthAIColors.RiskLow.copy(alpha = 0.08f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.CheckCircle,
                null,
                tint = SwasthAIColors.RiskLow,
                modifier = Modifier.size(22.dp)
            )
            Text(
                "All patients are on track 🎉",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = SwasthAIColors.RiskLow
            )
        }
    }
}

// ── Recent Activity Card ──────────────────────────────────────────────────────

@Composable
private fun RecentActivityCard(item: RecentActivityItem, modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.patientName.firstOrNull()?.toString()?.uppercase() ?: "P",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.patientName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    item.action,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                item.timeAgo,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun RecentActivityEmptyCard() {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.History,
                null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
            Text(
                "No recent activity",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
