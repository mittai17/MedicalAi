package com.swasthai.app.core.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.swasthai.app.core.theme.SwasthAIColors
import com.swasthai.app.domain.model.RiskLevel

// ═══════════════════════════════════════
// TOP APP BAR
// ═══════════════════════════════════════

/**
 * SwasthAI branded top app bar with optional offline indicator.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwasthAITopBar(
    title: String,
    modifier: Modifier = Modifier,
    isOnline: Boolean = true,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!isOnline) {
                    OfflineIndicator()
                }
            }
        },
        navigationIcon = {
            if (showBackButton) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        },
        actions = actions,
        modifier = modifier
    )
}

// ═══════════════════════════════════════
// BOTTOM NAVIGATION BAR
// ═══════════════════════════════════════

/**
 * SwasthAI bottom navigation bar.
 * Adapts icons and labels based on the items provided.
 */
@Composable
fun SwasthAIBottomBar(
    items: List<BottomNavItem>,
    selectedRoute: String,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        tonalElevation = 4.dp
    ) {
        items.forEach { item ->
            val selected = selectedRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = { onItemClick(item.route) },
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            )
        }
    }
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

/**
 * Pre-configured bottom nav items for Citizen.
 */
val citizenBottomNavItems = listOf(
    BottomNavItem("citizen_dashboard", "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem("health_records", "Records", Icons.Filled.Description, Icons.Outlined.Description),
    BottomNavItem("connect_providers", "Connect", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder),
    BottomNavItem("alerts_reminders", "Alerts", Icons.Filled.Notifications, Icons.Outlined.Notifications),
    BottomNavItem("citizen_profile", "Profile", Icons.Filled.Person, Icons.Outlined.Person)
)

/**
 * Pre-configured bottom nav items for Health Worker.
 */
val hwBottomNavItems = listOf(
    BottomNavItem("hw_dashboard", "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem("patient_list", "Patients", Icons.Filled.People, Icons.Outlined.People),
    BottomNavItem("hw_reports", "Reports", Icons.Filled.Assessment, Icons.Outlined.Assessment),
    BottomNavItem("hw_alerts", "Alerts", Icons.Filled.Notifications, Icons.Outlined.Notifications),
    BottomNavItem("hw_profile", "Profile", Icons.Filled.Person, Icons.Outlined.Person)
)

// ═══════════════════════════════════════
// OFFLINE INDICATOR
// ═══════════════════════════════════════

/**
 * Small chip showing "Offline Mode" status.
 */
@Composable
fun OfflineIndicator(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.CloudOff,
                contentDescription = "Offline",
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = "Offline Mode",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

/**
 * Online/Offline status badge.
 */
@Composable
fun ConnectionStatusBadge(
    isOnline: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isOnline) SwasthAIColors.RiskLowBackground else SwasthAIColors.RiskHighBackground,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "connection_bg"
    )
    val textColor = if (isOnline) SwasthAIColors.RiskLow else SwasthAIColors.RiskHigh

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (isOnline) Icons.Filled.Wifi else Icons.Filled.WifiOff,
                contentDescription = if (isOnline) "Online" else "Offline",
                modifier = Modifier.size(14.dp),
                tint = textColor
            )
            Text(
                text = if (isOnline) "Online" else "Offline",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
        }
    }
}

// ═══════════════════════════════════════
// RISK BADGE
// ═══════════════════════════════════════

/**
 * Colored badge showing risk level (Low, Moderate, High).
 */
@Composable
fun RiskBadge(
    riskLevel: RiskLevel,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor, text) = when (riskLevel) {
        RiskLevel.LOW -> Triple(SwasthAIColors.RiskLowBackground, SwasthAIColors.RiskLow, "Low")
        RiskLevel.MODERATE -> Triple(SwasthAIColors.RiskModerateBackground, SwasthAIColors.RiskModerate, "Moderate")
        RiskLevel.HIGH -> Triple(SwasthAIColors.RiskHighBackground, SwasthAIColors.RiskHigh, "High")
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}

// ═══════════════════════════════════════
// CARDS
// ═══════════════════════════════════════

/**
 * Elevated card with SwasthAI styling.
 */
@Composable
fun SwasthAICard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (onClick != null) {
        ElevatedCard(
            modifier = modifier,
            onClick = onClick,
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
    } else {
        ElevatedCard(
            modifier = modifier,
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
    }
}

// ═══════════════════════════════════════
// BUTTONS
// ═══════════════════════════════════════

/**
 * Primary filled button with large touch target (48dp min height).
 */
@Composable
fun SwasthAIPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp)
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Secondary outlined button with large touch target.
 */
@Composable
fun SwasthAIOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp)
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ═══════════════════════════════════════
// QUICK ACTION ITEM
// ═══════════════════════════════════════

/**
 * Dashboard quick action grid item with icon and label.
 * Used on both Citizen and HW dashboards.
 */
@Composable
fun QuickActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                if (badge != null) {
                    Badge(
                        modifier = Modifier.align(Alignment.TopEnd),
                        containerColor = MaterialTheme.colorScheme.error
                    ) {
                        Text(badge)
                    }
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ═══════════════════════════════════════
// LOADING OVERLAY
// ═══════════════════════════════════════

/**
 * Full-screen loading overlay with optional message.
 */
@Composable
fun LoadingOverlay(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    message: String = "Loading…"
) {
    if (isLoading) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════
// STEP INDICATOR
// ═══════════════════════════════════════

/**
 * Step progress indicator for multi-step screening flows.
 * Shows numbered circles connected by lines.
 */
@Composable
fun StepIndicator(
    totalSteps: Int,
    currentStep: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (step in 1..totalSteps) {
            val isActive = step <= currentStep
            val bgColor = if (isActive) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant
            val textColor = if (isActive) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(50))
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = step.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = textColor,
                    fontWeight = FontWeight.Bold
                )
            }

            if (step < totalSteps) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(2.dp)
                        .background(
                            if (step < currentStep) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                )
            }
        }
    }
}

// ═══════════════════════════════════════
// SCREENING HISTORY ITEM
// ═══════════════════════════════════════

/**
 * List item showing a past screening with title, date, and risk badge.
 * Used on dashboards and records screens.
 */
@Composable
fun ScreeningHistoryItem(
    title: String,
    date: String,
    riskLevel: RiskLevel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SwasthAICard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RiskBadge(riskLevel = riskLevel)
        }
    }
}
