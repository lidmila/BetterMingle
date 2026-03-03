package com.bettermingle.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush

object BetterMingleGradients {

    @Composable
    fun primary(): Brush {
        return Brush.linearGradient(
            colors = listOf(PrimaryBlue, AccentPink)
        )
    }

    @Composable
    fun cta(): Brush {
        return Brush.linearGradient(
            colors = listOf(AccentOrange, AccentGold)
        )
    }

    @Composable
    fun background(): Brush {
        return Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surface,
                BackgroundSecondary
            )
        )
    }

    @Composable
    fun surface(): Brush {
        return Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        )
    }
}
