package com.bettermingle.app.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import com.bettermingle.app.ui.theme.AccentOrange
import com.bettermingle.app.ui.theme.Spacing
import com.bettermingle.app.ui.theme.TextSecondary

@Composable
fun ErrorState(
    title: String = "Něco se pokazilo",
    description: String = "Zkontroluj připojení k internetu a zkus to znovu.",
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    var visible by remember { mutableStateOf(false) }
    val iconScale = remember { Animatable(0.3f) }

    LaunchedEffect(Unit) {
        visible = true
        iconScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = "Chyba",
            modifier = Modifier
                .size(Spacing.iconXXL)
                .scale(iconScale.value),
            tint = AccentOrange.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(Spacing.md))

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(400, delayMillis = 150)) +
                    slideInVertically(tween(400, delayMillis = 150, easing = FastOutSlowInEasing)) { it / 3 }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(Spacing.sm))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }

        if (onRetry != null) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(300, delayMillis = 400)) +
                        slideInVertically(tween(300, delayMillis = 400)) { it / 2 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(Spacing.lg))
                    BetterMingleButton(
                        text = "Zkus to znovu",
                        onClick = onRetry,
                        isCta = true
                    )
                }
            }
        }
    }
}
