package com.swasthai.app.feature.citizen.connect

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.swasthai.app.core.components.SwasthAITopBar
import com.swasthai.app.core.theme.SwasthAIColors
import com.swasthai.app.domain.model.ConsultationRequest
import com.swasthai.app.domain.model.ConsultationRequestStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Online Consultation tracker (Screen in Flow1 wireframe).
 *
 * SwasthAI does not run live video/audio calls; the user submits a request
 * which is stored offline, queued for backend sync, and its status is tracked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineConsultationScreen(
    onBack: () -> Unit,
    viewModel: OnlineConsultationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var reason by rememberSaveable { mutableStateOf("") }
    var urgency by rememberSaveable { mutableStateOf("NORMAL") }

    Scaffold(
        topBar = {
            SwasthAITopBar(
                title = "Online Consultation",
                showBackButton = true,
                onBackClick = onBack
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.VideoChat,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Request a Consultation",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            "Describe your concern. A health professional from the network will review it and schedule a follow-up. We track the request here — no live video call.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = reason,
                            onValueChange = { reason = it },
                            label = { Text("What's your concern?") },
                            placeholder = { Text("e.g. Persistent fever and cough for 3 days") },
                            minLines = 2,
                            maxLines = 4,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "Urgency",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            UrgencyChip("NORMAL", "Normal", urgency, onSelect = { urgency = it })
                            UrgencyChip("URGENT", "Urgent", urgency, onSelect = { urgency = it })
                            UrgencyChip("HIGH", "High", urgency, onSelect = { urgency = it })
                        }
                        Button(
                            onClick = { viewModel.submitRequest(reason, urgency) },
                            enabled = reason.isNotBlank() && !uiState.submitting,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (uiState.submitting) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(if (uiState.submitting) "Submitting…" else "Submit Request")
                        }
                        uiState.message?.let { message ->
                            val isSuccess = !message.startsWith("Couldn't")
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSuccess) SwasthAIColors.RiskLow
                                else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Your Requests",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (uiState.requests.isEmpty()) {
                item {
                    EmptyConsultationCard()
                }
            } else {
                items(uiState.requests, key = { it.id }) { request ->
                    RequestCard(request)
                }
            }
        }
    }
}

@Composable
private fun UrgencyChip(
    value: String,
    label: String,
    selectedValue: String,
    onSelect: (String) -> Unit
) {
    FilterChip(
        selected = selectedValue == value,
        onClick = { onSelect(value) },
        label = { Text(label) }
    )
}

@Composable
private fun EmptyConsultationCard() {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Filled.ChatBubbleOutline,
                null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(36.dp)
            )
            Text(
                "No consultation requests yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Submit one above and it will appear here.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun RequestCard(request: ConsultationRequest) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusChip(request.status)
                val time = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                    .format(Date(request.createdAt))
                Text(
                    text = time,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Text(
                text = request.reason,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = request.urgency,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Tracked offline · synced when online",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatusChip(status: ConsultationRequestStatus) {
    val (label, color) = when (status) {
        ConsultationRequestStatus.REQUESTED -> Pair("Requested", SwasthAIColors.SyncPending)
        ConsultationRequestStatus.CONFIRMED -> Pair("Confirmed", SwasthAIColors.RiskLow)
        ConsultationRequestStatus.COMPLETED -> Pair("Completed", SwasthAIColors.RiskLow)
        ConsultationRequestStatus.CANCELLED -> Pair("Cancelled", MaterialTheme.colorScheme.outline)
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}