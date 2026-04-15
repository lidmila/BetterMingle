package com.bettermingle.app.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.bettermingle.app.data.model.Event
import com.bettermingle.app.data.model.EventModule
import com.bettermingle.app.data.model.EventStatus
import com.bettermingle.app.ui.theme.AccentGold
import com.bettermingle.app.ui.theme.AccentOrange
import com.bettermingle.app.ui.theme.AccentPink
import com.bettermingle.app.ui.theme.BetterMingleThemeColors
import com.bettermingle.app.ui.theme.CardShadow
import com.bettermingle.app.ui.theme.CornerRadius
import com.bettermingle.app.ui.theme.PrimaryBlue
import com.bettermingle.app.ui.theme.Spacing
import com.bettermingle.app.ui.theme.Success

import com.bettermingle.app.R
import com.bettermingle.app.utils.debouncedClick
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val CardShape = RoundedCornerShape(CornerRadius.card)

private data class StatusStyle(
    val bgColor: Color,
    val textColor: Color,
    val labelResId: Int
)

@Composable
private fun statusStyle(status: EventStatus): StatusStyle {
    val ext = BetterMingleThemeColors.extended
    return when (status) {
        EventStatus.PLANNING -> StatusStyle(ext.pastelGold, AccentGold, R.string.event_status_planning)
        EventStatus.CONFIRMED -> StatusStyle(ext.pastelBlue, PrimaryBlue, R.string.event_status_confirmed)
        EventStatus.ONGOING -> StatusStyle(ext.pastelGreen, Success, R.string.event_status_ongoing)
        EventStatus.COMPLETED -> StatusStyle(ext.pastelGray, MaterialTheme.colorScheme.onSurfaceVariant, R.string.event_status_completed)
        EventStatus.CANCELLED -> StatusStyle(ext.pastelOrange, AccentOrange, R.string.event_status_cancelled)
    }
}

private fun moduleIcon(module: EventModule): ImageVector = when (module) {
    EventModule.VOTING -> Icons.Default.HowToVote
    EventModule.EXPENSES -> Icons.Default.AccountBalance
    EventModule.CARPOOL -> Icons.Default.DirectionsCar
    EventModule.ROOMS -> Icons.Default.Hotel
    EventModule.CHAT -> Icons.AutoMirrored.Filled.Chat
    EventModule.SCHEDULE -> Icons.Default.Schedule
    EventModule.TASKS -> Icons.Default.CheckCircle
    EventModule.PACKING_LIST -> Icons.Default.Backpack
    EventModule.WISHLIST -> Icons.Default.CardGiftcard
    EventModule.CATERING -> Icons.Default.Restaurant
    EventModule.BUDGET -> Icons.Default.AccountBalance
}

@Composable
fun EventCard(
    event: Event,
    participantCount: Int = 0,
    isHero: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val pressScale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val ext = BetterMingleThemeColors.extended
    val style = statusStyle(event.status)
    val dateFormat = remember { SimpleDateFormat("d. M. yyyy", Locale.forLanguageTag("cs")) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale.value
                scaleY = pressScale.value
            }
            .pointerInput(onLongClick) {
                detectTapGestures(
                    onPress = {
                        scope.launch {
                            pressScale.animateTo(0.96f, spring(stiffness = Spring.StiffnessMediumLow))
                        }
                        tryAwaitRelease()
                        scope.launch {
                            pressScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                        }
                    },
                    onTap = { debouncedClick(action = onClick) },
                    onLongPress = if (onLongClick != null) { { onLongClick() } } else null
                )
            }
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
        // === Zone A: Status chip + Theme tag ===
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status chip
            Text(
                text = stringResource(style.labelResId),
                style = MaterialTheme.typography.labelSmall,
                color = style.textColor,
                modifier = Modifier
                    .background(style.bgColor, RoundedCornerShape(100.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )

            // Theme tag
            if (event.theme.isNotEmpty()) {
                Text(
                    text = event.theme,
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentPink,
                    modifier = Modifier
                        .background(ext.pastelPink, RoundedCornerShape(100.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(if (isHero) 16.dp else 12.dp))

        // === Zone B: Title + Description ===
        Text(
            text = event.name,
            style = if (isHero) {
                MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            } else {
                MaterialTheme.typography.titleLarge
            },
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        if (event.description.isNotEmpty()) {
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = event.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (isHero) 3 else 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(if (isHero) 16.dp else 12.dp))

        // === Zone C: Date range + Location ===
        if (event.startDate != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(Spacing.iconSM),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(Spacing.xs))
                val dateText = if (event.endDate != null && event.endDate != event.startDate) {
                    "${dateFormat.format(Date(event.startDate))} \u2192 ${dateFormat.format(Date(event.endDate))}"
                } else {
                    dateFormat.format(Date(event.startDate))
                }
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (event.locationName.isNotEmpty()) {
            Spacer(modifier = Modifier.height(Spacing.xs))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(Spacing.iconSM),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(Spacing.xs))
                Text(
                    text = event.locationName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // === Zone D: Participants + Module icons ===
        @Suppress("USELESS_ELVIS")
        val safeModules = event.enabledModules ?: emptyList()
        if (participantCount > 0 || safeModules.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Participant ratio + progress
                if (participantCount > 0 || event.maxParticipants > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = null,
                            modifier = Modifier.size(Spacing.iconXS),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        val ratioText = if (event.maxParticipants > 0) {
                            "$participantCount/${event.maxParticipants}"
                        } else {
                            "$participantCount"
                        }
                        Text(
                            text = ratioText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (event.maxParticipants > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            LinearProgressIndicator(
                                progress = {
                                    (participantCount.toFloat() / event.maxParticipants).coerceIn(0f, 1f)
                                },
                                modifier = Modifier
                                    .width(80.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = PrimaryBlue,
                                trackColor = ext.pastelBlue
                            )
                        }
                    }
                }

                // Module icons
                if (safeModules.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val displayModules = safeModules.take(5)
                        val remaining = safeModules.size - 5

                        displayModules.forEach { module ->
                            Icon(
                                imageVector = moduleIcon(module),
                                contentDescription = module.name,
                                modifier = Modifier.size(Spacing.iconXS),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (remaining > 0) {
                            Text(
                                text = "+$remaining",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
