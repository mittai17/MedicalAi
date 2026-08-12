package com.swasthai.app.feature.healthworker.sync

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.work.WorkManager
import com.swasthai.app.core.components.SwasthAIPrimaryButton
import com.swasthai.app.core.components.SwasthAITopBar
import com.swasthai.app.core.theme.SwasthAIColors
import com.swasthai.app.sync.SyncWorker
import java.text.SimpleDateFormat
import java.util.*

/**
 * Sync Data Screen (Screen 24 / Phase 7).
 *
 * Shows:
 *  - Current sync status (online/offline)
 *  - Pending records count
 *  - Last sync timestamp
 *  - Manual sync trigger button
 *  - Sync history log
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncDataScreen(
    isOnline: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var isSyncing by remember { mutableStateOf(false) }
    var lastSyncTime by remember { mutableStateOf(System.currentTimeMillis() - 3600_000L) }
    val pendingCount by remember { mutableIntStateOf(7) }

    // Rotating animation when syncing
    val infiniteTransition = rememberInfiniteTransition(label = "sync_rotate")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing)
        ),
        label = "rotate"
    )

    Scaffold(
        topBar = {
            SwasthAITopBar(
                title = "Sync Data",
                showBackButton = true,
                onBackClick = onBack,
                isOnline = isOnline
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
            // Status card
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (isOnline) SwasthAIColors.RiskLowBackground else SwasthAIColors.RiskHighBackground
                )
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(shape = CircleShape, color = if (isOnline) SwasthAIColors.RiskLow else SwasthAIColors.RiskHigh, modifier = Modifier.size(48.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (isOnline) Icons.Filled.Wifi else Icons.Filled.WifiOff,
                                null, tint = Color.White, modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            if (isOnline) "Connected to Server" else "Offline Mode",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isOnline) SwasthAIColors.RiskLow else SwasthAIColors.RiskHigh
                        )
                        Text(
                            if (isOnline) "Data can be synced now" else "Data will sync when connected",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Pending / last sync info
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SyncInfoCard(
                    label = "Pending Records",
                    value = pendingCount.toString(),
                    icon = Icons.Filled.PendingActions,
                    color = if (pendingCount > 0) SwasthAIColors.RiskModerate else SwasthAIColors.RiskLow,
                    modifier = Modifier.weight(1f)
                )
                SyncInfoCard(
                    label = "Last Synced",
                    value = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(lastSyncTime)),
                    icon = Icons.Filled.AccessTime,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }

            // Manual sync button
            Button(
                onClick = {
                    if (isOnline) {
                        isSyncing = true
                        SyncWorker.enqueueImmediateSync(WorkManager.getInstance(context))
                        // Simulate completion after 3s
                        lastSyncTime = System.currentTimeMillis()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = isOnline && !isSyncing
            ) {
                if (isSyncing) {
                    Icon(Icons.Filled.Sync, null, modifier = Modifier.size(20.dp).rotate(rotation))
                } else {
                    Icon(Icons.Filled.CloudUpload, null, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(if (isSyncing) "Syncing…" else "Sync Now", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }

            if (!isOnline) {
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        Text(
                            "Sync will begin automatically when internet is available",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Sync history
            HorizontalDivider()
            Text("Sync History", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

            val history = listOf(
                Triple("✓", "47 records uploaded", "2 hours ago"),
                Triple("✓", "12 records uploaded", "Yesterday, 6:00 PM"),
                Triple("✗", "Failed — no internet", "Yesterday, 2:30 PM"),
                Triple("✓", "31 records uploaded", "2 days ago")
            )
            history.forEach { (status, label, time) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            status,
                            color = if (status == "✓") SwasthAIColors.RiskLow else SwasthAIColors.RiskHigh,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(label, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SyncInfoCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier) {
    ElevatedCard(modifier = modifier, shape = RoundedCornerShape(14.dp)) {
        Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
