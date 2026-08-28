package com.swasthai.app.feature.healthworker.reports

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.swasthai.app.core.components.RiskBadge
import com.swasthai.app.core.components.SwasthAITopBar
import com.swasthai.app.core.theme.SwasthAIColors
import com.swasthai.app.domain.model.RiskLevel

/**
 * HW Reports & Analytics Screen (Screen 23).
 *
 * Shows:
 *  - Summary stats (screenings, referrals, high-risk cases)
 *  - Simple bar chart (monthly screenings — drawn with Box)
 *  - List of pending referrals
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HWReportsScreen(
    onBack: () -> Unit,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            SwasthAITopBar(
                title = "Reports & Analytics",
                showBackButton = true,
                onBackClick = onBack,
                actions = {
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "SwasthAI Reports & Analytics")
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "SwasthAI Reports & Analytics\n\n" +
                                    "Screenings this month: ${uiState.screeningsThisMonth}\n" +
                                    "Pending referrals: ${uiState.referralsThisMonth}\n" +
                                    "High-risk cases this month: ${uiState.highRiskThisMonth}\n\n" +
                                    "Export prepared on device — data stays private."
                            )
                        }
                        context.startActivity(Intent.createChooser(intent, "Export Report"))
                    }) {
                        Icon(Icons.Filled.FileDownload, "Export")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0; viewModel.load() }, text = { Text("Summary") }, icon = { Icon(Icons.Filled.Dashboard, null) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Referrals") }, icon = { Icon(Icons.Filled.TransferWithinAStation, null) })
            }

            when (selectedTab) {
                0 -> SummaryTab(uiState = uiState, onRetry = { viewModel.load() })
                1 -> ReferralsTab(uiState = uiState, onRetry = { viewModel.load() })
            }
        }
    }
}

@Composable
private fun SummaryTab(uiState: ReportsUiState, onRetry: () -> Unit) {
    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (uiState.screeningsThisMonth == 0 && uiState.referralsThisMonth == 0) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Dashboard, null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.outlineVariant)
                Text("No activity this month yet", style = MaterialTheme.typography.titleMedium)
            }
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("This Month", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryStatCard("Screenings", uiState.screeningsThisMonth.toString(), Icons.Filled.MedicalServices, SwasthAIColors.HWPrimary, Modifier.weight(1f))
                SummaryStatCard("Referrals", uiState.referralsThisMonth.toString(), Icons.Filled.TransferWithinAStation, SwasthAIColors.RiskModerate, Modifier.weight(1f))
                SummaryStatCard("High Risk", uiState.highRiskThisMonth.toString(), Icons.Filled.Warning, SwasthAIColors.RiskHigh, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SummaryStatCard(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier) {
    ElevatedCard(modifier = modifier, shape = RoundedCornerShape(14.dp)) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ReferralsTab(uiState: ReportsUiState, onRetry: () -> Unit) {
    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (uiState.pendingReferralsList.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.TransferWithinAStation, null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.outlineVariant)
                Text("No pending referrals", style = MaterialTheme.typography.titleMedium)
            }
        }
        return
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Pending Referrals", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        items(uiState.pendingReferralsList) { referral ->
            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(referral.patientName.ifBlank { "Patient" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(referral.facilityName.ifBlank { "Referral" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    RiskBadge(riskLevel = priorityToRisk(referral.priority))
                }
            }
        }
    }
}

private fun priorityToRisk(priority: String): RiskLevel = when (priority.uppercase()) {
    "HIGH" -> RiskLevel.HIGH
    "MODERATE" -> RiskLevel.MODERATE
    else -> RiskLevel.LOW
}
