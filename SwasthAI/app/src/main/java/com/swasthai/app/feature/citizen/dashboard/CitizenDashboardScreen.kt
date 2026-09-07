package com.swasthai.app.feature.citizen.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.swasthai.app.feature.citizen.exercise.ExerciseViewModel
import com.swasthai.app.feature.citizen.medications.MedicationsViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitizenDashboardScreen(
    onSymptomCheck: () -> Unit,
    onVoiceCommand: () -> Unit,
    onImageCheck: () -> Unit,
    onHealthTips: () -> Unit,
    onConnectProviders: () -> Unit,
    onAlertsReminders: () -> Unit,
    onProfile: () -> Unit,
    onScreeningDetail: (String) -> Unit,
    onViewAllRecords: () -> Unit = {},
    onNavigateToMedications: () -> Unit = {},
    onNavigateToExercise: () -> Unit = {},
    viewModel: CitizenDashboardViewModel = hiltViewModel(),
    medicationsViewModel: MedicationsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    exerciseViewModel: ExerciseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val medState by medicationsViewModel.uiState.collectAsState()
    val todayExerciseMinutes by exerciseViewModel.todayMinutes.collectAsState()
    val exerciseGoal by exerciseViewModel.dailyGoalMinutes.collectAsState()

    // Medications summary
    val medTaken = medState.todayProgress.first
    val medTotal = medState.todayProgress.second
    val nextReminder = medState.upcomingReminders.firstOrNull()
    val nextReminderLabel = if (nextReminder != null) {
        "Next: ${SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(nextReminder.second.scheduledTimeMillis))}"
    } else if (medTotal > 0) {
        "All done today 🎉"
    } else {
        "No medicines today"
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {

            // ── Header ──────────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        val name = uiState.userName
                        Text(
                            text = "${getGreeting()}${if (name.isNotBlank()) ", $name" else ""}! 👋",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Take small steps for a healthier you.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "Profile",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            // ── Start Health Check CTA ──────────────────────────────────────
            item {
                Card(
                    onClick = onSymptomCheck,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1565C0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Start Health Check",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Check your health in a few minutes",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }
            }

            // ── Today Section Header ────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Today",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onViewAllRecords) {
                        Text(
                            text = "See all",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // ── Today Cards Row ─────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    TodaySummaryCard(
                        modifier = Modifier.weight(1f),
                        emoji = "💊",
                        title = "Medicines",
                        highlight = if (medTotal > 0) "$medTaken / $medTotal taken" else "No medicines",
                        highlightColor = if (medTaken == medTotal && medTotal > 0) Color(0xFF2E7D32) else Color(0xFF1B5E20),
                        subtitle = nextReminderLabel,
                        containerColor = Color(0xFFE8F5E9),
                        onClick = onNavigateToMedications
                    )
                    TodaySummaryCard(
                        modifier = Modifier.weight(1f),
                        emoji = "🚶",
                        title = "Exercise",
                        highlight = "$todayExerciseMinutes min today",
                        highlightColor = if (todayExerciseMinutes >= exerciseGoal) Color(0xFF2E7D32) else Color(0xFF1565C0),
                        subtitle = "Goal: $exerciseGoal min",
                        containerColor = Color(0xFFE3F2FD),
                        onClick = onNavigateToExercise
                    )
                }
            }

            // ── Quick Access ────────────────────────────────────────────────
            item {
                Text(
                    text = "Quick Access",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    QuickAccessCard(
                        modifier = Modifier.weight(1f),
                        emoji = "📅",
                        title = "Appointments",
                        subtitle = "View or book",
                        containerColor = Color(0xFFFCE4EC),
                        onClick = onAlertsReminders
                    )
                    QuickAccessCard(
                        modifier = Modifier.weight(1f),
                        emoji = "👥",
                        title = "Connect",
                        subtitle = "Family / Caregiver",
                        containerColor = Color(0xFFEDE7F6),
                        onClick = onConnectProviders
                    )
                }
            }

            // ── Today's Health Tip Header ───────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Today's Health Tip",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onHealthTips) {
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = "See all tips",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // ── Health Tip Card ─────────────────────────────────────────────
            item {
                Card(
                    onClick = onHealthTips,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "💧", fontSize = 36.sp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Drink enough water\nand stay active.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4E342E),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

private fun getGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Good Morning"
        hour < 17 -> "Good Afternoon"
        else -> "Good Evening"
    }
}

@Composable
private fun TodaySummaryCard(
    modifier: Modifier = Modifier,
    emoji: String,
    title: String,
    highlight: String,
    highlightColor: Color,
    subtitle: String,
    containerColor: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = emoji, fontSize = 28.sp)
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF212121)
            )
            Text(
                text = highlight,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = highlightColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF757575)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF9E9E9E),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickAccessCard(
    modifier: Modifier = Modifier,
    emoji: String,
    title: String,
    subtitle: String,
    containerColor: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = emoji, fontSize = 28.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF757575),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF9E9E9E),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
