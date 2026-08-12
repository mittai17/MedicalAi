package com.swasthai.app.feature.healthworker.misc

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.swasthai.app.core.components.SwasthAITopBar
import com.swasthai.app.core.theme.SwasthAIColors
import com.swasthai.app.feature.onboarding.OnboardingViewModel

/**
 * HW Alerts Screen (Screen 25).
 * HW Profile Screen (Screen 26).
 * Help & Support Screen (Screen 27).
 * Settings Screen (Screen 28).
 *
 * All in one file to keep the HW module manageable.
 */

// ═══════════════════════════════════════
// HW ALERTS SCREEN
// ═══════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HWAlertsScreen(onBack: () -> Unit) {
    val alerts = listOf(
        HWAlertItem(Icons.Filled.Warning, "High-Risk Patient", "Ramesh Kumar — High risk, immediate referral needed", SwasthAIColors.RiskHigh, "10 min ago"),
        HWAlertItem(Icons.Filled.Sync, "Sync Required", "7 records pending upload", SwasthAIColors.SyncPending, "1 hr ago"),
        HWAlertItem(Icons.Filled.TransferWithinAStation, "Referral Confirmed", "Sunita Devi's referral to CHC confirmed", SwasthAIColors.RiskLow, "2 hr ago"),
        HWAlertItem(Icons.Filled.Event, "Follow-up Due", "Amit Singh — follow-up visit due today", SwasthAIColors.RiskModerate, "Today"),
        HWAlertItem(Icons.Filled.Vaccines, "Vaccination Camp", "Polio camp at Block A on 5th Aug", MaterialTheme.colorScheme.primary, "Aug 5")
    )

    Scaffold(
        topBar = {
            SwasthAITopBar(title = "Alerts", showBackButton = true, onBackClick = onBack,
                actions = {
                    IconButton(onClick = {}) { Icon(Icons.Filled.DoneAll, "Mark all read") }
                }
            )
        }
    ) { pad ->
        Column(modifier = Modifier.fillMaxSize().padding(pad)) {
            if (alerts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.NotificationsOff, null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.outlineVariant)
                        Text("No alerts", style = MaterialTheme.typography.titleMedium)
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    alerts.forEach { alert -> HWAlertCard(alert) }
                }
            }
        }
    }
}

private data class HWAlertItem(
    val icon: ImageVector, val title: String, val subtitle: String, val color: Color, val time: String
)

@Composable
private fun HWAlertCard(alert: HWAlertItem) {
    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = RoundedCornerShape(12.dp), color = alert.color.copy(alpha = 0.15f), modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(alert.icon, null, tint = alert.color, modifier = Modifier.size(22.dp))
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(alert.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(alert.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(alert.time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                IconButton(onClick = {}, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Filled.Close, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

// ═══════════════════════════════════════
// HW PROFILE SCREEN
// ═══════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HWProfileScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onSettings: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val userName by viewModel.userName.collectAsState(initial = "")
    val userPhone by viewModel.userPhone.collectAsState(initial = "")
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = { Icon(Icons.Filled.Logout, null) },
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to sign out?") },
            confirmButton = {
                TextButton(onClick = { showLogoutDialog = false; viewModel.logout(); onLogout() }) {
                    Text("Sign Out", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            SwasthAITopBar(title = "My Profile", showBackButton = true, onBackClick = onBack,
                actions = {
                    IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, "Settings") }
                })
        }
    ) { pad ->
        Column(modifier = Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState())) {
            // Green hero
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(SwasthAIColors.HWPrimary, SwasthAIColors.HWPrimary.copy(alpha = 0.7f))))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier.size(80.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            (userName?.firstOrNull() ?: "H").toString().uppercase(),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold, color = Color.White
                        )
                    }
                    Text(userName ?: "Health Worker", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                    Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.2f)) {
                        Text("Health Worker", modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(4.dp)) {
                        HWInfoRow(Icons.Filled.Person, "Name", userName ?: "—")
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        HWInfoRow(Icons.Filled.Phone, "Phone", userPhone ?: "—")
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        HWInfoRow(Icons.Filled.Badge, "Role", "Accredited Health Worker (AHW)")
                    }
                }

                ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(4.dp)) {
                        HWActionRow(Icons.Filled.Settings, "Settings", onSettings)
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        HWActionRow(Icons.Filled.Help, "Help & Support") {}
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        HWActionRow(Icons.Filled.Info, "About SwasthAI") {}
                    }
                }

                OutlinedButton(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Filled.Logout, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out", fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun HWInfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = SwasthAIColors.HWPrimary, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun HWActionRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(onClick = onClick, color = Color.Transparent) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                Text(label, style = MaterialTheme.typography.bodyMedium)
            }
            Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
        }
    }
}

