package com.swasthai.app.feature.citizen.connect

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.swasthai.app.core.components.SwasthAITopBar
import com.swasthai.app.core.theme.SwasthAIColors

/**
 * Connect & Providers Screen (Screen 10 from Flow1 wireframe).
 *
 * Options:
 *  - Find Nearby Health Centres (map intent)
 *  - Call Health Worker (phone intent)
 *  - Emergency Services 108
 *  - Online Consultation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectProvidersScreen(
    onBack: () -> Unit,
    onOpenConsultation: () -> Unit = {}
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            SwasthAITopBar(
                title = "Connect & Providers"
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Get Help & Connect",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Access healthcare services and connect with professionals",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Emergency card — always at top
            EmergencyCard(
                onCall108 = {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:108"))
                    context.startActivity(intent)
                }
            )

            // Find Nearby Health Centres
            ConnectOptionCard(
                icon = Icons.Filled.LocalHospital,
                title = "Find Nearby Health Centres",
                subtitle = "Locate PHC, CHC and hospitals near you",
                iconColor = MaterialTheme.colorScheme.primary,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("geo:0,0?q=primary+health+centre+near+me")
                    }
                    try { context.startActivity(intent) } catch (_: Exception) {}
                }
            )

            // Call Health Worker
            ConnectOptionCard(
                icon = Icons.Filled.RecordVoiceOver,
                title = "Call Health Worker",
                subtitle = "Free National Health Helpline (104)",
                iconColor = SwasthAIColors.RiskLow,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                onClick = {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:104"))
                    context.startActivity(intent)
                }
            )

            // Online Consultation (request tracker)
            ConnectOptionCard(
                icon = Icons.Filled.VideoChat,
                title = "Online Consultation",
                subtitle = "Request a consultation with a health professional",
                iconColor = MaterialTheme.colorScheme.tertiary,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                onClick = onOpenConsultation
            )

            // Government helplines
            HelplineSection()

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun EmergencyCard(onCall108: () -> Unit) {
    ElevatedCard(
        onClick = onCall108,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = SwasthAIColors.RiskHighBackground
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = SwasthAIColors.RiskHigh,
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Call,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "Emergency Services",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SwasthAIColors.RiskHigh
                    )
                    Text(
                        text = "Call 108 — Ambulance & Emergency",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = "108",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = SwasthAIColors.RiskHigh
            )
        }
    }
}

@Composable
private fun ConnectOptionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color,
    containerColor: Color,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
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
                color = containerColor,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun HelplineSection() {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Government Helplines", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

            val helplines = listOf(
                "108" to "Medical Emergency",
                "104" to "Health Helpline (Free)",
                "112" to "National Emergency Number",
                "1800-11-4477" to "NPPC Mental Health Helpline"
            )
            helplines.forEach { (number, label) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(number, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
