package com.swasthai.app.feature.citizen.records

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.swasthai.app.ai.engine.DiseaseKnowledgeBase
import com.swasthai.app.core.components.RiskBadge
import com.swasthai.app.core.components.SwasthAIPrimaryButton
import com.swasthai.app.core.components.SwasthAITopBar
import com.swasthai.app.core.theme.SwasthAIColors
import com.swasthai.app.domain.model.MedicalAdvice
import com.swasthai.app.domain.model.ScreeningDetail
import com.swasthai.app.domain.model.ScreeningType
import com.swasthai.app.domain.model.SymptomSource
import com.swasthai.app.domain.model.Vitals
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Detailed view of one completed screening.
 *
 * Composes everything stored for the session: the reported symptoms,
 * recorded vitals, the AI diagnosis (disease, risk, confidence),
 * the doctor explanation, recommendations, and any captured images.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordDetailScreen(
    screeningId: String,
    onBack: () -> Unit,
    onShareReport: (String) -> Unit,
    viewModel: RecordDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(screeningId) {
        viewModel.load(screeningId)
    }

    Scaffold(
        topBar = {
            SwasthAITopBar(
                title = "Screening Detail",
                showBackButton = true,
                onBackClick = onBack,
                actions = {
                    if (uiState.detail != null) {
                        IconButton(onClick = { onShareReport(screeningId) }) {
                            Icon(
                                Icons.Filled.Share,
                                contentDescription = "Share report",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> Box(
                Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            uiState.error != null -> ErrorState(
                message = uiState.error ?: "Something went wrong.",
                error = uiState.error,
                onRetry = { viewModel.load(screeningId) }
            )

            uiState.detail != null -> DetailContent(
                detail = uiState.detail!!,
                onShareReport = { onShareReport(screeningId) }
            )
        }
    }
}

@Composable
private fun ErrorState(message: String, error: String?, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Filled.ErrorOutline, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.error)
            Text(message, style = MaterialTheme.typography.titleMedium)
            if (!error.isNullOrBlank() && error != message) {
                Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = onRetry) {
                Icon(Icons.Filled.Refresh, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Retry")
            }
        }
    }
}

@Composable
private fun DetailContent(
    detail: ScreeningDetail,
    onShareReport: () -> Unit
) {
    val diagnosis = detail.diagnosis
    val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        .format(Date(detail.screening.createdAt))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Hero: type + date + risk.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
                    )
                )
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = screeningTypeLabel(detail.screening.screeningType),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (diagnosis != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.25f)
                        ) {
                            Text(
                                "${diagnosis.confidenceScore.toInt()}% confidence",
                                Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
                Text(dateStr, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
            }
        }

        Spacer(Modifier.height(16.dp))

        Column(
            Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Diagnosis summary.
            if (diagnosis != null) {
                DetailSectionCard(
                    title = "Likely Condition",
                    icon = Icons.Filled.Medication
                ) {
                    Text(
                        diagnosis.predictedDisease,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Risk level", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        RiskBadge(riskLevel = diagnosis.riskLevel)
                    }
                }
            }

            // Reported symptoms.
            if (detail.symptoms.isNotEmpty()) {
                DetailSectionCard(
                    title = "Reported Symptoms",
                    icon = Icons.Filled.Checklist
                ) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(detail.symptoms.size) { index ->
                            val symptom = detail.symptoms[index]
                            val sourceLabel = when (symptom.source) {
                                SymptomSource.VOICE -> "voice"
                                SymptomSource.TEXT -> "typed"
                                else -> null
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = symptom.name + durationSuffix(symptom.duration) +
                                        (if (sourceLabel != null) " · $sourceLabel" else ""),
                                    Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            // Vitals.
            detail.vitals?.let { vitals ->
                DetailSectionCard(
                    title = "Vitals",
                    icon = Icons.Filled.Monitor
                ) {
                    VitalsGrid(vitals = vitals)
                }
            }

            // AI doctor explanation.
            diagnosis?.let {
                val advice = it.medicalAdvice
                    ?: DiseaseKnowledgeBase.adviceFor(it.predictedDisease)
                DoctorAdviceSection(advice = advice)
            }

            // Recommendations.
            if (diagnosis != null && diagnosis.recommendations.isNotEmpty()) {
                DetailSectionCard(
                    title = "Recommendations",
                    icon = Icons.Filled.TipsAndUpdates
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        diagnosis.recommendations.sortedBy { it.priority }.forEach { rec ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    null,
                                    Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(rec.text, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            // Captured images.
            if (detail.images.isNotEmpty()) {
                DetailSectionCard(
                    title = "Captured Images",
                    icon = Icons.Filled.CameraAlt
                ) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(detail.images.size) { index ->
                            val image = detail.images[index]
                            AsyncImage(
                                model = image.imagePath,
                                contentDescription = image.analysisResult ?: "Captured image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(96.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            SwasthAIPrimaryButton(
                text = "Share Report",
                onClick = onShareReport,
                leadingIcon = Icons.Filled.Share
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DetailSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun VitalsGrid(vitals: Vitals) {
    val items = listOf(
        "Temperature" to vitals.temperature?.let { "%.1f °C".format(it) },
        "Pulse" to vitals.pulse?.let { "$it bpm" },
        "SpO₂" to vitals.spo2?.let { "%.0f %%".format(it) },
        "Blood Pressure" to vitals.bloodPressure,
        "Weight" to vitals.weight?.let { "%.1f kg".format(it) },
        "Height" to vitals.height?.let { "%.1f cm".format(it) }
    ).filter { it.second != null }

    if (items.isEmpty()) return

    val rows = items.chunked(2)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEachIndexed { index, (label, value) ->
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(value ?: "—", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DoctorAdviceSection(advice: MedicalAdvice) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        )
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Psychology, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.tertiary)
                Text("AI Doctor Explains", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            AdviceRow(Icons.Filled.Info, "Why it happens", advice.cause, MaterialTheme.colorScheme.primary)
            AdviceRow(Icons.Filled.HealthAndSafety, "What to do", advice.remedy, MaterialTheme.colorScheme.secondary)
            AdviceRow(Icons.Filled.MedicalServices, "Which doctor to consult", advice.doctorToConsult, MaterialTheme.colorScheme.tertiary)
            if (advice.urgencyHint.isNotBlank()) {
                AdviceRow(Icons.Filled.Warning, "Watch for", advice.urgencyHint, SwasthAIColors.RiskHigh)
            }
        }
    }
}

@Composable
private fun AdviceRow(icon: ImageVector, title: String, body: String, tint: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Surface(shape = RoundedCornerShape(50), color = tint.copy(alpha = 0.12f), modifier = Modifier.size(32.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = tint, fontWeight = FontWeight.Bold)
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun screeningTypeLabel(type: ScreeningType) = when (type) {
    ScreeningType.SYMPTOM_CHECK -> "Symptom Check"
    ScreeningType.VOICE_ASSISTANT -> "Voice Screening"
    ScreeningType.IMAGE_CHECK -> "Image Screening"
    ScreeningType.COMBINED -> "Combined Screening"
}

private fun durationSuffix(duration: String?): String =
    if (duration.isNullOrBlank()) "" else " · $duration"