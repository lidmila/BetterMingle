package com.bettermingle.app.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.bettermingle.app.data.model.Event
import com.bettermingle.app.utils.debouncedClick
import com.bettermingle.app.data.model.EventStatus
import com.bettermingle.app.ui.theme.AccentGold
import com.bettermingle.app.ui.theme.AccentOrange
import com.bettermingle.app.ui.theme.AccentPink
import com.bettermingle.app.ui.theme.PrimaryBlue
import com.bettermingle.app.ui.theme.Spacing
import com.bettermingle.app.ui.theme.Success
import com.bettermingle.app.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EventCard(
    event: Event,
    participantCount: Int = 0,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pressScale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    val statusColor = when (event.status) {
        EventStatus.PLANNING -> AccentGold
        EventStatus.CONFIRMED -> PrimaryBlue
        EventStatus.ONGOING -> Success
        EventStatus.COMPLETED -> TextSecondary
        EventStatus.CANCELLED -> AccentOrange
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(pressScale.value)
            .pointerInput(Unit) {
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
                    onTap = { debouncedClick(action = onClick) }
                )
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column {
        Column(modifier = Modifier.padding(Spacing.cardPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = event.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (event.description.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(
                            text = event.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 2
                        )
                    }
                }

                Spacer(modifier = Modifier.width(Spacing.sm))

                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(statusColor)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (event.startDate != null) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(Spacing.iconSM),
                        tint = TextSecondary
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    val dateFormat = SimpleDateFormat("d. M. yyyy", Locale.forLanguageTag("cs"))
                    Text(
                        text = dateFormat.format(Date(event.startDate)),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.width(Spacing.md))
                }

                if (event.locationName.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(Spacing.iconSM),
                        tint = TextSecondary
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text(
                        text = event.locationName,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(Spacing.md))
                }

                if (participantCount > 0) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = null,
                        modifier = Modifier.size(Spacing.iconSM),
                        tint = TextSecondary
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text(
                        text = "$participantCount",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        // Status color bar at the bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(statusColor)
        )
        }
    }
}
