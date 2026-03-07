package com.bettermingle.app.ui.screen.event

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bettermingle.app.R
import com.bettermingle.app.ui.component.BetterMingleCard
import com.bettermingle.app.ui.component.EmptyState
import com.bettermingle.app.ui.theme.AccentGold
import com.bettermingle.app.ui.theme.AccentOrange
import com.bettermingle.app.ui.theme.AccentPink
import com.bettermingle.app.ui.theme.PrimaryBlue
import com.bettermingle.app.ui.theme.Spacing
import com.bettermingle.app.ui.theme.Success

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.Locale

private data class ActivityItem(
    val id: String,
    val actorName: String,
    val type: String,
    val description: String,
    val timestamp: Long
)

@Composable
private fun




        activityIcon(type: String): Pair<ImageVector, Color> = when (type) {
    "voting" -> Icons.Default.HowToVote to PrimaryBlue
    "expenses" -> Icons.Default.Payments to AccentOrange
    "chat" -> Icons.AutoMirrored.Filled.Chat to Success
    "schedule" -> Icons.Default.CalendarMonth to PrimaryBlue
    "tasks" -> Icons.Default.CheckCircle to AccentPink
    "carpool" -> Icons.Default.DirectionsCar to Success
    "participants" -> Icons.Default.People to AccentPink
    "settings" -> Icons.Default.Settings to MaterialTheme.colorScheme.onSurfaceVariant
    else -> Icons.Default.Timeline to AccentGold
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityFeedScreen(
    eventId: String,
    onNavigateBack: () -> Unit
) {
    val activities = remember { mutableStateListOf<ActivityItem>() }
    val dateFormat = remember { java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM, Locale.getDefault()) }
    val timeFormat = remember { java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT, Locale.getDefault()) }

    LaunchedEffect(eventId) {
        try {
            val snapshot = FirebaseFirestore.getInstance()
                .collection("events").document(eventId)
                .collection("activity")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(100)
                .get().await()

            val loaded = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                ActivityItem(
                    id = doc.id,
                    actorName = data["actorName"] as? String ?: "",
                    type = data["type"] as? String ?: "",
                    description = data["description"] as? String ?: "",
                    timestamp = (data["timestamp"] as? Number)?.toLong() ?: 0L
                )
            }
            activities.clear()
            activities.addAll(loaded)
        } catch (_: Exception) { }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.activity_title), style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        if (activities.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Timeline,
                title = stringResource(R.string.activity_empty_title),
                description = stringResource(R.string.activity_empty_description),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            // Group by day
            val grouped = activities.groupBy { dateFormat.format(Date(it.timestamp)) }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(Spacing.screenPadding),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                grouped.forEach { (day, items) ->
                    item(key = "header_$day") {
                        Text(
                            text = day,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryBlue,
                            modifier = Modifier.padding(vertical = Spacing.sm)
                        )
                    }

                    items(items, key = { it.id }) { activity ->
                        val (icon, color) = activityIcon(activity.type)
                        BetterMingleCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(color.copy(alpha = 0.12f)),
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
                                    Text(
                                        text = activity.actorName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = activity.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Text(
                                    text = timeFormat.format(Date(activity.timestamp)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
