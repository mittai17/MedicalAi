package com.swasthai.app.core.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.swasthai.app.domain.model.UserRole

/**
 * SwasthAI Theme
 *
 * Dynamically switches between Citizen (Blue) and Health Worker (Green)
 * color schemes based on the active user role. Supports system dark mode.
 */

// ═══════════════════════════════════════
// CITIZEN COLOR SCHEMES
// ═══════════════════════════════════════

private val CitizenLightColorScheme = lightColorScheme(
    primary = SwasthAIColors.CitizenPrimary,
    onPrimary = SwasthAIColors.CitizenOnPrimary,
    primaryContainer = SwasthAIColors.CitizenPrimaryContainer,
    onPrimaryContainer = SwasthAIColors.CitizenOnPrimaryContainer,
    secondary = SwasthAIColors.CitizenSecondary,
    onSecondary = SwasthAIColors.CitizenOnSecondary,
    secondaryContainer = SwasthAIColors.CitizenSecondaryContainer,
    onSecondaryContainer = SwasthAIColors.CitizenOnSecondaryContainer,
    tertiary = SwasthAIColors.CitizenTertiary,
    onTertiary = SwasthAIColors.CitizenOnTertiary,
    tertiaryContainer = SwasthAIColors.CitizenTertiaryContainer,
    onTertiaryContainer = SwasthAIColors.CitizenOnTertiaryContainer,
    background = SwasthAIColors.CitizenBackground,
    onBackground = SwasthAIColors.CitizenOnBackground,
    surface = SwasthAIColors.CitizenSurface,
    onSurface = SwasthAIColors.CitizenOnSurface,
    surfaceVariant = SwasthAIColors.CitizenSurfaceVariant,
    onSurfaceVariant = SwasthAIColors.CitizenOnSurfaceVariant,
    outline = SwasthAIColors.CitizenOutline,
    outlineVariant = SwasthAIColors.CitizenOutlineVariant,
    error = SwasthAIColors.CitizenError,
    onError = SwasthAIColors.CitizenOnError,
    errorContainer = SwasthAIColors.CitizenErrorContainer,
    onErrorContainer = SwasthAIColors.CitizenOnErrorContainer,
    surfaceTint = SwasthAIColors.CitizenSurfaceTint,
    inverseSurface = SwasthAIColors.CitizenInverseSurface,
    inverseOnSurface = SwasthAIColors.CitizenInverseOnSurface,
    inversePrimary = SwasthAIColors.CitizenInversePrimary
)

private val CitizenDarkColorScheme = darkColorScheme(
    primary = SwasthAIColors.CitizenPrimaryDark,
    onPrimary = SwasthAIColors.CitizenOnPrimaryDark,
    primaryContainer = SwasthAIColors.CitizenPrimaryContainerDark,
    onPrimaryContainer = SwasthAIColors.CitizenOnPrimaryContainerDark,
    secondary = SwasthAIColors.CitizenSecondaryDark,
    onSecondary = SwasthAIColors.CitizenOnSecondaryDark,
    secondaryContainer = SwasthAIColors.CitizenSecondaryContainerDark,
    onSecondaryContainer = SwasthAIColors.CitizenOnSecondaryContainerDark,
    tertiary = SwasthAIColors.CitizenTertiaryDark,
    onTertiary = SwasthAIColors.CitizenOnTertiaryDark,
    tertiaryContainer = SwasthAIColors.CitizenTertiaryContainerDark,
    onTertiaryContainer = SwasthAIColors.CitizenOnTertiaryContainerDark,
    background = SwasthAIColors.CitizenBackgroundDark,
    onBackground = SwasthAIColors.CitizenOnBackgroundDark,
    surface = SwasthAIColors.CitizenSurfaceDark,
    onSurface = SwasthAIColors.CitizenOnSurfaceDark,
    surfaceVariant = SwasthAIColors.CitizenSurfaceVariantDark,
    onSurfaceVariant = SwasthAIColors.CitizenOnSurfaceVariantDark,
    outline = SwasthAIColors.CitizenOutlineDark,
    outlineVariant = SwasthAIColors.CitizenOutlineVariantDark,
    error = SwasthAIColors.CitizenErrorDark,
    onError = SwasthAIColors.CitizenOnErrorDark
)

