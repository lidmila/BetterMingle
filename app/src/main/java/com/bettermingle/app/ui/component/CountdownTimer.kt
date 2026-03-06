package com.bettermingle.app.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.bettermingle.app.R
import com.bettermingle.app.ui.theme.AccentGold
import com.bettermingle.app.ui.theme.BetterMingleMotion
import com.bettermingle.app.ui.theme.PrimaryBlue
import com.bettermingle.app.ui.theme.TextOnColor
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CountdownTimer(
    targetTimestamp: Long,
    eventName: String = "",
    modifier: Modifier = Modifier
) {
    var remainingMillis by remember { mutableLongStateOf(targetTimestamp - System.currentTimeMillis()) }

    // Staggered entry animation for each unit
    val scales = List(4) { remember { Animatable(0f) } }

    LaunchedEffect(targetTimestamp) {
        // Staggered spring entrance
        scales.forEachIndexed { index, animatable ->
            launch {
                delay(index * 80L)
                animatable.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
        }

        while (remainingMillis > 0) {
            delay(1000)
            remainingMillis = targetTimestamp - System.currentTimeMillis()
        }
    }

    // Shimmer animation
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = LinearEasing)
        ),
        label = "shimmerOffset"
    )

    // Pulsing colon animation
    val colonAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500)
        ),
        label = "colonAlpha"
    )

    // Pulsing seconds border
    val secondsBorderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000)
        ),
        label = "secondsBorder"
    )

    val days = (remainingMillis / (1000 * 60 * 60 * 24)).coerceAtLeast(0)
    val hours = ((remainingMillis / (1000 * 60 * 60)) % 24).coerceAtLeast(0)
    val minutes = ((remainingMillis / (1000 * 60)) % 60).coerceAtLeast(0)
    val seconds = ((remainingMillis / 1000) % 60).coerceAtLeast(0)

    val shimmerWhite = Color.White.copy(alpha = 0.15f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(PrimaryBlue)
            .drawWithContent {
                drawContent()
                // Shimmer overlay
                val shimmerWidth = size.width * 0.3f
                val x = shimmerOffset * size.width
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.Transparent, shimmerWhite, Color.Transparent),
                        start = Offset(x, 0f),
                        end = Offset(x + shimmerWidth, size.height)
                    )
                )
            }
            .padding(horizontal = 16.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (eventName.isNotEmpty()) {
                Text(
                    text = eventName,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextOnColor.copy(alpha = 0.9f)
                )
                androidx.compose.foundation.layout.Spacer(
                    modifier = Modifier.height(8.dp)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CountdownUnitCard(
                    value = days,
                    label = stringResource(R.string.common_days),
                    scale = scales[0].value
                )

                PulsingColon(alpha = colonAlpha)

                CountdownUnitCard(
                    value = hours,
                    label = stringResource(R.string.common_hours),
                    scale = scales[1].value
                )

                PulsingColon(alpha = colonAlpha)

                CountdownUnitCard(
                    value = minutes,
                    label = stringResource(R.string.common_minutes),
                    scale = scales[2].value
                )

                PulsingColon(alpha = colonAlpha)

                CountdownUnitCard(
                    value = seconds,
                    label = stringResource(R.string.common_seconds),
                    scale = scales[3].value,
                    borderColor = AccentGold.copy(alpha = secondsBorderAlpha)
                )
            }
        }
    }
}

@Composable
private fun CountdownUnitCard(
    value: Long,
    label: String,
    scale: Float,
    borderColor: Color? = null
) {
    val cardShape = RoundedCornerShape(12.dp)

    Box(
        modifier = Modifier
            .width(72.dp)
            .height(80.dp)
            .scale(scale)
            .then(
                if (borderColor != null) {
                    Modifier.border(2.dp, borderColor, cardShape)
                } else {
                    Modifier
                }
            )
            .clip(cardShape)
            .background(Color.White.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedContent(
                targetState = value.toString().padStart(2, '0'),
                transitionSpec = {
                    (slideInVertically(tween(BetterMingleMotion.QUICK)) { -it } +
                            fadeIn(tween(BetterMingleMotion.QUICK)))
                        .togetherWith(
                            slideOutVertically(tween(BetterMingleMotion.QUICK)) { it } +
                                    fadeOut(tween(BetterMingleMotion.QUICK))
                        )
                },
                label = "countdown"
            ) { targetValue ->
                Text(
                    text = targetValue,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextOnColor
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextOnColor.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun PulsingColon(alpha: Float) {
    Text(
        text = ":",
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
        color = TextOnColor.copy(alpha = alpha)
    )
}
