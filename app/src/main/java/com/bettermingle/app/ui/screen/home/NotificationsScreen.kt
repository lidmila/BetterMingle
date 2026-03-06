package com.bettermingle.app.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bettermingle.app.R
import com.bettermingle.app.ui.component.BetterMingleCard
import com.bettermingle.app.ui.component.EmptyState
import com.bettermingle.app.ui.theme.AccentGold
import com.bettermingle.app.ui.theme.AccentOrange
import com.bettermingle.app.ui.theme.AccentPink
import com.bettermingle.app.ui.theme.BackgroundPrimary
import com.bettermingle.app.ui.theme.PrimaryBlue
import com.bettermingle.app.ui.theme.Spacing
import com.bettermingle.app.ui.theme.Success
import com.bettermingle.app.ui.theme.TextSecondary
import com.bettermingle.app.viewmodel.NotificationsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ActivityItem(
    val id: String = "",
    val eventId: String = "",
    val eventName: String = "",
    val actorName: String = "",
    val actorId: String = "",
    val type: String = "",
    val description: String = "",
    val timestamp: Long = 0,
    val isRead: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onEventClick: (String) -> Unit = {},
    viewModel: NotificationsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Mark as read when screen is shown
    LaunchedEffect(Unit) {
        viewModel.markAllRead()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.notifications_title), style = MaterialTheme.typography.titleMedium) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundPrimary
                )
            )
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.activities.isEmpty() && !uiState.isLoading) {
                EmptyState(
                    icon = Icons.Default.Notifications,
                    title = stringResource(R.string.notifications_empty_title),
                    description = stringResource(R.string.notifications_empty_description),
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(Spacing.screenPadding),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    items(uiState.activities, key = { it.id }) { activity ->
                        ActivityCard(
                            activity = activity,
                            onClick = { onEventClick(activity.eventId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityCard(
    activity: ActivityItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val (icon, color) = activityIconAndColor(activity.type)

    BetterMingleCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(Spacing.md))

            Column(modifier = Modifier.weight(1f)) {
                // Event name
                Text(
                    text = activity.eventName,
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimaryBlue,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Activity description with bold actor name
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                            append(activity.actorName)
                        }
                        append(" ")
                        append(activity.description)
                    },
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(Spacing.xs))

                // Timestamp
                Text(
                    text = formatActivityTime(activity.timestamp, context),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            // Unread indicator
            if (!activity.isRead) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(PrimaryBlue, CircleShape)
                )
            }
        }
    }
}

private fun activityIconAndColor(type: String): Pair<ImageVector, Color> = when (type) {
    "vote" -> Icons.Default.HowToVote to PrimaryBlue
    "expense" -> Icons.Default.Payments to AccentOrange
    "carpool" -> Icons.Default.DirectionsCar to PrimaryBlue
    "room" -> Icons.Default.Hotel to AccentPink
    "chat" -> Icons.AutoMirrored.Filled.Chat to Success
    "schedule" -> Icons.Default.CalendarMonth to AccentGold
    "task" -> Icons.Default.CheckCircle to Success
    "participant" -> Icons.Default.PersonAdd to PrimaryBlue
    "rating" -> Icons.Default.Star to AccentGold
    "settings" -> Icons.Default.Settings to TextSecondary
    "packing" -> Icons.Default.CheckCircle to AccentOrange
    else -> Icons.Default.Notifications to TextSecondary
}

private fun formatActivityTime(timestamp: Long, context: android.content.Context): String {
    if (timestamp == 0L) return ""
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    val minutes = diff / 60_000
    val hours = diff / 3_600_000
    val days = diff / 86_400_000

    return when {
        minutes < 1 -> context.getString(R.string.notifications_time_just_now)
        minutes < 60 -> context.getString(R.string.notifications_time_minutes_ago, minutes.toInt())
        hours < 24 -> context.getString(R.string.notifications_time_hours_ago, hours.toInt())
        days < 7 -> {
            SimpleDateFormat("EEE HH:mm", Locale.getDefault()).format(Date(timestamp))
        }
        else -> SimpleDateFormat("d. M. HH:mm", Locale.getDefault()).format(Date(timestamp))
    }
}
