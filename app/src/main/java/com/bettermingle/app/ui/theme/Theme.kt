package com.bettermingle.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

private val BetterMingleLightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = TextOnColor,
    primaryContainer = PrimaryBlue.copy(alpha = 0.08f),
    onPrimaryContainer = PrimaryBlue,

    secondary = AccentPink,
    onSecondary = TextOnColor,
    secondaryContainer = AccentPink.copy(alpha = 0.08f),
    onSecondaryContainer = TextPrimary,

    tertiary = AccentOrange,
    onTertiary = TextOnColor,
    tertiaryContainer = AccentOrange.copy(alpha = 0.08f),
    onTertiaryContainer = TextPrimary,

    background = BackgroundPrimary,
    onBackground = TextPrimary,

    surface = GlassWhite,
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

private val BetterMingleDarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = TextOnColor,
    primaryContainer = PrimaryBlue.copy(alpha = 0.15f),
    onPrimaryContainer = PrimaryBlue,

    secondary = AccentPink,
    onSecondary = TextOnColor,
    secondaryContainer = AccentPink.copy(alpha = 0.15f),
    onSecondaryContainer = DarkTextPrimary,

    tertiary = AccentOrange,
    onTertiary = TextOnColor,
    tertiaryContainer = AccentOrange.copy(alpha = 0.15f),
    onTertiaryContainer = DarkTextPrimary,

    background = DarkBackgroundPrimary,
    onBackground = DarkTextPrimary,

    surface = DarkSurfaceCard,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkBackgroundSecondary,
    onSurfaceVariant = DarkTextSecondary,

    outline = DarkBorderColor,
    outlineVariant = DarkSurfacePeach,

    error = Error,
    onError = TextOnColor,
    errorContainer = DarkErrorBackground,
    onErrorContainer = DarkTextPrimary
)

val LocalBetterMingleColors = staticCompositionLocalOf { LightExtendedColors }

@Composable
fun BetterMingleTheme(
    themeMode: String = "system",
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    val extendedColors = if (useDark) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalBetterMingleColors provides extendedColors) {
        MaterialTheme(
            colorScheme = if (useDark) BetterMingleDarkColorScheme else BetterMingleLightColorScheme,
            typography = BetterMingleTypography,
            shapes = BetterMingleShapes,
            content = content
        )
    }
}

object BetterMingleThemeColors {
    val extended: BetterMingleExtendedColors
        @Composable get() = LocalBetterMingleColors.current
}
