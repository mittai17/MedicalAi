package com.swasthai.app.feature.citizen.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.swasthai.app.core.components.*
import com.swasthai.app.core.theme.SwasthAIColors
import com.swasthai.app.domain.model.ScreeningType

/**
 * Citizen Dashboard Screen (Screen 04) — matches Flow1 wireframe.
 *
 * Displays:
 *  - Greeting + connection status badge
 *  - Quick action grid (Symptom Check, Image Check, Voice, Vitals, Records, Tips)
 *  - Recent screenings list
 *  - Bottom navigation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitizenDashboardScreen(
    onSymptomCheck: () -> Unit,
    onVoiceAssistant: () -> Unit,
    onImageCheck: () -> Unit,
    onVitalsInput: () -> Unit,
    onHealthRecords: () -> Unit,
    onHealthTips: () -> Unit,
    onConnectProviders: () -> Unit,
    onAlertsReminders: () -> Unit,
    onProfile: () -> Unit,
    onScreeningDetail: (String) -> Unit,
    viewModel: CitizenDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val navController = androidx.navigation.compose.rememberNavController()

    Scaffold(
        topBar = {
            SwasthAITopBar(
                title = "SwasthAI",
                isOnline = uiState.isOnline,
                actions = {
                    IconButton(onClick = onProfile) {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = "Profile",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        bottomBar = {
            SwasthAIBottomBar(
                items = citizenBottomNavItems,
                selectedRoute = "citizen_dashboard",
                onItemClick = { route ->
                    when (route) {
                        "health_records" -> onHealthRecords()
                        "connect_providers" -> onConnectProviders()
                        "alerts_reminders" -> onAlertsReminders()
                        "citizen_profile" -> onProfile()
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // ── Hero / Greeting Section ──
            item {
                CitizenHeroSection(
                    userName = uiState.userName,
                    isOnline = uiState.isOnline
                )
            }

            // ── Quick Actions Grid ──
            item {
                QuickActionsSection(
                    onSymptomCheck = onSymptomCheck,
                    onVoiceAssistant = onVoiceAssistant,
                    onImageCheck = onImageCheck,
                    onVitalsInput = onVitalsInput,
                    onHealthRecords = onHealthRecords,
                    onHealthTips = onHealthTips
                )
            }

            // ── Recent Screenings ──
            item {
                Text(
                    text = "Recent Screenings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (uiState.recentScreenings.isEmpty()) {
                item {
                    EmptyScreeningsCard(onStartScreening = onSymptomCheck)
                }
            } else {
                items(uiState.recentScreenings) { item ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInVertically()
                    ) {
                        ScreeningHistoryItem(
                            title = item.title,
                            date = item.date,
                            riskLevel = item.riskLevel,
                            onClick = { onScreeningDetail(item.id) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

// ── Hero Section ──

@Composable
private fun CitizenHeroSection(
    userName: String,
    isOnline: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        SwasthAIColors.CitizenPrimary,
                        SwasthAIColors.CitizenPrimary.copy(alpha = 0.85f)
                    )
                )
            )
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Hello, ${userName.ifBlank { "there" }} 👋",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "How are you feeling today?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
                ConnectionStatusBadge(isOnline = isOnline)
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Offline warning strip
            if (!isOnline) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CloudOff,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "You're offline. All data is saved locally.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// ── Quick Actions ──

@Composable
private fun QuickActionsSection(
    onSymptomCheck: () -> Unit,
    onVoiceAssistant: () -> Unit,
    onImageCheck: () -> Unit,
    onVitalsInput: () -> Unit,
    onHealthRecords: () -> Unit,
    onHealthTips: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        val actions = listOf(
            QuickAction(Icons.Filled.Checklist, "Symptom Check", onSymptomCheck),
            QuickAction(Icons.Filled.Mic, "Voice Assistant", onVoiceAssistant),
            QuickAction(Icons.Filled.CameraAlt, "Image Check", onImageCheck),
            QuickAction(Icons.Filled.MonitorHeart, "Vitals", onVitalsInput),
            QuickAction(Icons.Filled.FolderOpen, "Health Records", onHealthRecords),
            QuickAction(Icons.Filled.Lightbulb, "Health Tips", onHealthTips),
        )

        // 3-column grid
        val rows = actions.chunked(3)
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { action ->
                    QuickActionItem(
                        icon = action.icon,
                        label = action.label,
                        onClick = action.onClick,
                        modifier = Modifier.weight(1f)
                    )
                }
                // Fill remaining slots if row is incomplete
                repeat(3 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

private data class QuickAction(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit
)

// ── Empty State ──

@Composable
private fun EmptyScreeningsCard(onStartScreening: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.HealthAndSafety,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "No screenings yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Start your first health screening to monitor your wellness",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Button(
                onClick = onStartScreening,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Screening")
            }
        }
    }
}