// ═══════════════════════════════════════
// HELP & SUPPORT SCREEN
// ═══════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpSupportScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = { SwasthAITopBar(title = "Help & Support", showBackButton = true, onBackClick = onBack) }
    ) { pad ->
        Column(
            modifier = Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            val faqs = listOf(
                "How do I add a new patient?" to "Go to Dashboard → Add Patient, or tap the + FAB on the Patient List screen.",
                "How does offline mode work?" to "All data is saved locally on the device. When connected, it syncs automatically with the server.",
                "What does the risk level mean?" to "Low = Monitor. Moderate = Visit health centre within 24 hrs. High = Immediate medical attention.",
                "How to export a report?" to "Go to Reports → tap the export icon on the top right.",
                "How to update patient records?" to "Open Patient Detail → tap the Edit (pencil) icon in the top right.",
                "What if sync fails repeatedly?" to "Check your internet connection. Go to Sync → Sync Now. If it persists, contact support."
            )
            Text("Frequently Asked Questions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            faqs.forEach { (q, a) ->
                var expanded by remember { mutableStateOf(false) }
                ElevatedCard(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(q, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, tint = MaterialTheme.colorScheme.outline)
                        }
                        if (expanded) {
                            Text(a, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Contact Support", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("📧  support@swasthai.in", style = MaterialTheme.typography.bodySmall)
                    Text("📞  1800-XXX-XXXX (Toll-free)", style = MaterialTheme.typography.bodySmall)
                    Text("🕐  Mon–Sat, 9 AM – 6 PM", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ═══════════════════════════════════════
// SETTINGS SCREEN
// ═══════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    var notificationsEnabled by remember { mutableStateOf(true) }
    var autoSync by remember { mutableStateOf(true) }
    var darkMode by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { SwasthAITopBar(title = "Settings", showBackButton = true, onBackClick = onBack) }
    ) { pad ->
        Column(
            modifier = Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsSection("Notifications") {
                SettingsToggle("Push Notifications", "Alerts and reminders", Icons.Filled.Notifications, notificationsEnabled) { notificationsEnabled = it }
            }
            SettingsSection("Sync") {
                SettingsToggle("Auto Sync", "Sync data when online", Icons.Filled.Sync, autoSync) { autoSync = it }
            }
            SettingsSection("Appearance") {
                SettingsToggle("Dark Mode", "Use dark colour scheme", Icons.Filled.DarkMode, darkMode) { darkMode = it }
            }
            SettingsSection("Account") {
                SettingsActionRow("Change Language", Icons.Filled.Language) {}
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                SettingsActionRow("Privacy Policy", Icons.Filled.PrivacyTip) {}
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                SettingsActionRow("App Version: 1.0.0", Icons.Filled.Info) {}
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 4.dp))
        ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
            content()
        }
    }
}

@Composable
private fun SettingsToggle(label: String, subtitle: String, icon: ImageVector, value: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = value, onCheckedChange = onToggle)
    }
}

@Composable
private fun SettingsActionRow(label: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(onClick = onClick, color = Color.Transparent) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                Text(label, style = MaterialTheme.typography.bodyMedium)
            }
            Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
        }
    }
}
