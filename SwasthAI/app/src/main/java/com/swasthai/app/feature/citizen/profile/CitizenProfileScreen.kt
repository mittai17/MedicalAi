package com.swasthai.app.feature.citizen.profile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.swasthai.app.core.components.SwasthAITopBar
import com.swasthai.app.feature.onboarding.OnboardingViewModel

private val LANGUAGES = listOf(
    Triple("en", "English", "English"),
    Triple("hi", "हिन्दी", "Hindi"),
    Triple("ta", "தமிழ்", "Tamil"),
    Triple("te", "తెలుగు", "Telugu"),
    Triple("kn", "ಕನ್ನಡ", "Kannada"),
    Triple("ml", "മലയാളം", "Malayalam"),
    Triple("or", "ଓଡ଼ିଆ", "Odia")
)

/**
 * Citizen Profile — real data from the stored patient profile plus working
 * settings: language, notifications, dark mode, privacy/help/about info,
 * an edit profile entry point and sign out.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitizenProfileScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onEditProfile: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val userName by viewModel.userName.collectAsState(initial = null)
    val userPhone by viewModel.userPhone.collectAsState(initial = null)
    val userAge by viewModel.userAge.collectAsState(initial = null)
    val userSex by viewModel.userSex.collectAsState(initial = null)
    val userConditions by viewModel.userConditions.collectAsState(initial = emptyList())
    val language by viewModel.language.collectAsState(initial = "en")
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState(initial = true)
    val autoBackgroundRefresh by viewModel.autoBackgroundRefresh.collectAsState(initial = true)
    val darkMode by viewModel.darkMode.collectAsState(initial = false)

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var infoDialog by rememberSaveable { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // Keep the stored preference in sync with the OS grant.
        if (!granted) {
            viewModel.setNotificationsEnabled(false)
        }
    }

    fun onNotificationsToggle(enabled: Boolean) {
        viewModel.setNotificationsEnabled(enabled)
        if (enabled &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context.applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = { Icon(Icons.Filled.Logout, null) },
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to sign out of SwasthAI? Your records stay on this device and will reappear next time.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                        onLogout()
                    }
                ) { Text("Sign Out", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") }
            }
        )
    }

    infoDialog?.let { which ->
        InfoDialog(which = which, onDismiss = { infoDialog = null })
    }

    if (showLanguageDialog) {
        LanguageDialog(
            currentLanguage = language,
            onDismiss = { showLanguageDialog = false },
            onSelect = { code ->
                viewModel.saveLanguage(code)
                showLanguageDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            SwasthAITopBar(
                title = "My Profile",
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
        ) {
            // ── Profile Hero ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    )
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (userName?.firstOrNull() ?: "U").toString().uppercase(),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                    Text(
                        text = userName?.ifBlank { null } ?: "Citizen User",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    val profileLines = buildList {
                        userAge?.let { add("$it yrs") }
                        userSex?.let { add(sexLabel(it)) }
                    }
                    val profileDesc = if (profileLines.isEmpty()) "Profile info not set yet" else profileLines.joinToString(" · ")
                    Text(
                        text = profileDesc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // ── Info section ──
                ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(4.dp)) {
                        ProfileInfoRow(icon = Icons.Filled.Person, label = "Name", value = userName?.ifBlank { null } ?: "Not set")
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        ProfileInfoRow(icon = Icons.Filled.Phone, label = "Phone", value = userPhone?.ifBlank { null } ?: "Not set")
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        ProfileInfoRow(icon = Icons.Filled.Route, label = "Age", value = userAge?.let { "$it years" } ?: "Not set")
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        ProfileInfoRow(icon = Icons.Filled.Female, label = "Sex", value = userSex?.let { sexLabel(it) } ?: "Not set")
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        ProfileInfoRow(
                            icon = Icons.Filled.Shield,
                            label = "Role",
                            value = "Citizen"
                        )
                        if (userConditions.isNotEmpty()) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            ProfileInfoRow(
                                icon = Icons.Filled.MonitorHeart,
                                label = "Conditions",
                                value = userConditions.joinToString(", ")
                            )
                        }
                    }
                }

                // ── Edit Profile ──
                ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(4.dp)) {
                        ProfileActionRow(
                            icon = Icons.Filled.Edit,
                            label = "Edit Profile",
                            onClick = onEditProfile
                        )
                    }
                }

                // ── Settings ──
                Text("Settings", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))

                ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(4.dp)) {
                        ProfileActionRow(
                            icon = Icons.Filled.Language,
                            label = "Language",
                            value = languageLabel(language),
                            onClick = { showLanguageDialog = true }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        ProfileSwitchRow(
                            icon = Icons.Filled.Notifications,
                            label = "Notifications",
                            checked = notificationsEnabled,
                            onCheckedChange = ::onNotificationsToggle
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        ProfileSwitchRow(
                            icon = Icons.Filled.DarkMode,
                            label = "Dark Mode",
                            checked = darkMode,
                            onCheckedChange = { viewModel.setDarkMode(it) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        ProfileSwitchRow(
                            icon = Icons.Filled.Sync,
                            label = "Background data refresh",
                            checked = autoBackgroundRefresh,
                            onCheckedChange = { viewModel.setAutoBackgroundRefresh(it) }
                        )
                    }
                }

                // ── Info & support ──
                Text("Support", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))

                ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(4.dp)) {
                        ProfileActionRow(icon = Icons.Filled.Lock, label = "Privacy & Security", onClick = { infoDialog = "privacy" })
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        ProfileActionRow(icon = Icons.Filled.Help, label = "Help & Support", onClick = { infoDialog = "help" })
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        ProfileActionRow(icon = Icons.Filled.Info, label = "About SwasthAI", onClick = { infoDialog = "about" })
                    }
                }

                // ── Sign out ──
                Spacer(modifier = Modifier.height(8.dp))
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

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "SwasthAI v1.0.0 — Offline Edge AI Health",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun InfoDialog(which: String, onDismiss: () -> Unit) {
    val (title, body) = when (which) {
        "privacy" -> "Privacy & Security" to
            "Your screening data never leaves this device — results are computed by the on-device AI engine. " +
            "Pending records can be synced to the health network later when you go online. " +
            "Signing out keeps your records stored on this phone."
        "help" -> "Help & Support" to
            "Use Symptom Check, Voice Assistant or Image Check to run a screening. " +
            "Results include risk levels and recommendations — always consult a real doctor. " +
            "For support, contact your nearest primary health centre or community health worker."
        else -> "About SwasthAI" to
            "SwasthAI v1.0.0 is an offline edge-AI health companion that runs screening " +
            "models directly on your phone, works without internet, and keeps your data private."
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } }
    )
}

@Composable
private fun LanguageDialog(
    currentLanguage: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Language") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LANGUAGES.forEach { (code, native, english) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(code) }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentLanguage == code,
                            onClick = { onSelect(code) }
                        )
                        Column {
                            Text("$native · $english", fontWeight = if (currentLanguage == code) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ProfileInfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun ProfileActionRow(
    icon: ImageVector,
    label: String,
    value: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!value.isNullOrBlank()) {
                Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ProfileSwitchRow(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun sexLabel(value: String) = when (value.trim().lowercase()) {
    "m", "male" -> "Male"
    "f", "female" -> "Female"
    "o", "other" -> "Other"
    else -> value
}

private fun languageLabel(code: String) = LANGUAGES.firstOrNull { it.first == code }?.let { "${it.third}" }
    ?: code.uppercase()