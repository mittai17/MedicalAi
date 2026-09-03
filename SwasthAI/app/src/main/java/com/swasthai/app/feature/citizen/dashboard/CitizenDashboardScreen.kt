package com.swasthai.app.feature.citizen.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.swasthai.app.core.components.ConnectionStatusBadge
import com.swasthai.app.core.components.ScreeningHistoryItem
import com.swasthai.app.core.components.SwasthAITopBar
import com.swasthai.app.domain.model.Vitals

/**
 * Citizen Dashboard Screen — primary Home tab.
 *
 * Modern Material 3 layout:
 *  - Slim branded top bar (no bottom bar; the shell owns it)
 *  - Rounded tonal greeting card with a primary health-check CTA
 *  - 2×2 quick-action grid with tinted icon tiles
 *  - Recent screenings feed
 */
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
    viewModel: CitizenDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var editStepsDialog by remember { mutableStateOf(false) }
    var editHeartDialog by remember { mutableStateOf(false) }
    var stepsInput by remember { mutableStateOf("") }
    var heartInput by remember { mutableStateOf("") }

    if (editStepsDialog) {
        AlertDialog(
            onDismissRequest = { editStepsDialog = false },
            icon = { Icon(Icons.Filled.DirectionsWalk, null) },
            title = { Text("Today's Steps") },
            text = {
                OutlinedTextField(
                    value = stepsInput,
                    onValueChange = { stepsInput = it.filter { c -> c.isDigit() }.take(6) },
                    label = { Text("Step count") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        stepsInput.toIntOrNull()?.let { viewModel.setManualSteps(it) }
                        editStepsDialog = false
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editStepsDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (editHeartDialog) {
        AlertDialog(
            onDismissRequest = { editHeartDialog = false },
            icon = { Icon(Icons.Filled.Favorite, null) },
            title = { Text("Heart Rate") },
            text = {
                OutlinedTextField(
                    value = heartInput,
                    onValueChange = { heartInput = it.filter { c -> c.isDigit() }.take(3) },
                    label = { Text("Beats per minute") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        heartInput.toIntOrNull()?.let { viewModel.setManualHeartRate(it) }
                        editHeartDialog = false
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editHeartDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SwasthAITopBar(
                title = "SwasthAI",
                isOnline = uiState.isOnline,
                actions = {
                    IconButton(onClick = onVoiceCommand) {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = "Talk to SwasthAI",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onAlertsReminders) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = "Alerts",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onProfile) {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = "Profile",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // ── Greeting / CTA card ──
            item(key = "hero_section") {
                CitizenHeroSection(
                    userName = uiState.userName,
                    isOnline = uiState.isOnline,
                    onStartScreening = onSymptomCheck
                )
            }

            // ── Health tracking (steps, heart, latest vitals) ──
            item(key = "health_header") {
                SectionHeader(
                    title = "Your Health Today",
                    actionLabel = null,
                    onActionClick = null
                )
            }
            item(key = "health_tracking") {
                HealthTrackingSection(
                    steps = uiState.steps,
                    stepsGoal = uiState.stepsGoal,
                    stepsLive = uiState.stepsLive,
                    heartRate = uiState.heartRate,
                    heartLive = uiState.heartLive,
                    latestVitals = uiState.latestVitals,
                    onEditSteps = {
                        stepsInput = (uiState.steps.takeIf { it > 0 }).toString()
                        editStepsDialog = true
                    },
                    onEditHeart = {
                        heartInput = (uiState.heartRate ?: uiState.latestVitals?.pulse)?.toString().orEmpty()
                        editHeartDialog = true
                    }
                )
            }

            // ── Daily health alerts (hydration, movement, rest, …) ──
            item(key = "alerts_header") {
                SectionHeader(
                    title = "Daily Health Alerts",
                    actionLabel = "Alerts & reminders",
                    onActionClick = onAlertsReminders
                )
            }
            item(key = "alerts_card") {
                DailyHealthAlertsCard()
            }

            // ── Quick actions grid ──
            item(key = "quick_actions_header") {
                SectionHeader(
                    title = "Quick Actions",
                    actionLabel = null,
                    onActionClick = null
                )
            }
            item(key = "quick_actions_grid") {
                QuickActionsSection(
                    onSymptomCheck = onSymptomCheck,
                    onVoiceCommand = onVoiceCommand,
                    onImageCheck = onImageCheck,
                    onHealthTips = onHealthTips
                )
            }

            // ── Recent screenings ──
            item(key = "screenings_header") {
                SectionHeader(
                    title = "Recent Screenings",
                    actionLabel = "View all",
                    onActionClick = onViewAllRecords
                )
            }

            if (uiState.isLoading) {
                item(key = "loading_indicator") {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (uiState.recentScreenings.isEmpty()) {
                item(key = "empty_screenings") {
                    EmptyScreeningsCard(onStartScreening = onSymptomCheck)
                }
            } else {
                items(
                    items = uiState.recentScreenings,
                    key = { it.id },
                    contentType = { "screening_history" }
                ) { item ->
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
    }
}

// ── Section Header ──

@Composable
private fun SectionHeader(
    title: String,
    actionLabel: String?,
    onActionClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        if (actionLabel != null && onActionClick != null) {
            TextButton(onClick = onActionClick) {
                Text(actionLabel, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// ── Hero / Greeting ──

@Composable
private fun CitizenHeroSection(
    userName: String,
    isOnline: Boolean,
    onStartScreening: () -> Unit
) {
    val container = MaterialTheme.colorScheme.primaryContainer
    val content = MaterialTheme.colorScheme.onPrimaryContainer

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(28.dp),
        color = container
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hi${if (userName.isNotBlank()) ", $userName" else ""} 👋",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = content
                )
                ConnectionStatusBadge(isOnline = isOnline)
            }

            Text(
                text = "How are you feeling today? Run a quick, private check — even offline.",
                style = MaterialTheme.typography.bodyMedium,
                color = content.copy(alpha = 0.8f)
            )

            Button(
                onClick = onStartScreening,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = content,
                    contentColor = container
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Thermostat,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start a Health Check", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ── Health widgets (steps + heart) ──

@Composable
private fun HealthTrackingSection(
    steps: Int,
    stepsGoal: Int,
    stepsLive: Boolean,
    heartRate: Int?,
    heartLive: Boolean,
    latestVitals: Vitals?,
    onEditSteps: () -> Unit,
    onEditHeart: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StepsMetricCard(
                steps = steps,
                stepsGoal = stepsGoal,
                stepsLive = stepsLive,
                onClick = onEditSteps,
                modifier = Modifier.weight(1f)
            )
            HealthMetricCard(
                icon = Icons.Filled.Favorite,
                value = (heartRate ?: latestVitals?.pulse)?.toString() ?: "—",
                unit = "bpm",
                label = when {
                    heartLive -> "Heart · live"
                    heartRate != null -> "Heart · manual"
                    else -> "Heart · latest"
                },
                onClick = onEditHeart,
                modifier = Modifier.weight(1f),
                pulse = heartLive
            )
        }
        LatestVitalsGrid(vitals = latestVitals)
    }
}

@Composable
private fun StepsMetricCard(
    steps: Int,
    stepsGoal: Int,
    stepsLive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasValue = stepsLive || steps > 0
    val progress = (steps.toFloat() / stepsGoal).coerceIn(0f, 1f)
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.height(140.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Filled.DirectionsWalk,
                        contentDescription = null,
                        modifier = Modifier.padding(7.dp).size(20.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Text(
                    text = if (stepsLive) "Steps · live" else "Steps · manual",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = if (hasValue) "$steps" else "—",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "steps",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            Column {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Goal · ${steps.coerceAtLeast(0)} / $stepsGoal",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Latest vitals grid ──

@Composable
private fun LatestVitalsGrid(vitals: Vitals?) {
    val temp = vitals?.temperature?.let { "%.1f".format(it) } ?: "—"
    val tempUnit = if (vitals?.temperature != null) "°C" else ""
    val spo2 = vitals?.spo2?.let { it.toInt().toString() } ?: "—"
    val spo2Unit = if (vitals?.spo2 != null) "%" else ""
    val bp = vitals?.bloodPressure ?: "—"
    val bpUnit = if (vitals?.bloodPressure != null) "mmHg" else ""
    val pulse = vitals?.pulse?.toString() ?: "—"
    val pulseUnit = if (vitals?.pulse != null) "bpm" else ""
    val empty = vitals == null

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            VitalsTile(
                icon = Icons.Filled.Thermostat,
                label = "Temperature",
                value = temp,
                unit = tempUnit,
                hint = if (empty) "Run a health check" else "latest check",
                modifier = Modifier.weight(1f)
            )
            VitalsTile(
                icon = Icons.Filled.Air,
                label = "SpO2",
                value = spo2,
                unit = spo2Unit,
                hint = if (empty) "Run a health check" else "latest check",
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            VitalsTile(
                icon = Icons.Filled.Speed,
                label = "Blood Pressure",
                value = bp,
                unit = bpUnit,
                hint = if (empty) "Run a health check" else "latest check",
                modifier = Modifier.weight(1f)
            )
            VitalsTile(
                icon = Icons.Filled.MonitorHeart,
                label = "Pulse",
                value = pulse,
                unit = pulseUnit,
                hint = if (empty) "Run a health check" else "latest check",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun VitalsTile(
    icon: ImageVector,
    label: String,
    value: String,
    unit: String,
    hint: String,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.padding(6.dp).size(16.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
                if (unit.isNotBlank()) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
            Text(
                text = hint,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HealthMetricCard(
    icon: ImageVector,
    value: String,
    unit: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    pulse: Boolean = false
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.height(140.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (pulse) MaterialTheme.colorScheme.tertiaryContainer
                    else MaterialTheme.colorScheme.primaryContainer
                ) {
                    if (pulse) {
                        HeartPulseIcon(
                            modifier = Modifier.padding(7.dp).size(20.dp),
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    } else {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.padding(7.dp).size(20.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(verticalAlignment = androidx.compose.ui.Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            Column {
                Text(
                    text = if (pulse) "Live sensor pulse" else "Normal · 60–100 bpm",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HeartPulseIcon(modifier: Modifier = Modifier, tint: androidx.compose.ui.graphics.Color) {
    val infinite = rememberInfiniteTransition(label = "pulse")
    val scale by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "scale"
    )
    val brighten by infinite.animateFloat(
        initialValue = 0.75f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "brighten"
    )
    Icon(
        imageVector = Icons.Filled.Favorite,
        contentDescription = null,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            alpha = brighten
        },
        tint = tint
    )
}

// ── Daily Health Alerts ──

private data class HealthAlertItem(
    val icon: ImageVector,
    val title: String,
    val detail: String
)

/**
 * Daily health-maintenance nudges shown on the home page: hydration,
 * movement, balanced eating, sleep, breaks and medication timing.
 */
@Composable
private fun DailyHealthAlertsCard() {
    val alerts = remember {
        listOf(
            HealthAlertItem(
                icon = Icons.Filled.WaterDrop,
                title = "Drink water",
                detail = "Stay hydrated — a glass every 1–2 hours"
            ),
            HealthAlertItem(
                icon = Icons.Filled.DirectionsWalk,
                title = "Get moving",
                detail = "30 minutes of walking or light exercise today"
            ),
            HealthAlertItem(
                icon = Icons.Filled.RestaurantMenu,
                title = "Eat balanced",
                detail = "Fruits, vegetables and protein in each meal"
            ),
            HealthAlertItem(
                icon = Icons.Filled.Bedtime,
                title = "Sleep well",
                detail = "Aim for 7–8 hours of rest each night"
            ),
            HealthAlertItem(
                icon = Icons.Filled.SelfImprovement,
                title = "Stretch & breathe",
                detail = "Take a stretch break after every 45 minutes"
            ),
            HealthAlertItem(
                icon = Icons.Filled.Medication,
                title = "Medication on time",
                detail = "Take medicines as prescribed and refill early"
            )
        )
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            alerts.forEach { alert ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(
                            imageVector = alert.icon,
                            contentDescription = null,
                            modifier = Modifier.padding(8.dp).size(20.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = alert.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = alert.detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

// ── Quick Actions ──

@Composable
private fun QuickActionsSection(
    onSymptomCheck: () -> Unit,
    onVoiceCommand: () -> Unit,
    onImageCheck: () -> Unit,
    onHealthTips: () -> Unit
) {
    val actions = remember(onSymptomCheck, onVoiceCommand, onImageCheck, onHealthTips) {
        listOf(
            QuickAction(Icons.Filled.Vaccines, "Symptom Check", onSymptomCheck),
            QuickAction(Icons.Filled.Mic, "Talk to SwasthAI", onVoiceCommand),
            QuickAction(Icons.Filled.CameraAlt, "Image Check", onImageCheck),
            QuickAction(Icons.Filled.Lightbulb, "Health Tips", onHealthTips),
        )
    }

    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        actions.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { action ->
                    ModernQuickAction(
                        icon = action.icon,
                        label = action.label,
                        onClick = action.onClick,
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(2 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ModernQuickAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier
            .height(112.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(10.dp).size(22.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
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
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = Icons.Filled.HealthAndSafety,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp).size(28.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "No screenings yet",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Start your first health check to monitor your wellness.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}