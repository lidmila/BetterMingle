package com.bettermingle.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val BitterbalLightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = TextOnColor,
    primaryContainer = PrimaryBlue.copy(alpha = 0.12f),
    onPrimaryContainer = PrimaryBlue,

    secondary = AccentPink,
    onSecondary = TextOnColor,
    secondaryContainer = AccentPink.copy(alpha = 0.12f),
    onSecondaryContainer = TextPrimary,

    tertiary = AccentOrange,
    onTertiary = TextOnColor,
    tertiaryContainer = AccentOrange.copy(alpha = 0.12f),
    onTertiaryContainer = TextPrimary,

    background = BackgroundPrimary,
    onBackground = TextPrimary,

    surface = BackgroundPrimary,
    onSurface = TextPrimary,
    surfaceVariant = BackgroundSecondary,
    onSurfaceVariant = TextSecondary,

    outline = BorderColor,
    outlineVariant = SurfacePeach,

    error = Error,
    onError = TextOnColor,
    errorContainer = ErrorBackground,
    onErrorContainer = TextPrimary
)

@Composable
fun BetterMingleTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BitterbalLightColorScheme,
        typography = BetterMingleTypography,
        shapes = BetterMingleShapes,
        content = content
    )
}
