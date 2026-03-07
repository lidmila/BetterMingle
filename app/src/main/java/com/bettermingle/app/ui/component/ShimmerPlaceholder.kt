package com.bettermingle.app.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.bettermingle.app.ui.theme.BetterMingleThemeColors
import com.bettermingle.app.ui.theme.CardShadow
import com.bettermingle.app.ui.theme.CornerRadius
import com.bettermingle.app.ui.theme.Spacing

private val CardShape = RoundedCornerShape(CornerRadius.card)

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val surfacePeach = MaterialTheme.colorScheme.outlineVariant
    val shimmerColors = listOf(
        surfacePeach.copy(alpha = 0.2f),
        surfacePeach.copy(alpha = 0.4f),
        surfacePeach.copy(alpha = 0.2f)
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 200f, translateAnim - 200f),
        end = Offset(translateAnim, translateAnim)
    )

    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(brush)
    )
}

@Composable
fun ShimmerEventCard(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = CardShape,
                ambientColor = CardShadow,
                spotColor = CardShadow
            )
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(Spacing.cardPadding)
    ) {
        // Top row: status chip + theme tag shimmer pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ShimmerBox(
                modifier = Modifier
                    .width(80.dp)
                    .height(22.dp)
                    .clip(RoundedCornerShape(100.dp))
            )
            ShimmerBox(
                modifier = Modifier
                    .width(90.dp)
                    .height(22.dp)
                    .clip(RoundedCornerShape(100.dp))
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Title shimmer
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(22.dp)
        )

        Spacer(modifier = Modifier.height(Spacing.xs))

        // Description shimmer (2 rows)
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .height(14.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(14.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Date row shimmer
        Row(verticalAlignment = Alignment.CenterVertically) {
            ShimmerBox(modifier = Modifier.size(Spacing.iconSM))
            Spacer(modifier = Modifier.width(Spacing.xs))
            ShimmerBox(
                modifier = Modifier
                    .width(160.dp)
                    .height(14.dp)
            )
        }

        Spacer(modifier = Modifier.height(Spacing.xs))

        // Location row shimmer
        Row(verticalAlignment = Alignment.CenterVertically) {
            ShimmerBox(modifier = Modifier.size(Spacing.iconSM))
            Spacer(modifier = Modifier.width(Spacing.xs))
            ShimmerBox(
                modifier = Modifier
                    .width(120.dp)
                    .height(14.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Divider
        HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

        Spacer(modifier = Modifier.height(12.dp))

        // Bottom row: participant shimmer + icon dots
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShimmerBox(modifier = Modifier.size(Spacing.iconXS))
                Spacer(modifier = Modifier.width(4.dp))
                ShimmerBox(
                    modifier = Modifier
                        .width(30.dp)
                        .height(12.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                ShimmerBox(
                    modifier = Modifier
                        .width(80.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(4) {
                    ShimmerBox(
                        modifier = Modifier
                            .size(Spacing.iconXS)
                            .clip(CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
fun ShimmerListItem(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(Spacing.cardPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShimmerBox(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(16.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(12.dp)
            )
        }
        Spacer(modifier = Modifier.width(Spacing.sm))
        ShimmerBox(
            modifier = Modifier
                .width(60.dp)
                .height(14.dp)
        )
    }
}

@Composable
fun ShimmerModuleContent(
    modifier: Modifier = Modifier,
    itemCount: Int = 5
) {
    Column(
        modifier = modifier.padding(Spacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        repeat(itemCount) {
            ShimmerListItem()
        }
    }
}

@Composable
fun ShimmerModuleCard(
    modifier: Modifier = Modifier,
    index: Int = 0
) {
    val ext = BetterMingleThemeColors.extended
    val shimmerPastels = listOf(ext.pastelBlue, ext.pastelPink, ext.pastelOrange, ext.pastelGreen, ext.pastelGold)
    val pastelBg = shimmerPastels[index % shimmerPastels.size]

    Column(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = CardShape,
                ambientColor = pastelBg.copy(alpha = 0.3f),
                spotColor = pastelBg.copy(alpha = 0.2f)
            )
            .clip(CardShape)
            .background(pastelBg)
            .padding(Spacing.cardPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ShimmerBox(modifier = Modifier.size(32.dp))
        Spacer(modifier = Modifier.height(Spacing.sm))
        ShimmerBox(
            modifier = Modifier
                .width(60.dp)
                .height(14.dp)
        )
    }
}
