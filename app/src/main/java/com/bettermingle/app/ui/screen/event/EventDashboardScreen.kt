package com.bettermingle.app.ui.screen.event

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.twotone.Ballot
import androidx.compose.material.icons.twotone.CalendarMonth
import androidx.compose.material.icons.twotone.ChatBubble
import androidx.compose.material.icons.twotone.Backpack
import androidx.compose.material.icons.twotone.Checklist
import androidx.compose.material.icons.twotone.DirectionsCar
import androidx.compose.material.icons.twotone.Hotel
import androidx.compose.material.icons.twotone.Groups
import androidx.compose.material.icons.twotone.Payments
import androidx.compose.material.icons.twotone.Settings
import androidx.compose.material.icons.twotone.StarRate
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import com.bettermingle.app.R
import com.bettermingle.app.ui.component.BetterMingleButton
import com.bettermingle.app.ui.component.BetterMingleOutlinedButton
import com.bettermingle.app.ui.component.CountdownTimer
import com.bettermingle.app.ui.component.FeatureModuleCard
import com.bettermingle.app.ui.component.QrCodeImage
import androidx.compose.material.icons.twotone.Assessment
import androidx.compose.material.icons.twotone.Timeline
import com.bettermingle.app.ui.theme.AccentGold
import com.bettermingle.app.ui.theme.AccentOrange
import com.bettermingle.app.ui.theme.AccentPink
import com.bettermingle.app.ui.theme.BackgroundSecondary
import com.bettermingle.app.ui.theme.PrimaryBlue
import com.bettermingle.app.ui.theme.BetterMingleMotion
import com.bettermingle.app.ui.theme.Spacing
import com.bettermingle.app.ui.theme.Success

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.bettermingle.app.data.ads.AdManager
import com.bettermingle.app.data.preferences.SettingsManager
import com.bettermingle.app.ui.component.NativeAdCard
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class ModuleInfo(
    val key: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconTint: Color,
    val badgeCount: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDashboardScreen(
    eventId: String,
    onNavigateBack: () -> Unit,
    onModuleClick: (String) -> Unit
) {
    var eventName by remember { mutableStateOf("") }
    var eventDescription by remember { mutableStateOf("") }
    var eventIntroText by remember { mutableStateOf("") }
    var eventLocation by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf<Long?>(null) }
    var endDate by remember { mutableStateOf<Long?>(null) }
    var coverImageUrl by remember { mutableStateOf("") }
    var eventStatus by remember { mutableStateOf("PLANNING") }
    var inviteCode by remember { mutableStateOf("") }
    var participantCount by remember { mutableStateOf(0) }
    val context = LocalContext.current
    var pollCount by remember { mutableStateOf(0) }
    var expenseTotal by remember { mutableStateOf("") }
    var rideCount by remember { mutableStateOf(0) }
    var roomCount by remember { mutableStateOf(0) }
    var scheduleCount by remember { mutableStateOf(0) }
    var messageCount by remember { mutableStateOf(0) }
    var taskCount by remember { mutableStateOf(0) }
    var packingCount by remember { mutableStateOf(0) }
    val badgeCounts = remember { mutableStateMapOf<String, Int>() }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf(false) }

    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    var showShareSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager(context) }
    val settings by settingsManager.settingsFlow.collectAsState(initial = null)
    val showAds = settings?.let { AdManager.hasAds(it.premiumTier) } ?: false

    val currencyCzk = stringResource(R.string.dashboard_currency_czk)
    val activeCountStr = stringResource(R.string.dashboard_active_count)
    val peopleCountStr = stringResource(R.string.dashboard_people_count)
    val ridesCountStr = stringResource(R.string.dashboard_rides_count)
    val roomsCountStr = stringResource(R.string.dashboard_rooms_count)
    val itemsCountStr = stringResource(R.string.dashboard_items_count)
    val messagesCountStr = stringResource(R.string.dashboard_messages_count)
    val tasksCountStr = stringResource(R.string.dashboard_tasks_count)
    val thingsCountStr = stringResource(R.string.dashboard_things_count)
    val votingStr = stringResource(R.string.dashboard_voting)
    val participantsStr = stringResource(R.string.dashboard_participants)
    val expensesStr = stringResource(R.string.dashboard_expenses)
    val carpoolStr = stringResource(R.string.dashboard_carpool)
    val roomsStr = stringResource(R.string.dashboard_rooms)
    val scheduleStr = stringResource(R.string.dashboard_schedule)
    val chatStr = stringResource(R.string.dashboard_chat)
    val tasksStr = stringResource(R.string.dashboard_tasks)
    val packingStr = stringResource(R.string.dashboard_packing)
    val activityStr = stringResource(R.string.dashboard_activity)
    val ratingStr = stringResource(R.string.dashboard_rating)
    val summaryStr = stringResource(R.string.dashboard_summary)
    val settingsStr = stringResource(R.string.dashboard_settings)

    LaunchedEffect(eventId, isLoading) {
        if (!isLoading) return@LaunchedEffect
        val firestore = FirebaseFirestore.getInstance()
        val eventRef = firestore.collection("events").document(eventId)

        try {
            val eventDoc = eventRef.get().await()
            eventName = eventDoc.getString("name") ?: ""
            eventDescription = eventDoc.getString("description") ?: ""
            eventIntroText = eventDoc.getString("introText") ?: ""
            eventLocation = eventDoc.getString("locationName") ?: ""
            startDate = (eventDoc.get("startDate") as? Number)?.toLong()
            endDate = (eventDoc.get("endDate") as? Number)?.toLong()
            coverImageUrl = eventDoc.getString("coverImageUrl") ?: ""
            eventStatus = eventDoc.getString("status") ?: "PLANNING"
            inviteCode = eventDoc.getString("inviteCode") ?: ""
            isLoading = false
        } catch (_: Exception) {
            loadError = true
            isLoading = false
        }

        try {
            val parts = eventRef.collection("participants").get().await()
            participantCount = parts.size()
        } catch (_: Exception) { }

        try {
            val polls = eventRef.collection("polls").get().await()
            pollCount = polls.documents.count { !(it.getBoolean("isClosed") ?: false) }
        } catch (_: Exception) { }

        try {
            val expenses = eventRef.collection("expenses").get().await()
            val total = expenses.documents.sumOf { (it.get("amount") as? Number)?.toDouble() ?: 0.0 }
            expenseTotal = if (total > 0) "${String.format("%,.0f", total)} $currencyCzk" else ""
        } catch (_: Exception) { }

        try { rideCount = eventRef.collection("carpoolRides").get().await().size() } catch (_: Exception) { }
        try { roomCount = eventRef.collection("rooms").get().await().size() } catch (_: Exception) { }
        try { scheduleCount = eventRef.collection("schedule").get().await().size() } catch (_: Exception) { }
        try { messageCount = eventRef.collection("messages").get().await().size() } catch (_: Exception) { }
        try { taskCount = eventRef.collection("tasks").get().await().size() } catch (_: Exception) { }
        try { packingCount = eventRef.collection("packingItems").get().await().size() } catch (_: Exception) { }

        // Load badge counts based on lastSeen
        if (currentUserId.isNotEmpty()) {
            try {
                val lastSeenDoc = eventRef.collection("lastSeen").document(currentUserId).get().await()
                val moduleCollections = mapOf(
                    "voting" to "polls",
                    "expenses" to "expenses",
                    "chat" to "messages",
                    "tasks" to "tasks",
                    "carpool" to "carpoolRides",
                    "rooms" to "rooms",
                    "schedule" to "schedule"
                )
                for ((moduleKey, collection) in moduleCollections) {
                    try {
                        val lastSeen = (lastSeenDoc.get(moduleKey) as? Number)?.toLong() ?: 0L
                        val items = eventRef.collection(collection).get().await()
                        val newCount = items.documents.count { doc ->
                            val createdAt = (doc.get("createdAt") as? Number)?.toLong() ?: 0L
                            createdAt > lastSeen
                        }
                        if (newCount > 0) {
                            badgeCounts[moduleKey] = newCount
                        }
                    } catch (_: Exception) { }
                }
            } catch (_: Exception) { }
        }
    }

    // Write lastSeen on module navigation
    val handleModuleClick: (String) -> Unit = { moduleKey ->
        if (currentUserId.isNotEmpty()) {
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("events").document(eventId)
                .collection("lastSeen").document(currentUserId)
                .set(mapOf(moduleKey to System.currentTimeMillis()), com.google.firebase.firestore.SetOptions.merge())
            badgeCounts.remove(moduleKey)
        }
        onModuleClick(moduleKey)
    }

    val settingsColor = MaterialTheme.colorScheme.onSurfaceVariant
    val modules = remember(participantCount, pollCount, expenseTotal, rideCount, roomCount, scheduleCount, messageCount, taskCount, packingCount, badgeCounts.toMap(), eventStatus, settingsColor) {
        buildList {
            add(ModuleInfo("voting", votingStr, if (pollCount > 0) "$pollCount $activeCountStr" else "", Icons.TwoTone.Ballot, PrimaryBlue, badgeCounts["voting"] ?: 0))
            add(ModuleInfo("participants", participantsStr, if (participantCount > 0) "$participantCount $peopleCountStr" else "", Icons.TwoTone.Groups, AccentPink))
            add(ModuleInfo("expenses", expensesStr, expenseTotal, Icons.TwoTone.Payments, AccentOrange, badgeCounts["expenses"] ?: 0))
            add(ModuleInfo("carpool", carpoolStr, if (rideCount > 0) "$rideCount $ridesCountStr" else "", Icons.TwoTone.DirectionsCar, Success, badgeCounts["carpool"] ?: 0))
            add(ModuleInfo("rooms", roomsStr, if (roomCount > 0) "$roomCount $roomsCountStr" else "", Icons.TwoTone.Hotel, AccentGold, badgeCounts["rooms"] ?: 0))
            add(ModuleInfo("schedule", scheduleStr, if (scheduleCount > 0) "$scheduleCount $itemsCountStr" else "", Icons.TwoTone.CalendarMonth, PrimaryBlue, badgeCounts["schedule"] ?: 0))
            add(ModuleInfo("chat", chatStr, if (messageCount > 0) "$messageCount $messagesCountStr" else "", Icons.TwoTone.ChatBubble, AccentOrange, badgeCounts["chat"] ?: 0))
            add(ModuleInfo("tasks", tasksStr, if (taskCount > 0) "$taskCount $tasksCountStr" else "", Icons.TwoTone.Checklist, AccentPink, badgeCounts["tasks"] ?: 0))
            add(ModuleInfo("packing", packingStr, if (packingCount > 0) "$packingCount $thingsCountStr" else "", Icons.TwoTone.Backpack, Success))
            add(ModuleInfo("activity", activityStr, "", Icons.TwoTone.Timeline, PrimaryBlue))
            add(ModuleInfo("rating", ratingStr, "", Icons.TwoTone.StarRate, AccentGold))
            if (eventStatus == "COMPLETED") {
                add(ModuleInfo("summary", summaryStr, "", Icons.TwoTone.Assessment, AccentPink))
            }
            add(ModuleInfo("settings", settingsStr, "", Icons.TwoTone.Settings, settingsColor))
        }
    }

    // Share bottom sheet
    if (showShareSheet && inviteCode.isNotEmpty()) {
        val inviteLink = "https://bettermingle.app/invite/$inviteCode"
        val shareText = stringResource(R.string.dashboard_share_text, eventName, inviteLink)
        ModalBottomSheet(
            onDismissRequest = { showShareSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.dashboard_share_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(Spacing.lg))

                QrCodeImage(content = inviteLink, size = 180.dp)

                Spacer(modifier = Modifier.height(Spacing.md))

                Text(
                    text = stringResource(R.string.dashboard_share_code, inviteCode),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue
                )

                Spacer(modifier = Modifier.height(Spacing.lg))

                BetterMingleOutlinedButton(
                    text = stringResource(R.string.dashboard_share_copy_link),
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("invite_link", inviteLink))
                        Toast.makeText(context, context.getString(R.string.dashboard_share_link_copied), Toast.LENGTH_SHORT).show()
                    }
                )

                Spacer(modifier = Modifier.height(Spacing.sm))

                BetterMingleButton(
                    text = stringResource(R.string.common_share),
                    onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(intent, context.getString(R.string.dashboard_share_chooser)))
                    },
                    isCta = true
                )

                Spacer(modifier = Modifier.height(Spacing.lg))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        eventName.ifEmpty { stringResource(R.string.dashboard_loading) },
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    if (inviteCode.isNotEmpty()) {
                        IconButton(onClick = { showShareSheet = true }) {
                            Icon(Icons.Default.Share, contentDescription = stringResource(R.string.dashboard_share_invite))
                        }
                    }
                    if (startDate != null) {
                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_INSERT).apply {
                                data = CalendarContract.Events.CONTENT_URI
                                putExtra(CalendarContract.Events.TITLE, eventName)
                                putExtra(CalendarContract.Events.DESCRIPTION, eventDescription)
                                putExtra(CalendarContract.Events.EVENT_LOCATION, eventLocation)
                                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startDate)
                                endDate?.let { putExtra(CalendarContract.EXTRA_EVENT_END_TIME, it) }
                            }
                            context.startActivity(intent)
                        }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = stringResource(R.string.dashboard_add_to_calendar))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { visible = true }

        if (loadError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                com.bettermingle.app.ui.component.ErrorState(
                    onRetry = {
                        loadError = false
                        isLoading = true
                    }
                )
            }
        } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.screenPadding)
        ) {
            Spacer(modifier = Modifier.height(Spacing.sm))

            // Cover image
            if (coverImageUrl.isNotEmpty()) {
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(BetterMingleMotion.STANDARD))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        AsyncImage(
                            model = coverImageUrl,
                            contentDescription = stringResource(R.string.dashboard_cover_description),
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        // Gradient scrim
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .background(
                                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.4f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.md))
            }

            // Description
            if (eventDescription.isNotEmpty()) {
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(BetterMingleMotion.STANDARD)) +
                            slideInVertically(tween(BetterMingleMotion.STANDARD)) { -it / 3 }
                ) {
                    Text(
                        text = parseSimpleMarkdown(eventDescription),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.md))
            }

            // Intro text
            if (eventIntroText.isNotEmpty()) {
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(BetterMingleMotion.STANDARD)) +
                            slideInVertically(tween(BetterMingleMotion.STANDARD)) { -it / 3 }
                ) {
                    androidx.compose.material3.Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Text(
                            text = parseSimpleMarkdown(eventIntroText),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(Spacing.md)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.md))
            }

            startDate?.let { target ->
                if (target > System.currentTimeMillis()) {
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(BetterMingleMotion.STANDARD)) +
                                slideInVertically(tween(BetterMingleMotion.STANDARD)) { -it / 3 }
                    ) {
                        CountdownTimer(targetTimestamp = target, eventName = eventName)
                    }

                    Spacer(modifier = Modifier.height(Spacing.lg))
                }
            }

            // Native ad for FREE tier
            if (showAds) {
                NativeAdCard(
                    modifier = Modifier.padding(vertical = Spacing.sm)
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        BackgroundSecondary,
                        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
                    .padding(horizontal = Spacing.sm, vertical = Spacing.sm)
            ) {
            Text(
                text = stringResource(R.string.dashboard_modules),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = Spacing.xs)
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(vertical = Spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(modules, key = { _, module -> module.key }) { index, module ->
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(BetterMingleMotion.STANDARD, delayMillis = 100 + index * 40)) +
                                scaleIn(
                                    spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    ),
                                    initialScale = 0.7f
                                )
                    ) {
                        FeatureModuleCard(
                            title = module.title,
                            icon = module.icon,
                            iconTint = module.iconTint,
                            subtitle = module.subtitle,
                            badgeCount = module.badgeCount,
                            onClick = { handleModuleClick(module.key) }
                        )
                    }
                }
            }
            }
        }
        }
    }
}

private fun parseSimpleMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            when {
                // Bold: **text**
                text.startsWith("**", i) -> {
                    val end = text.indexOf("**", i + 2)
                    if (end > i) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(text.substring(i + 2, end))
                        }
                        i = end + 2
                    } else {
                        append(text[i])
                        i++
                    }
                }
                // Italic: *text*
                text[i] == '*' && (i == 0 || text[i - 1] != '*') && i + 1 < text.length && text[i + 1] != '*' -> {
                    val end = text.indexOf('*', i + 1)
                    if (end > i) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                else -> {
                    append(text[i])
                    i++
                }
            }
        }
    }
}
