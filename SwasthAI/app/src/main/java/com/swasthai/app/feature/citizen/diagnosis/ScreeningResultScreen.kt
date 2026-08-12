package com.swasthai.app.feature.citizen.diagnosis

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.swasthai.app.core.components.RiskBadge
import com.swasthai.app.core.components.SwasthAIPrimaryButton
import com.swasthai.app.core.components.SwasthAITopBar
import com.swasthai.app.core.theme.SwasthAIColors
import com.swasthai.app.domain.model.DiagnosisResult
import com.swasthai.app.domain.model.MedicalAdvice
import com.swasthai.app.domain.model.RiskLevel
import com.swasthai.app.domain.model.Recommendation
import com.swasthai.app.feature.citizen.screening.ScreeningViewModel

/**
 * Screening Result Screen (Screen 09 from Flow1 wireframe).
 *
 * Displays:
 *  - Risk badge (Moderate / High / Low) with animated reveal
 *  - Predicted disease + confidence score
 *  - Differential diagnoses
 *  - Recommendations list
 *  - Action buttons: Find Nearby Centre, Call Health Worker, Save Report
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreeningResultScreen(
    onBack: () -> Unit,
    onFindNearbyCenter: () -> Unit,
    onCallHealthWorker: () -> Unit,
    onSaveReport: (String) -> Unit,
    onNewScreening: () -> Unit,
    viewModel: ScreeningViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val result = uiState.diagnosisResult

    Scaffold(
        topBar = {
            SwasthAITopBar(
                title = "Screening Result",
                showBackButton = true,
                onBackClick = onBack,
                actions = {
                    IconButton(onClick = { onSaveReport(uiState.screeningId) }) {
                        Icon(
                            Icons.Filled.Save,
                            contentDescription = "Save Report",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (result == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text("Processing your symptoms…", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                // ── Risk Level Hero ──
                RiskHeroSection(result = result)

                // ── Disease prediction ──
                DiagnosisSummaryCard(result = result)

                // ── AI Doctor explanation: cause, remedy, who to consult ──
                result.medicalAdvice?.let { advice ->
                    DoctorExplanationCard(advice = advice)
                }

                // ── Differential diagnoses ──
                if (result.differentialDiagnosis.isNotEmpty()) {
                    DifferentialDiagnosisCard(differentials = result.differentialDiagnosis)
                }

                // ── Recommendations ──
                if (result.recommendations.isNotEmpty()) {
                    RecommendationsSection(recommendations = result.recommendations)
                }

                // ── Action buttons ──
                ActionButtonsSection(
                    riskLevel = result.riskLevel,
                    onFindNearbyCenter = onFindNearbyCenter,
                    onCallHealthWorker = onCallHealthWorker,
                    onSaveReport = { onSaveReport(uiState.screeningId) },
                    onNewScreening = {
                        viewModel.resetScreening()
                        onNewScreening()
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// ── Risk Hero ──

@Composable
private fun RiskHeroSection(result: DiagnosisResult) {
    val (gradientStart, gradientEnd) = when (result.riskLevel) {
        RiskLevel.HIGH -> Pair(SwasthAIColors.RiskHigh, Color(0xFFFF6B6B))
        RiskLevel.MODERATE -> Pair(Color(0xFFF59E0B), Color(0xFFFBBF24))
        RiskLevel.LOW -> Pair(SwasthAIColors.RiskLow, Color(0xFF34D399))
    }

    val riskLabel = when (result.riskLevel) {
        RiskLevel.HIGH -> "High Risk"
        RiskLevel.MODERATE -> "Moderate Risk"
        RiskLevel.LOW -> "Low Risk"
    }

    val riskIcon = when (result.riskLevel) {
        RiskLevel.HIGH -> Icons.Filled.Warning
        RiskLevel.MODERATE -> Icons.Filled.Info
        RiskLevel.LOW -> Icons.Filled.CheckCircle
    }

    // Confidence animation
    var animatedConfidence by remember { mutableFloatStateOf(0f) }
    val animatedValue by animateFloatAsState(
        targetValue = animatedConfidence,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "confidence"
    )
    LaunchedEffect(result) {
        animatedConfidence = result.confidenceScore / 100f
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(gradientStart, gradientEnd))
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = riskIcon,
                contentDescription = riskLabel,
                modifier = Modifier.size(56.dp),
                tint = Color.White
            )
            Text(
                text = riskLabel,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            // Animated confidence meter
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { animatedValue },
                    modifier = Modifier.size(80.dp),
                    color = Color.White,
                    strokeWidth = 6.dp,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${result.confidenceScore.toInt()}%",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "confidence",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

// ── Disease Summary ──

@Composable
private fun DiagnosisSummaryCard(result: DiagnosisResult) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Likely Condition",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = result.predictedDisease,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Risk Level",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                RiskBadge(riskLevel = result.riskLevel)
            }
        }
    }
}

// ── AI Doctor Explanation ──

@Composable
private fun DoctorExplanationCard(advice: MedicalAdvice) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Psychology,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "AI Doctor Explains",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            AdviceBlock(
                icon = Icons.Filled.Info,
                title = "Why it happens",
                body = advice.cause,
                tint = MaterialTheme.colorScheme.primary
            )
            AdviceBlock(
                icon = Icons.Filled.HealthAndSafety,
                title = "What to do",
                body = advice.remedy,
                tint = MaterialTheme.colorScheme.secondary
            )
            AdviceBlock(
                icon = Icons.Filled.MedicalServices,
                title = "Which doctor to consult",
                body = advice.doctorToConsult,
                tint = MaterialTheme.colorScheme.tertiary
            )
            if (advice.urgencyHint.isNotBlank()) {
                AdviceBlock(
                    icon = Icons.Filled.Warning,
                    title = "Watch for",
                    body = advice.urgencyHint,
                    tint = SwasthAIColors.RiskHigh
                )
            }
        }
    }
}

@Composable
private fun AdviceBlock(
    icon: ImageVector,
    title: String,
    body: String,
    tint: Color
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = tint.copy(alpha = 0.12f),
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = tint,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ── Differential Diagnosis ──

@Composable
private fun DifferentialDiagnosisCard(differentials: List<String>) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.MedicalServices, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                Text(
                    text = "Other Possible Conditions",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            differentials.forEachIndexed { index, disease ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(text = disease, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

// ── Recommendations ──

@Composable
private fun RecommendationsSection(recommendations: List<Recommendation>) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Recommendations",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        recommendations.sortedBy { it.priority }.forEach { rec ->
            RecommendationItem(recommendation = rec)
        }
    }
}

@Composable
private fun RecommendationItem(recommendation: Recommendation) {
    val (iconVector, iconColor) = when (recommendation.category) {
        "urgent" -> Pair(Icons.Filled.Emergency, SwasthAIColors.RiskHigh)
        "emergency" -> Pair(Icons.Filled.LocalHospital, SwasthAIColors.RiskHigh)
        "action" -> Pair(Icons.Filled.DirectionsWalk, SwasthAIColors.RiskModerate)
        "medication" -> Pair(Icons.Filled.Medication, MaterialTheme.colorScheme.primary)
        "followup" -> Pair(Icons.Filled.Schedule, MaterialTheme.colorScheme.secondary)
        else -> Pair(Icons.Filled.Info, MaterialTheme.colorScheme.tertiary)
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(22.dp).padding(top = 2.dp)
            )
            Text(
                text = recommendation.text,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

// ── Action Buttons ──

@Composable
private fun ActionButtonsSection(
    riskLevel: RiskLevel,
    onFindNearbyCenter: () -> Unit,
    onCallHealthWorker: () -> Unit,
    onSaveReport: () -> Unit,
    onNewScreening: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // High-risk urgent button
        if (riskLevel == RiskLevel.HIGH) {
            Button(
                onClick = onFindNearbyCenter,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SwasthAIColors.RiskHigh
                )
            ) {
                Icon(Icons.Filled.LocalHospital, null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Find Nearest Health Centre", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        }

        SwasthAIPrimaryButton(
            text = "Call Health Worker",
            onClick = onCallHealthWorker,
            leadingIcon = Icons.Filled.Call
        )

        OutlinedButton(
            onClick = onSaveReport,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Filled.Save, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save Report")
        }

        TextButton(
            onClick = onNewScreening,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Start New Screening")
        }
    }
}
