package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class TelemetryColors(
    val addedContainer: Color,
    val addedContent: Color,
    val cleanContainer: Color,
    val cleanContent: Color,
    val infoContainer: Color,
    val infoContent: Color,
    val neutralBadgeContainer: Color,
    val neutralBadgeContent: Color,
)

private val LightTelemetryColors = TelemetryColors(
    addedContainer = LightAddedContainer,
    addedContent = LightAddedContent,
    cleanContainer = LightCleanContainer,
    cleanContent = LightCleanContent,
    infoContainer = LightInfoContainer,
    infoContent = LightInfoContent,
    neutralBadgeContainer = LightNeutralBadge,
    neutralBadgeContent = LightNeutralBadgeText,
)

private val DarkTelemetryColors = TelemetryColors(
    addedContainer = DarkAddedContainer,
    addedContent = DarkAddedContent,
    cleanContainer = DarkCleanContainer,
    cleanContent = DarkCleanContent,
    infoContainer = DarkInfoContainer,
    infoContent = DarkInfoContent,
    neutralBadgeContainer = DarkNeutralBadge,
    neutralBadgeContent = DarkNeutralBadgeText,
)

val LocalTelemetryColors = staticCompositionLocalOf { LightTelemetryColors }

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainerLow = LightSurface,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
)

@Composable
fun DZContactFixerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val telemetryColors = if (darkTheme) DarkTelemetryColors else LightTelemetryColors

    CompositionLocalProvider(LocalTelemetryColors provides telemetryColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content,
        )
    }
}
