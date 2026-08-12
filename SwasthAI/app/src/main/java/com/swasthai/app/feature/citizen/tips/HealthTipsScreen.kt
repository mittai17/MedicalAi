package com.swasthai.app.feature.citizen.tips

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.swasthai.app.core.components.SwasthAITopBar
import com.swasthai.app.core.theme.SwasthAIColors

/**
 * Health Tips Screen (Screen 14 from Flow1 wireframe).
 *
 * Horizontal pager carousel of health tip cards.
 * Each card has gradient background, icon, title, description.
 * Dot pagination indicator.
 * Followed by a list of quick tips.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HealthTipsScreen(
    onBack: () -> Unit
) {
    val tips = healthTips()
    val pagerState = rememberPagerState(pageCount = { tips.size })

    Scaffold(
        topBar = {
            SwasthAITopBar(
                title = "Health Tips",
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
            Spacer(modifier = Modifier.height(16.dp))

            // ── Carousel ──
            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = 32.dp),
                pageSpacing = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            ) { page ->
                HealthTipCard(tip = tips[page])
            }

            // Dot indicators
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tips.indices.forEach { index ->
                    Box(
                        modifier = Modifier
                            .size(if (pagerState.currentPage == index) 10.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (pagerState.currentPage == index)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.outlineVariant
                            )
                    )
                    if (index < tips.lastIndex) Spacer(modifier = Modifier.width(6.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(modifier = Modifier.height(16.dp))

            // ── Quick Tips list ──
            Text(
                text = "Daily Wellness Tips",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            quickTips().forEach { tip ->
                QuickTipRow(tip = tip)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HealthTipCard(tip: HealthTip) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxSize(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(listOf(tip.gradientStart, tip.gradientEnd))
                )
                .padding(24.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(tip.icon, null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }
                Text(
                    text = tip.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = tip.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = tip.category,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickTipRow(tip: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            Icons.Filled.CheckCircle,
            null,
            tint = SwasthAIColors.RiskLow,
            modifier = Modifier.size(20.dp).padding(top = 2.dp)
        )
        Text(
            text = tip,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private data class HealthTip(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val category: String,
    val gradientStart: Color,
    val gradientEnd: Color
)

private fun healthTips() = listOf(
    HealthTip(
        Icons.Filled.WaterDrop,
        "Stay Hydrated",
        "Drink at least 8 glasses of water every day. Proper hydration improves kidney function, skin health, and energy levels.",
        "Hydration",
        Color(0xFF0891B2),
        Color(0xFF06B6D4)
    ),
    HealthTip(
        Icons.Filled.Bedtime,
        "Sleep 7–8 Hours",
        "Quality sleep is essential for immune function, mental health, and physical recovery. Maintain a consistent sleep schedule.",
        "Sleep Health",
        Color(0xFF7C3AED),
        Color(0xFF9333EA)
    ),
    HealthTip(
        Icons.Filled.FitnessCenter,
        "Exercise Regularly",
        "Even 30 minutes of walking daily reduces the risk of heart disease, diabetes, and obesity significantly.",
        "Physical Activity",
        Color(0xFF15803D),
        Color(0xFF16A34A)
    ),
    HealthTip(
        Icons.Filled.Restaurant,
        "Balanced Diet",
        "Include fruits, vegetables, whole grains, and lean proteins. Reduce processed foods, sugar, and excess salt.",
        "Nutrition",
        Color(0xFFD97706),
        Color(0xFFF59E0B)
    ),
    HealthTip(
        Icons.Filled.SelfImprovement,
        "Manage Stress",
        "Practice deep breathing, meditation, or yoga. Chronic stress leads to serious health complications.",
        "Mental Health",
        Color(0xFFE11D48),
        Color(0xFFF43F5E)
    )
)

private fun quickTips() = listOf(
    "Wash your hands frequently — before meals and after bathroom",
    "Cover your mouth when coughing or sneezing",
    "Avoid self-medication — consult a doctor",
    "Get regular health check-ups annually",
    "Keep your vaccinations up to date",
    "Limit alcohol and avoid tobacco",
    "Maintain a healthy weight through diet and exercise",
    "Monitor blood pressure and blood sugar regularly if at risk"
)
