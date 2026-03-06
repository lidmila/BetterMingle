package com.bettermingle.app.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bettermingle.app.ui.theme.AccentGold
import com.bettermingle.app.ui.theme.AccentOrange
import com.bettermingle.app.ui.theme.AccentPink
import com.bettermingle.app.ui.theme.CornerRadius
import com.bettermingle.app.ui.theme.PastelBlue
import com.bettermingle.app.ui.theme.PastelGold
import com.bettermingle.app.ui.theme.PastelGray
import com.bettermingle.app.ui.theme.PastelGreen
import com.bettermingle.app.ui.theme.PastelOrange
import com.bettermingle.app.ui.theme.PastelPink
import com.bettermingle.app.ui.theme.PrimaryBlue
import com.bettermingle.app.ui.theme.Spacing
import com.bettermingle.app.ui.theme.Success
import com.bettermingle.app.ui.theme.TextOnColor
import com.bettermingle.app.ui.theme.TextSecondary
import com.bettermingle.app.utils.debouncedClick
import kotlinx.coroutines.launch

private val CardShape = RoundedCornerShape(CornerRadius.card)
private val BadgeColor = AccentOrange
private val BadgeShape = RoundedCornerShape(CornerRadius.pill)

@Composable
fun FeatureModuleCard(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    subtitle: String = "",
    badgeCount: Int = 0,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    // Map iconTint to pastel background color
    val pastelBg = when (iconTint) {
        PrimaryBlue -> PastelBlue
        AccentPink -> PastelPink
        AccentOrange -> PastelOrange
        Success -> PastelGreen
        AccentGold -> PastelGold
        else -> PastelGray
    }

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 120.dp)
                .scale(scale.value)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            scope.launch {
                                scale.animateTo(0.92f, spring(stiffness = Spring.StiffnessMediumLow))
                            }
                            tryAwaitRelease()
                            scope.launch {
                                scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                            }
                        },
                        onTap = { debouncedClick(action = onClick) }
                    )
                }
                .shadow(
                    elevation = 8.dp,
                    shape = CardShape,
                    ambientColor = iconTint.copy(alpha = 0.20f),
                    spotColor = iconTint.copy(alpha = 0.15f)
                )
                .clip(CardShape)
                .background(pastelBg)
                .padding(Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon in white circle
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.White.copy(alpha = 0.7f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(Spacing.iconMD)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Badge with gradient
        if (badgeCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .background(BadgeColor, BadgeShape)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (badgeCount > 99) "99+" else "$badgeCount",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextOnColor
                )
            }
        }
    }
}
