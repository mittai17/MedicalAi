package com.swasthai.app.feature.citizen.records

import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.swasthai.app.core.components.SwasthAIPrimaryButton
import com.swasthai.app.core.components.SwasthAITopBar

/**
 * Share / Download Report Screen (Screen 12 from Flow1 wireframe).
 *
 * Format selector (PDF, Image, Text)
 * Share via (WhatsApp, Bluetooth, Nearby Share, More)
 * Download button
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareReportScreen(
    screeningId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedFormat by remember { mutableStateOf("PDF") }
    var shareSuccess by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            SwasthAITopBar(
                title = "Share Report",
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
            // Format selector
            ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Select Format", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple("PDF", Icons.Filled.PictureAsPdf, "PDF Document"),
                            Triple("Image", Icons.Filled.Image, "Image File"),
                            Triple("Text", Icons.Filled.TextSnippet, "Plain Text")
                        ).forEach { (format, icon, desc) ->
                            FormatCard(
                                icon = icon,
                                label = format,
                                isSelected = selectedFormat == format,
                                onClick = { selectedFormat = format },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Share via
            ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Share Via", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    val shareOptions = listOf(
                        Triple(Icons.Filled.Chat, "WhatsApp", {
                            shareReport(context, "WhatsApp", screeningId, selectedFormat); shareSuccess = true
                        }),
                        Triple(Icons.Filled.Bluetooth, "Bluetooth", {
                            shareReport(context, "Bluetooth", screeningId, selectedFormat); shareSuccess = true
                        }),
                        Triple(Icons.Filled.Share, "Nearby Share", {
                            shareReport(context, "Nearby", screeningId, selectedFormat); shareSuccess = true
                        }),
                        Triple(Icons.Filled.MoreHoriz, "More Options", {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = if (selectedFormat == "Image") "image/*" else "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "SwasthAI Health Report - Screening ID: $screeningId")
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Report via"))
                            shareSuccess = true
                        })
                    )

                    shareOptions.forEach { (icon, label, action) ->
                        ShareOptionRow(icon = icon, label = label, onClick = { action() })
                    }
                }
            }

            // Download button
            SwasthAIPrimaryButton(
                text = "Download Report",
                onClick = { downloadReport(context, screeningId, selectedFormat); shareSuccess = true },
                leadingIcon = Icons.Filled.Download
            )

            if (shareSuccess) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                        Text("Report shared successfully!", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
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
        border = androidx.compose.foundation.BorderStroke(
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
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
        }
    }
}

private fun shareReport(context: android.content.Context, via: String, screeningId: String, format: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = if (format == "Image") "image/*" else "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "SwasthAI Health Report")
        putExtra(
            Intent.EXTRA_TEXT,
            "My SwasthAI Health Report\nScreening ID: $screeningId\nFormat: $format\n\nThis report was generated by SwasthAI — Your offline health companion."
        )
        if (via == "WhatsApp") {
            setPackage("com.whatsapp")
        }
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        val fallback = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "SwasthAI Health Report - Screening: $screeningId")
        }
        context.startActivity(Intent.createChooser(fallback, "Share Report"))
    }
}

private fun downloadReport(context: android.content.Context, screeningId: String, format: String) {
    try {
        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val extension = when (format) {
            "Image" -> "png"
            "Text" -> "txt"
            else -> "txt"
        }
        val file = File(dir, "SwasthAI_Report_$screeningId.$extension")
        val content = "SwasthAI Health Report\nScreening ID: $screeningId\nFormat: $format\n\n" +
            "This report was generated by SwasthAI — Your offline health companion."
        FileOutputStream(file).use { it.write(content.toByteArray()) }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, if (format == "Image") "image/*" else "text/plain")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { context.startActivity(viewIntent) }.onFailure {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = if (format == "Image") "image/*" else "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Report saved — Share it"))
        }
    } catch (e: Exception) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "SwasthAI Health Report - Screening ID: $screeningId")
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Report"))
    }
}
