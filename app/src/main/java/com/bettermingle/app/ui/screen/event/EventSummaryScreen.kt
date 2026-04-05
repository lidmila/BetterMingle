package com.bettermingle.app.ui.screen.event

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.launch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.bettermingle.app.ui.theme.PrimaryBlue
import com.bettermingle.app.ui.theme.Spacing
import com.bettermingle.app.ui.theme.Success

import androidx.compose.material3.CircularProgressIndicator
import androidx.core.content.FileProvider
import com.bettermingle.app.data.preferences.SettingsManager
import com.bettermingle.app.data.preferences.TierLimits
import com.bettermingle.app.data.preferences.AppSettings
import com.bettermingle.app.utils.EventPdfGenerator
import com.bettermingle.app.utils.ParticipantUtils
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal data class SummaryStats(
    val eventName: String = "",
    val eventDescription: String = "",
    val eventTheme: String = "",
    val locationName: String = "",
    val locationAddress: String = "",
    val startDate: Long? = null,
    val endDate: Long? = null,
    val status: String = "",
    val inviteCode: String = "",
    val participantCount: Int = 0,
    val acceptedCount: Int = 0,
    val declinedCount: Int = 0,
    val maybeCount: Int = 0,
    val pendingCount: Int = 0,
    val totalExpenses: Double = 0.0,
    val topPayer: String = "",
    val topPayerAmount: Double = 0.0,
    val messageCount: Int = 0,
    val pollCount: Int = 0,
    val activePollCount: Int = 0,
    val closedPollCount: Int = 0,
    val rideCount: Int = 0,
    val taskCount: Int = 0,
    val packingItemCount: Int = 0,
    val wishlistItemCount: Int = 0,
    val mostActiveParticipant: String = ""
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EventSummaryScreen(
    eventId: String,
    onNavigateBack: () -> Unit,
    onNavigateToUpgrade: () -> Unit = {}
) {
    var stats by remember { mutableStateOf(SummaryStats()) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val settingsManager = remember { SettingsManager(context) }
    val settings by settingsManager.settingsFlow.collectAsState(initial = AppSettings())
    var showExportLimitDialog by remember { mutableStateOf(false) }
    var showShareFormatDialog by remember { mutableStateOf(false) }
    var isGeneratingPdf by remember { mutableStateOf(false) }

    LaunchedEffect(eventId) {
        val firestore = FirebaseFirestore.getInstance()
        val eventRef = firestore.collection("events").document(eventId)

        try {
            val eventDoc = eventRef.get().await()
            val eventName = eventDoc.getString("name") ?: ""
            val eventDescription = eventDoc.getString("description") ?: ""
            val eventTheme = eventDoc.getString("theme") ?: ""
            val locationName = eventDoc.getString("locationName") ?: ""
            val locationAddress = eventDoc.getString("locationAddress") ?: ""
            val startDate = (eventDoc.get("startDate") as? Number)?.toLong()
            val endDate = (eventDoc.get("endDate") as? Number)?.toLong()
            val status = eventDoc.getString("status") ?: ""
            val inviteCode = eventDoc.getString("inviteCode") ?: ""

            // Participants + RSVP breakdown
            val parts = eventRef.collection("participants").get().await()
            val participantCount = parts.size()
            var acceptedCount = 0
            var declinedCount = 0
            var maybeCount = 0
            var pendingCount = 0
            for (doc in parts.documents) {
                when ((doc.getString("rsvp") ?: "PENDING").uppercase()) {
                    "ACCEPTED" -> acceptedCount++
                    "DECLINED" -> declinedCount++
                    "MAYBE" -> maybeCount++
                    else -> pendingCount++
                }
            }

            // Expenses
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
            var topPayerName = ""
            if (topPayerEntry != null) {
                val payerId = topPayerEntry.key
                if (ParticipantUtils.isManualId(payerId)) {
                    try {
                        val partDoc = firestore.collection("events").document(eventId)
                            .collection("participants").document(payerId).get().await()
                        topPayerName = partDoc.getString("displayName") ?: ""
                    } catch (_: Exception) { }
                } else {
                    try {
                        val userDoc = firestore.collection("users").document(payerId).get().await()
                        topPayerName = userDoc.getString("displayName") ?: ""
                    } catch (_: Exception) { }
                }
            }

            // Messages
            val messages = eventRef.collection("messages").get().await()
            val messageCount = messages.size()

            // Polls — active vs closed
            val polls = eventRef.collection("polls").get().await()
            val pollCount = polls.size()
            val now = System.currentTimeMillis()
            var activePollCount = 0
            var closedPollCount = 0
            for (doc in polls.documents) {
                val isClosed = doc.getBoolean("isClosed") ?: false
                val deadline = (doc.get("deadline") as? Number)?.toLong()
                val isExpired = deadline != null && now > deadline
                if (isClosed || isExpired) closedPollCount++ else activePollCount++
            }

            // Rides
            val rides = eventRef.collection("carpoolRides").get().await()
            val rideCount = rides.size()

            // Tasks
            val tasks = eventRef.collection("tasks").get().await()
            val taskCount = tasks.size()

            // Packing items
            val packingItems = eventRef.collection("packingItems").get().await()
            val packingItemCount = packingItems.size()

            // Wishlist items
            val wishlistItems = eventRef.collection("wishlistItems").get().await()
            val wishlistItemCount = wishlistItems.size()

            // Most active participant
            val activities = eventRef.collection("activity").get().await()
            val actorCounts = mutableMapOf<String, Int>()
            for (doc in activities.documents) {
                val actor = doc.getString("actorName") ?: continue
                actorCounts[actor] = (actorCounts[actor] ?: 0) + 1
            }
            val mostActive = actorCounts.maxByOrNull { it.value }?.key ?: ""

            stats = SummaryStats(
                eventName = eventName,
                eventDescription = eventDescription,
                eventTheme = eventTheme,
                locationName = locationName,
                locationAddress = locationAddress,
                startDate = startDate,
                endDate = endDate,
                status = status,
                inviteCode = inviteCode,
                participantCount = participantCount,
                acceptedCount = acceptedCount,
                declinedCount = declinedCount,
                maybeCount = maybeCount,
                pendingCount = pendingCount,
                totalExpenses = totalExpenses,
                topPayer = topPayerName,
                topPayerAmount = topPayerEntry?.value ?: 0.0,
                messageCount = messageCount,
                pollCount = pollCount,
                activePollCount = activePollCount,
                closedPollCount = closedPollCount,
                rideCount = rideCount,
                taskCount = taskCount,
                packingItemCount = packingItemCount,
                wishlistItemCount = wishlistItemCount,
                mostActiveParticipant = mostActive
            )
        } catch (_: Exception) {
            scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.error_load_failed)) }
        }
    }

    val currencyCzk = stringResource(R.string.dashboard_currency_czk)
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.summary_title), style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
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
            // ── Section 1: Event Info ──
            BetterMingleCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.summary_event_info),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Text(
                        text = stats.eventName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (stats.eventTheme.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(
                            text = stats.eventTheme,
                            style = MaterialTheme.typography.bodyMedium,
                            color = PrimaryBlue
                        )
                    }
                    val sd = stats.startDate
                    val ed = stats.endDate
                    if (sd != null) {
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        val dateStr = buildString {
                            append(dateFormat.format(Date(sd)))
                            if (ed != null) {
                                append(" – ")
                                append(dateFormat.format(Date(ed)))
                            }
                        }
                        Text(
                            text = dateStr,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (stats.locationName.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(
                            text = "${stringResource(R.string.summary_location)}: ${stats.locationName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (stats.status.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(
                            text = "${stringResource(R.string.summary_status)}: ${stats.status}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (stats.inviteCode.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(
                            text = "${stringResource(R.string.summary_invite_code)}: ${stats.inviteCode}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // ── Section 2: Participants + RSVP breakdown ──
            BetterMingleCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(AccentPink.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.People,
                                contentDescription = null,
                                tint = AccentPink,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Column {
                            Text(
                                text = "${stats.participantCount}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.summary_participants),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        RsvpChip(stringResource(R.string.summary_rsvp_accepted), stats.acceptedCount, Success)
                        RsvpChip(stringResource(R.string.summary_rsvp_declined), stats.declinedCount, AccentPink)
                        RsvpChip(stringResource(R.string.summary_rsvp_maybe), stats.maybeCount, AccentOrange)
                        RsvpChip(stringResource(R.string.summary_rsvp_pending), stats.pendingCount, MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // ── Section 3: Expenses ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                StatCard(
                    icon = Icons.Default.Payments,
                    value = if (stats.totalExpenses > 0) "${String.format("%,.0f", stats.totalExpenses)} $currencyCzk" else "0 $currencyCzk",
                    label = stringResource(R.string.summary_total_expenses),
                    color = AccentOrange,
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
                Spacer(modifier = Modifier.height(Spacing.sm))
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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

            Spacer(modifier = Modifier.height(Spacing.md))

            // ── Section 4: Polls ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                StatCard(
                    icon = Icons.Default.HowToVote,
                    value = "${stats.pollCount}",
                    label = stringResource(R.string.summary_polls),
                    color = PrimaryBlue,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = Icons.AutoMirrored.Filled.Chat,
                    value = "${stats.messageCount}",
                    label = stringResource(R.string.summary_messages),
                    color = Success,
                    modifier = Modifier.weight(1f)
                )
            }

            if (stats.pollCount > 0) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    RsvpChip(stringResource(R.string.summary_polls_active), stats.activePollCount, PrimaryBlue)
                    RsvpChip(stringResource(R.string.summary_polls_closed), stats.closedPollCount, Success)
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // ── Section 5: Other modules ──
            Text(
                text = stringResource(R.string.summary_other_modules),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
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
                    icon = Icons.Default.Checklist,
                    value = "${stats.taskCount}",
                    label = stringResource(R.string.summary_tasks),
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
                    icon = Icons.Default.Inventory2,
                    value = "${stats.packingItemCount}",
                    label = stringResource(R.string.summary_packing_items),
                    color = AccentOrange,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = Icons.Default.Redeem,
                    value = "${stats.wishlistItemCount}",
                    label = stringResource(R.string.summary_wishlist_items),
                    color = AccentPink,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            // ── CTA: Download overview ──
            val shareHeaderStr = stringResource(R.string.summary_share_header, stats.eventName)
            val shareParticipantsStr = stringResource(R.string.summary_share_participants, stats.participantCount)
            val shareRsvpStr = stringResource(R.string.summary_share_rsvp, stats.acceptedCount, stats.declinedCount, stats.maybeCount, stats.pendingCount)
            val shareExpensesStr = stringResource(R.string.summary_share_expenses, String.format("%,.0f", stats.totalExpenses))
            val shareMessagesStr = stringResource(R.string.summary_share_messages, stats.messageCount)
            val sharePollsStr = stringResource(R.string.summary_share_polls_breakdown, stats.pollCount, stats.activePollCount, stats.closedPollCount)
            val shareTasksStr = stringResource(R.string.summary_share_tasks, stats.taskCount)
            val sharePackingStr = stringResource(R.string.summary_share_packing, stats.packingItemCount)
            val shareWishlistStr = stringResource(R.string.summary_share_wishlist, stats.wishlistItemCount)
            val shareFooterStr = stringResource(R.string.summary_share_footer)

            if (showExportLimitDialog) {
                AlertDialog(
                    onDismissRequest = { showExportLimitDialog = false },
                    title = { Text(stringResource(R.string.tier_export_summary_title)) },
                    text = { Text(stringResource(R.string.tier_export_summary_message)) },
                    confirmButton = {
                        TextButton(onClick = {
                            showExportLimitDialog = false
                            onNavigateToUpgrade()
                        }) { Text(stringResource(R.string.tier_upgrade_button)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showExportLimitDialog = false }) { Text(stringResource(R.string.common_cancel)) }
                    }
                )
            }

            val shareChooserStr = stringResource(R.string.summary_share_chooser)
            val pdfGeneratingStr = stringResource(R.string.summary_pdf_generating)
            val pdfErrorStr = stringResource(R.string.summary_pdf_error)

            if (showShareFormatDialog) {
                AlertDialog(
                    onDismissRequest = { showShareFormatDialog = false },
                    title = { Text(stringResource(R.string.summary_share_format_title)) },
                    text = {
                        Column {
                            TextButton(onClick = {
                                showShareFormatDialog = false
                                val shareText = buildString {
                                    appendLine("\uD83D\uDCCA $shareHeaderStr")
                                    appendLine("\uD83D\uDC65 $shareParticipantsStr")
                                    appendLine(shareRsvpStr)
                                    if (stats.totalExpenses > 0) appendLine("\uD83D\uDCB0 $shareExpensesStr")
                                    appendLine("\uD83D\uDCAC $shareMessagesStr")
                                    appendLine("\uD83D\uDDF3\uFE0F $sharePollsStr")
                                    if (stats.rideCount > 0) appendLine("\uD83D\uDE97 ${stats.rideCount} rides")
                                    if (stats.taskCount > 0) appendLine("\u2705 $shareTasksStr")
                                    if (stats.packingItemCount > 0) appendLine("\uD83C\uDF92 $sharePackingStr")
                                    if (stats.wishlistItemCount > 0) appendLine("\uD83C\uDF81 $shareWishlistStr")
                                    appendLine("\n$shareFooterStr")
                                }
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(Intent.createChooser(intent, shareChooserStr))
                            }) {
                                Text(stringResource(R.string.summary_share_format_text))
                            }
                            TextButton(onClick = {
                                showShareFormatDialog = false
                                isGeneratingPdf = true
                                scope.launch {
                                    try {
                                        val file = withContext(Dispatchers.IO) {
                                            EventPdfGenerator(context).generate(stats)
                                        }
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            file
                                        )
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/pdf"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(intent, shareChooserStr))
                                    } catch (_: Exception) {
                                        snackbarHostState.showSnackbar(pdfErrorStr)
                                    } finally {
                                        isGeneratingPdf = false
                                    }
                                }
                            }) {
                                Text(stringResource(R.string.summary_share_format_pdf))
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { showShareFormatDialog = false }) {
                            Text(stringResource(R.string.common_cancel))
                        }
                    }
                )
            }

            if (isGeneratingPdf) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Text(
                        text = pdfGeneratingStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(Spacing.sm))
            }

            BetterMingleButton(
                text = stringResource(R.string.summary_share_button),
                onClick = {
                    if (!TierLimits.canExportSummary(settings.premiumTier)) {
                        showExportLimitDialog = true
                        return@BetterMingleButton
                    }
                    showShareFormatDialog = true
                },
                isCta = true
            )
        }
    }
}

@Composable
private fun RsvpChip(label: String, count: Int, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = "$label: $count",
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
