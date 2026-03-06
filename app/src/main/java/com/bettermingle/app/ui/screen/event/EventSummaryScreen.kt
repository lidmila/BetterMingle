package com.bettermingle.app.ui.screen.event

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bettermingle.app.R
import com.bettermingle.app.ui.component.BetterMingleButton
import com.bettermingle.app.ui.component.BetterMingleCard
import com.bettermingle.app.ui.theme.AccentGold
import com.bettermingle.app.ui.theme.AccentOrange
import com.bettermingle.app.ui.theme.AccentPink
import com.bettermingle.app.ui.theme.BackgroundPrimary
import com.bettermingle.app.ui.theme.PrimaryBlue
import com.bettermingle.app.ui.theme.Spacing
import com.bettermingle.app.ui.theme.Success
import com.bettermingle.app.ui.theme.TextSecondary
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

private data class SummaryStats(
    val eventName: String = "",
    val participantCount: Int = 0,
    val totalExpenses: Double = 0.0,
    val topPayer: String = "",
    val topPayerAmount: Double = 0.0,
    val messageCount: Int = 0,
    val pollCount: Int = 0,
    val rideCount: Int = 0,
    val mostActiveParticipant: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventSummaryScreen(
    eventId: String,
    onNavigateBack: () -> Unit
) {
    var stats by remember { mutableStateOf(SummaryStats()) }
    val context = LocalContext.current

    LaunchedEffect(eventId) {
        val firestore = FirebaseFirestore.getInstance()
        val eventRef = firestore.collection("events").document(eventId)

        try {
            val eventDoc = eventRef.get().await()
            val eventName = eventDoc.getString("name") ?: ""

            val parts = eventRef.collection("participants").get().await()
            val participantCount = parts.size()

            val expenses = eventRef.collection("expenses").get().await()
            val totalExpenses = expenses.documents.sumOf { (it.get("amount") as? Number)?.toDouble() ?: 0.0 }

            // Find top payer
            val payerTotals = mutableMapOf<String, Double>()
            for (doc in expenses.documents) {
                val payer = doc.getString("paidBy") ?: continue
                val amount = (doc.get("amount") as? Number)?.toDouble() ?: 0.0
                payerTotals[payer] = (payerTotals[payer] ?: 0.0) + amount
            }
            val topPayerEntry = payerTotals.maxByOrNull { it.value }

            // Resolve top payer name
            var topPayerName = ""
            if (topPayerEntry != null) {
                try {
                    val userDoc = firestore.collection("users").document(topPayerEntry.key).get().await()
                    topPayerName = userDoc.getString("displayName") ?: ""
                } catch (_: Exception) { }
            }

            val messages = eventRef.collection("messages").get().await()
            val messageCount = messages.size()

            val polls = eventRef.collection("polls").get().await()
            val pollCount = polls.size()

            val rides = eventRef.collection("carpoolRides").get().await()
            val rideCount = rides.size()

            // Most active: count activity logs per actor
            val activities = eventRef.collection("activity").get().await()
            val actorCounts = mutableMapOf<String, Int>()
            for (doc in activities.documents) {
                val actor = doc.getString("actorName") ?: continue
                actorCounts[actor] = (actorCounts[actor] ?: 0) + 1
            }
            val mostActive = actorCounts.maxByOrNull { it.value }?.key ?: ""

            stats = SummaryStats(
                eventName = eventName,
                participantCount = participantCount,
                totalExpenses = totalExpenses,
                topPayer = topPayerName,
                topPayerAmount = topPayerEntry?.value ?: 0.0,
                messageCount = messageCount,
                pollCount = pollCount,
                rideCount = rideCount,
                mostActiveParticipant = mostActive
            )
        } catch (_: Exception) { }
    }

    val currencyCzk = stringResource(R.string.dashboard_currency_czk)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.summary_title), style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundPrimary)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.screenPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Event name header
            Text(
                text = stats.eventName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = stringResource(R.string.summary_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(Spacing.xl))

            // Stats grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                StatCard(
                    icon = Icons.Default.People,
                    value = "${stats.participantCount}",
                    label = stringResource(R.string.summary_participants),
                    color = AccentPink,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = Icons.Default.Payments,
                    value = if (stats.totalExpenses > 0) "${String.format("%,.0f", stats.totalExpenses)} $currencyCzk" else "0 $currencyCzk",
                    label = stringResource(R.string.summary_total_expenses),
                    color = AccentOrange,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                StatCard(
                    icon = Icons.AutoMirrored.Filled.Chat,
                    value = "${stats.messageCount}",
                    label = stringResource(R.string.summary_messages),
                    color = Success,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = Icons.Default.HowToVote,
                    value = "${stats.pollCount}",
                    label = stringResource(R.string.summary_polls),
                    color = PrimaryBlue,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                StatCard(
                    icon = Icons.Default.DirectionsCar,
                    value = "${stats.rideCount}",
                    label = stringResource(R.string.summary_rides),
                    color = Success,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = Icons.Default.EmojiEvents,
                    value = stats.mostActiveParticipant.ifEmpty { "-" },
                    label = stringResource(R.string.summary_most_active),
                    color = AccentGold,
                    modifier = Modifier.weight(1f)
                )
            }

            // Top payer highlight
            if (stats.topPayer.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.lg))
                BetterMingleCard {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("\uD83D\uDCB0", style = MaterialTheme.typography.headlineMedium)
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(
                            text = stringResource(R.string.summary_top_payer),
                            style = MaterialTheme.typography.titleSmall,
                            color = TextSecondary
                        )
                        Text(
                            text = stats.topPayer,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${String.format("%,.0f", stats.topPayerAmount)} $currencyCzk",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AccentOrange
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            // Share button
            val shareHeaderStr = stringResource(R.string.summary_share_header, stats.eventName)
            val shareParticipantsStr = stringResource(R.string.summary_share_participants, stats.participantCount)
            val shareExpensesStr = stringResource(R.string.summary_share_expenses, String.format("%,.0f", stats.totalExpenses))
            val shareMessagesStr = stringResource(R.string.summary_share_messages, stats.messageCount)
            val sharePollsStr = stringResource(R.string.summary_share_polls, stats.pollCount)
            val shareFooterStr = stringResource(R.string.summary_share_footer)
            val shareChooserStr = stringResource(R.string.summary_share_chooser)
            BetterMingleButton(
                text = stringResource(R.string.summary_share_button),
                onClick = {
                    val shareText = buildString {
                        appendLine("\uD83D\uDCCA $shareHeaderStr")
                        appendLine("\uD83D\uDC65 $shareParticipantsStr")
                        if (stats.totalExpenses > 0) appendLine("\uD83D\uDCB0 $shareExpensesStr")
                        appendLine("\uD83D\uDCAC $shareMessagesStr")
                        appendLine("\uD83D\uDDF3\uFE0F $sharePollsStr")
                        appendLine("\n$shareFooterStr")
                    }
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(intent, shareChooserStr))
                },
                isCta = true
            )
        }
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    BetterMingleCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}
