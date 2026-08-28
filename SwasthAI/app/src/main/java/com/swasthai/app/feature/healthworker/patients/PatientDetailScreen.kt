package com.swasthai.app.feature.healthworker.patients

import android.content.Intent
import android.net.Uri
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
import com.swasthai.app.core.components.SwasthAIPrimaryButton
import com.swasthai.app.core.components.SwasthAITopBar
import com.swasthai.app.core.theme.SwasthAIColors
import com.swasthai.app.domain.model.Patient

/**
 * Patient Detail Screen (Screen 18).
 *
 * Shows patient profile: avatar, demographics, screening history.
 * CTA: Start New Screening, Call Patient.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDetailScreen(
    patientId: String,
    onBack: () -> Unit,
    onStartScreening: (String) -> Unit,
    viewModel: PatientViewModel = hiltViewModel()
) {
    val uiState by viewModel.detailUiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(patientId) { viewModel.loadPatientDetail(patientId) }

    Scaffold(
        topBar = {
            SwasthAITopBar(
                title = "Patient Profile",
                showBackButton = true,
                onBackClick = onBack,
                actions = {
                    IconButton(onClick = { /* Edit patient */ }) {
                        Icon(Icons.Filled.Edit, "Edit Patient")
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.patient == null -> Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("Patient not found", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
            }
            else -> {
                val patient = uiState.patient!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Hero
                    PatientHero(patient = patient)

                    // Demographics card
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(4.dp)) {
                            InfoRow(Icons.Filled.Person, "Name", patient.name)
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            InfoRow(Icons.Filled.Cake, "Age", "${patient.age ?: "—"} years")
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            InfoRow(Icons.Filled.Wc, "Gender", patient.gender ?: "—")
                            patient.village?.let {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                InfoRow(Icons.Filled.LocationOn, "Village", it)
                            }
                            patient.phone?.let {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                InfoRow(Icons.Filled.Phone, "Phone", it)
                            }
                            patient.aadharNumber?.let {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                InfoRow(Icons.Filled.Badge, "Aadhaar", "XXXX-XXXX-${it.takeLast(4)}")
                            }
                        }
                    }

                    // Quick stats
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickStatCard("Screenings", patient.screeningCount.toString(), Icons.Filled.HealthAndSafety, SwasthAIColors.RiskLow, Modifier.weight(1f))
                        QuickStatCard("Visits", patient.visitCount.toString(), Icons.Filled.LocalHospital, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Actions
                    Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SwasthAIPrimaryButton(
                            text = "Start Screening",
                            onClick = { onStartScreening(patientId) },
                            leadingIcon = Icons.Filled.MedicalServices
                        )
                        if (patient.phone != null) {
                            OutlinedButton(
                                onClick = {
                                    context.startActivity(
                                        Intent(Intent.ACTION_DIAL, Uri.parse("tel:${patient.phone}"))
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.Call, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Call Patient")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun PatientHero(patient: Patient) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(SwasthAIColors.HWPrimary, SwasthAIColors.HWPrimary.copy(alpha = 0.7f))
                )
            )
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.size(80.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = patient.name.firstOrNull()?.toString()?.uppercase() ?: "P",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
            Text(patient.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
            Text(
                "${patient.age ?: "?"} yrs • ${patient.gender ?: "—"} • ${patient.village ?: "—"}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun QuickStatCard(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier) {
    ElevatedCard(modifier = modifier, shape = RoundedCornerShape(14.dp)) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