// ═══════════════════════════════════════
// HEALTH WORKER COLOR SCHEMES
// ═══════════════════════════════════════

private val HWLightColorScheme = lightColorScheme(
    primary = SwasthAIColors.HWPrimary,
    onPrimary = SwasthAIColors.HWOnPrimary,
    primaryContainer = SwasthAIColors.HWPrimaryContainer,
    onPrimaryContainer = SwasthAIColors.HWOnPrimaryContainer,
    secondary = SwasthAIColors.HWSecondary,
    onSecondary = SwasthAIColors.HWOnSecondary,
    secondaryContainer = SwasthAIColors.HWSecondaryContainer,
    onSecondaryContainer = SwasthAIColors.HWOnSecondaryContainer,
    tertiary = SwasthAIColors.HWTertiary,
    onTertiary = SwasthAIColors.HWOnTertiary,
    tertiaryContainer = SwasthAIColors.HWTertiaryContainer,
    onTertiaryContainer = SwasthAIColors.HWOnTertiaryContainer,
    background = SwasthAIColors.HWBackground,
    onBackground = SwasthAIColors.HWOnBackground,
    surface = SwasthAIColors.HWSurface,
    onSurface = SwasthAIColors.HWOnSurface,
    surfaceVariant = SwasthAIColors.HWSurfaceVariant,
    onSurfaceVariant = SwasthAIColors.HWOnSurfaceVariant,
    outline = SwasthAIColors.HWOutline,
    outlineVariant = SwasthAIColors.HWOutlineVariant,
    error = SwasthAIColors.HWError,
    onError = SwasthAIColors.HWOnError,
    errorContainer = SwasthAIColors.HWErrorContainer,
    onErrorContainer = SwasthAIColors.HWOnErrorContainer,
    surfaceTint = SwasthAIColors.HWSurfaceTint,
    inverseSurface = SwasthAIColors.HWInverseSurface,
    inverseOnSurface = SwasthAIColors.HWInverseOnSurface,
    inversePrimary = SwasthAIColors.HWInversePrimary
)

private val HWDarkColorScheme = darkColorScheme(
    primary = SwasthAIColors.HWPrimaryDark,
    onPrimary = SwasthAIColors.HWOnPrimaryDark,
    primaryContainer = SwasthAIColors.HWPrimaryContainerDark,
    onPrimaryContainer = SwasthAIColors.HWOnPrimaryContainerDark,
    secondary = SwasthAIColors.HWSecondaryDark,
    onSecondary = SwasthAIColors.HWOnSecondaryDark,
    secondaryContainer = SwasthAIColors.HWSecondaryContainerDark,
    onSecondaryContainer = SwasthAIColors.HWOnSecondaryContainerDark,
    tertiary = SwasthAIColors.HWTertiaryDark,
    onTertiary = SwasthAIColors.HWOnTertiaryDark,
    tertiaryContainer = SwasthAIColors.HWTertiaryContainerDark,
    onTertiaryContainer = SwasthAIColors.HWOnTertiaryContainerDark,
    background = SwasthAIColors.HWBackgroundDark,
    onBackground = SwasthAIColors.HWOnBackgroundDark,
    surface = SwasthAIColors.HWSurfaceDark,
    onSurface = SwasthAIColors.HWOnSurfaceDark,
    surfaceVariant = SwasthAIColors.HWSurfaceVariantDark,
    onSurfaceVariant = SwasthAIColors.HWOnSurfaceVariantDark,
    outline = SwasthAIColors.HWOutlineDark,
    outlineVariant = SwasthAIColors.HWOutlineVariantDark,
    error = SwasthAIColors.HWErrorDark,
    onError = SwasthAIColors.HWOnErrorDark
)

@Composable
fun SwasthAITheme(
    userRole: UserRole = UserRole.CITIZEN,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        userRole == UserRole.HEALTH_WORKER && darkTheme -> HWDarkColorScheme
        userRole == UserRole.HEALTH_WORKER -> HWLightColorScheme
        darkTheme -> CitizenDarkColorScheme
        else -> CitizenLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SwasthAITypography,
        shapes = SwasthAIShapes,
        content = content
    )
}
