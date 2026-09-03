package com.swasthai.app.feature.citizen.records

import android.content.Context
import android.content.Intent
import android.content.ActivityNotFoundException
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.swasthai.app.core.components.RiskBadge
import com.swasthai.app.core.components.SwasthAIPrimaryButton
import com.swasthai.app.core.components.SwasthAITopBar
import com.swasthai.app.domain.model.ScreeningType
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Share / Download Report — builds a real report from the stored screening
 * (symptoms, vitals, diagnosis, recommendations) and lets the user export it
 * as PDF / Image / Text and share it through other apps.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareReportScreen(
    screeningId: String,
    onBack: () -> Unit,
    viewModel: ShareReportViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    var selectedFormatName by rememberSaveable { mutableStateOf(ReportFormat.PDF.name) }
    var generatedFile by remember { mutableStateOf<File?>(null) }
    var reportPersisted by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(screeningId) {
        viewModel.load(screeningId)
    }

    val selectedFormat = remember(selectedFormatName) { ReportFormat.valueOf(selectedFormatName) }

    Scaffold(
        topBar = {
            SwasthAITopBar(
                title = "Share Report",
                showBackButton = true,
                onBackClick = onBack
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> Box(
                Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            uiState.error != null -> Box(
                Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Filled.ErrorOutline, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.error)
                    Text(uiState.error ?: "Something went wrong.", style = MaterialTheme.typography.titleMedium)
                    Button(onClick = { viewModel.load(screeningId) }) {
                        Icon(Icons.Filled.Refresh, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Retry")
                    }
                }
            }

            uiState.detail != null -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // What the report contains (real data).
                ReportSummaryCard(detail = uiState.detail!!)

                // Format selector.
                ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Select Format", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ReportFormat.entries.forEach { format ->
                                FormatCard(
                                    icon = when (format) {
                                        ReportFormat.PDF -> Icons.Filled.PictureAsPdf
                                        ReportFormat.IMAGE -> Icons.Filled.Image
                                        ReportFormat.TEXT -> Icons.Filled.TextSnippet
                                    },
                                    label = format.label,
                                    isSelected = selectedFormat == format,
                                    onClick = {
                                        selectedFormatName = format.name
                                        statusMessage = null
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Create the real report file.
                SwasthAIPrimaryButton(
                    text = if (generatedFile != null) "Regenerate Report" else "Create Report",
                    onClick = {
                        scope.launch {
                            val detail = uiState.detail ?: return@launch
                            val result = ReportGenerator.generate(
                                context = context,
                                detail = detail,
                                patientName = uiState.patientName,
                                format = selectedFormat
                            )
                            result.onSuccess { file ->
                                generatedFile = file
                                statusMessage = when (selectedFormat) {
                                    ReportFormat.PDF -> "PDF report ready."
                                    ReportFormat.IMAGE -> "Image report ready."
                                    ReportFormat.TEXT -> "Text report ready."
                                }
                                if (!reportPersisted) {
                                    viewModel.persistReport(
                                        detail = detail,
                                        patientName = uiState.patientName,
                                        pdfPath = file.absolutePath
                                    )
                                    reportPersisted = true
                                }
                            }.onFailure {
                                statusMessage = "Could not create report: ${it.message}"
                            }
                        }
                    },
                    leadingIcon = Icons.Filled.Description
                )

                // Share (enabled only after a real file exists).
                ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Share Report", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        ShareOptionRow(
                            icon = Icons.Filled.Chat,
                            label = "WhatsApp",
                            enabled = generatedFile != null,
                            onClick = {
                                generatedFile?.let {
                                    shareFile(context, it, selectedFormat, "com.whatsapp")
                                }
                            }
                        )
                        ShareOptionRow(
                            icon = Icons.Filled.Share,
                            label = "Other Apps",
                            enabled = generatedFile != null,
                            onClick = {
                                generatedFile?.let {
                                    shareFile(context, it, selectedFormat, null)
                                }
                            }
                        )
                        ShareOptionRow(
                            icon = Icons.Filled.Download,
                            label = "Open / Download",
                            enabled = generatedFile != null,
                            onClick = {
                                generatedFile?.let { openFile(context, it, selectedFormat) }
                            }
                        )
                    }
                }

                if (!statusMessage.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (statusMessage!!.startsWith("Could not")) Icons.Filled.ErrorOutline else Icons.Filled.CheckCircle,
                                null,
                                tint = if (statusMessage!!.startsWith("Could not")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                            Text(statusMessage.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportSummaryCard(detail: com.swasthai.app.domain.model.ScreeningDetail) {
    val diagnosis = detail.diagnosis
    val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        .format(Date(detail.screening.createdAt))
    ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Report Contents", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            SummaryLine("Screening", when (detail.screening.screeningType) {
                ScreeningType.SYMPTOM_CHECK -> "Symptom Check"
                ScreeningType.VOICE_ASSISTANT -> "Voice Screening"
                ScreeningType.IMAGE_CHECK -> "Image Screening"
                ScreeningType.COMBINED -> "Combined Screening"
            })
            SummaryLine("Date", dateStr)
            if (diagnosis != null) {
                SummaryLine("Likely condition", diagnosis.predictedDisease)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Risk level", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    RiskBadge(riskLevel = diagnosis.riskLevel)
                }
            }
            Text(
                "Includes symptoms, vitals, AI explanation and recommendations.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun FormatCard(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ShareOptionRow(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = label, tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                if (!enabled) "Create report first" else "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (enabled) {
                Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
            }
        }
    }
}

/**
 * Shares the generated file through the system chooser (or a specific app).
 */
private fun shareFile(
    context: Context,
    file: File,
    format: ReportFormat,
    targetPackage: String?
) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = format.mimeType
        putExtra(Intent.EXTRA_SUBJECT, "SwasthAI Health Report")
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        targetPackage?.let { setPackage(it) }
    }
    try {
        if (targetPackage != null) {
            context.startActivity(intent)
        } else {
            context.startActivity(Intent.createChooser(intent, "Share report via"))
        }
    } catch (e: ActivityNotFoundException) {
        if (targetPackage != null) {
            // Primary target (e.g. WhatsApp) isn't installed — resend via chooser.
            intent.setPackage(null)
            context.startActivity(Intent.createChooser(intent, "Share report via"))
        } else {
            context.startActivity(Intent.createChooser(intent, "Share report via"))
        }
    }
}

/**
 * Opens the generated file so the user can view / save it.
 */
private fun openFile(context: Context, file: File, format: ReportFormat) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, format.mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }.onFailure {
        // Nothing can open this type directly — offer sharing instead.
        context.startActivity(Intent.createChooser(intent, "Share report"))
    }
}