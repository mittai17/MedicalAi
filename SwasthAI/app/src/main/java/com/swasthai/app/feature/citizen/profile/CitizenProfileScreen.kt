package com.swasthai.app.feature.citizen.profile

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
import com.swasthai.app.feature.onboarding.OnboardingViewModel

/**
 * Citizen Profile Screen.
 *
 * Shows:
 *  - Avatar initials circle with gradient
 *  - Name, phone, language
 *  - Edit Profile option
 *  - Settings shortcuts (Language, Dark Mode, Notifications)
 *  - Logout button
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitizenProfileScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val userNameState by viewModel.userName.collectAsState(initial = "")
    val userPhoneState by viewModel.userPhone.collectAsState(initial = "")
    val userName = userNameState ?: ""
    val userPhone = userPhoneState ?: ""
    val language by viewModel.language.collectAsState(initial = "en")

    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = { Icon(Icons.Filled.Logout, null) },
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to sign out of SwasthAI?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) { Text("Sign Out", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") }
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
                    // Avatar circle with initials
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (userName.firstOrNull() ?: "U").toString().uppercase(),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                    Text(
                        text = userName.ifBlank { "Citizen User" },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = userPhone.ifBlank { "Not set" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Info section ──
            Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(4.dp)) {
                        ProfileInfoRow(icon = Icons.Filled.Person, label = "Name", value = userName.ifBlank { "Not set" })
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        ProfileInfoRow(icon = Icons.Filled.Phone, label = "Phone", value = userPhone.ifBlank { "Not set" })
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        ProfileInfoRow(icon = Icons.Filled.Language, label = "Language", value = languageLabel(language))
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        ProfileInfoRow(icon = Icons.Filled.Shield, label = "Role", value = "Citizen")
                    }
                }

                // Settings options
                Text("Settings", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))

                ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(4.dp)) {
                        ProfileInfoRow(icon = Icons.Filled.Language, label = "Language", value = languageLabel(language))
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        ProfileInfoRow(icon = Icons.Filled.Notifications, label = "Notifications", value = "Enabled")
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        ProfileActionRow(icon = Icons.Filled.Lock, label = "Privacy & Security", onClick = { showLogoutDialog = true })
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        ProfileActionRow(icon = Icons.Filled.Help, label = "Help & Support", onClick = { showLogoutDialog = true })
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        ProfileActionRow(icon = Icons.Filled.Info, label = "About SwasthAI", onClick = { showLogoutDialog = true })
                    }
                }

                // Logout button
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
private fun ProfileActionRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(onClick = onClick, color = Color.Transparent) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
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
            Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
        }
    }
}

private fun languageLabel(code: String) = when (code) {
    "en" -> "English"
    "hi" -> "हिन्दी (Hindi)"
    "ta" -> "தமிழ் (Tamil)"
    "te" -> "తెలుగు (Telugu)"
    "kn" -> "ಕನ್ನಡ (Kannada)"
    "ml" -> "മലയാളം (Malayalam)"
    "or" -> "ଓଡ଼ିଆ (Odia)"
    else -> code.uppercase()
}
