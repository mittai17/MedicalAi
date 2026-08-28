package com.swasthai.app.feature.healthworker.screening

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.swasthai.app.core.components.RiskBadge
import com.swasthai.app.core.components.SwasthAITopBar
import com.swasthai.app.core.theme.SwasthAIColors
import com.swasthai.app.domain.model.RiskLevel
import com.swasthai.app.feature.citizen.screening.ScreeningViewModel

/**
 * Follow-up Decision Screen (Screen 20).
 *
 * After HW screening result, the worker chooses:
 *   A) Manage locally — schedule follow-up
 *   B) Refer to PHC / hospital
 *   C) Emergency — call 108
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowUpDecisionScreen(
    patientName: String,
    onBack: () -> Unit,
    onScheduleFollowUp: () -> Unit,
    onReferToPHC: () -> Unit,
    viewModel: ScreeningViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val result = uiState.diagnosisResult
    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        topBar = {
            SwasthAITopBar(
                title = "Follow-up Decision",
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
            // Summary card
            ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Screening Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(patientName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            result?.predictedDisease?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        result?.let { RiskBadge(riskLevel = it.riskLevel) }
                            ?: Text(
                                "No result yet",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                    }
                }
            }

            Text(
                "What action would you like to take?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Decision options
            val isHighRisk = result?.riskLevel == RiskLevel.HIGH

            if (isHighRisk) {
                DecisionOptionCard(
                    icon = Icons.Filled.Emergency,
                    title = "Emergency — Call 108",
                    subtitle = "Patient needs immediate emergency care",
                    color = SwasthAIColors.RiskHigh,
                    isUrgent = true,
                    onClick = { dialEmergency(context) }
                )
            }

            DecisionOptionCard(
                icon = Icons.Filled.LocalHospital,
                title = "Refer to PHC / Hospital",
                subtitle = "Patient needs professional medical attention",
                color = SwasthAIColors.RiskModerate,
                onClick = onReferToPHC
            )

            DecisionOptionCard(
                icon = Icons.Filled.Schedule,
                title = "Schedule Follow-up",
                subtitle = "Manage locally with a planned follow-up visit",
                color = MaterialTheme.colorScheme.primary,
                onClick = onScheduleFollowUp
            )

            DecisionOptionCard(
                icon = Icons.Filled.CheckCircle,
                title = "No Further Action",
                subtitle = "Patient is stable, no immediate follow-up needed",
                color = SwasthAIColors.RiskLow,
                onClick = onBack
            )
        }
    }
}

@Composable
private fun DecisionOptionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    isUrgent: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = if (isUrgent) BorderStroke(2.dp, color) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = color.copy(alpha = 0.15f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(26.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = if (isUrgent) color else MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

private fun dialEmergency(context: android.content.Context) {
    context.startActivity(
        Intent(Intent.ACTION_DIAL, Uri.parse("tel:108")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}
