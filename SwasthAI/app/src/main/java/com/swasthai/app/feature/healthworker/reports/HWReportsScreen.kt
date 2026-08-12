package com.swasthai.app.feature.healthworker.reports

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
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            SwasthAITopBar(
                title = "Reports & Analytics",
                showBackButton = true,
                onBackClick = onBack,
                actions = {
                    IconButton(onClick = { /* Export */ }) {
                        Icon(Icons.Filled.FileDownload, "Export")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Summary") }, icon = { Icon(Icons.Filled.Dashboard, null) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Referrals") }, icon = { Icon(Icons.Filled.TransferWithinAStation, null) })
            }

            when (selectedTab) {
                0 -> SummaryTab()
                1 -> ReferralsTab()
            }
        }
    }
}

@Composable
private fun SummaryTab() {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Month stats
        item {
            Text("This Month", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryStatCard("Screenings", "24", Icons.Filled.MedicalServices, SwasthAIColors.HWPrimary, Modifier.weight(1f))
                SummaryStatCard("Referrals", "6", Icons.Filled.TransferWithinAStation, SwasthAIColors.RiskModerate, Modifier.weight(1f))
                SummaryStatCard("High Risk", "3", Icons.Filled.Warning, SwasthAIColors.RiskHigh, Modifier.weight(1f))
            }
        }

        // Monthly chart
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Screenings per Month", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    MonthlyBarChart()
                }
            }
        }

        // Disease breakdown
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Top Conditions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    val conditions = listOf(
                        Triple("Viral Fever", 8, SwasthAIColors.RiskModerate),
                        Triple("Respiratory Infection", 5, SwasthAIColors.RiskHigh),
                        Triple("Malaria", 4, SwasthAIColors.RiskHigh),
                        Triple("Anemia", 3, SwasthAIColors.RiskModerate),
                        Triple("Hypertension", 2, SwasthAIColors.RiskLow)
                    )
                    conditions.forEach { (name, count, color) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            LinearProgressIndicator(
                                progress = { count / 10f },
                                modifier = Modifier.width(80.dp).height(6.dp),
                                color = color,
                                trackColor = color.copy(alpha = 0.2f)
                            )
                            Text(" $count", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = color)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthlyBarChart() {
    val months = listOf(
        "Mar" to 10, "Apr" to 14, "May" to 9,
        "Jun" to 18, "Jul" to 22, "Aug" to 24
    )
    val maxVal = months.maxOf { it.second }.toFloat()

    Row(
        modifier = Modifier.fillMaxWidth().height(120.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        months.forEach { (month, count) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(count.toString(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height((count / maxVal * 80).dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
                            ),
                            shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                        )
                )
                Text(month, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
private fun ReferralsTab() {
    val mockReferrals = listOf(
        Triple("Ramesh Kumar", "PHC Block A", RiskLevel.HIGH),
        Triple("Sunita Devi", "CHC District Hospital", RiskLevel.MODERATE),
        Triple("Amit Singh", "PHC Block A", RiskLevel.MODERATE)
    )

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Pending Referrals", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        items(mockReferrals) { (name, facility, risk) ->
            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(facility, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    RiskBadge(riskLevel = risk)
                }
            }
        }
    }
}
