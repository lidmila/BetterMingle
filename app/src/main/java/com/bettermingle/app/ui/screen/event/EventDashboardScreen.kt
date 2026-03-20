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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.twotone.Assessment
import androidx.compose.material.icons.twotone.Backpack
import androidx.compose.material.icons.twotone.Ballot
import androidx.compose.material.icons.twotone.CalendarMonth
import androidx.compose.material.icons.twotone.CardGiftcard
import androidx.compose.material.icons.twotone.ChatBubble
import androidx.compose.material.icons.twotone.Checklist
import androidx.compose.material.icons.twotone.DirectionsCar
import androidx.compose.material.icons.twotone.Groups
import androidx.compose.material.icons.twotone.Hotel
import androidx.compose.material.icons.twotone.Payments
import androidx.compose.material.icons.twotone.Settings
import androidx.compose.material.icons.twotone.StarRate
import androidx.compose.material.icons.twotone.Timeline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.bettermingle.app.R
import com.bettermingle.app.data.ads.AdManager
import com.bettermingle.app.data.model.EventModule
import com.bettermingle.app.data.preferences.PremiumTier
import com.bettermingle.app.data.preferences.SettingsManager
import com.bettermingle.app.data.preferences.TierLimits
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.twotone.Restaurant
import com.bettermingle.app.ui.component.CoachMarkBanner
import com.bettermingle.app.ui.component.CoachMarkIds
import com.bettermingle.app.ui.component.BetterMingleButton
import com.bettermingle.app.ui.component.BetterMingleOutlinedButton
import com.bettermingle.app.ui.component.CountdownTimer
import com.bettermingle.app.ui.component.FeatureModuleCard
import com.bettermingle.app.ui.component.NativeAdCard
import com.bettermingle.app.ui.component.QrCodeImage
import com.bettermingle.app.ui.theme.AccentGold
import com.bettermingle.app.ui.theme.AccentOrange
import com.bettermingle.app.ui.theme.AccentPink
import com.bettermingle.app.ui.theme.BackgroundSecondary
import com.bettermingle.app.ui.theme.BetterMingleMotion
import com.bettermingle.app.ui.theme.BetterMingleThemeColors
import com.bettermingle.app.ui.theme.PrimaryBlue
import com.bettermingle.app.ui.theme.Spacing
import com.bettermingle.app.ui.theme.Success
import com.bettermingle.app.ui.theme.hexToColor
import com.bettermingle.app.data.repository.EventRepository
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TextButton
import androidx.core.content.FileProvider
import com.bettermingle.app.data.preferences.AppSettings
import com.bettermingle.app.utils.EventPdfGenerator
import com.bettermingle.app.utils.PdfSection
import com.bettermingle.app.utils.loadDetailedEventReport
import androidx.compose.material3.Switch
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Collections

data class ModuleInfo(
    val key: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconTint: Color,
    val badgeCount: Int = 0
)

private val MODULE_KEY_TO_ENUM = mapOf(
    "voting" to EventModule.VOTING,
    "expenses" to EventModule.EXPENSES,
    "carpool" to EventModule.CARPOOL,
    "rooms" to EventModule.ROOMS,
    "chat" to EventModule.CHAT,
    "schedule" to EventModule.SCHEDULE,
    "tasks" to EventModule.TASKS,
    "packing" to EventModule.PACKING_LIST,
    "wishlist" to EventModule.WISHLIST,
    "catering" to EventModule.CATERING,
    "budget" to EventModule.BUDGET
)

private val SYSTEM_MODULE_KEYS = setOf("participants", "activity", "rating", "summary", "settings")

private sealed interface GridItem {
    data class ModuleItem(val info: ModuleInfo, val isSystem: Boolean) : GridItem
    data object AddItem : GridItem
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDashboardScreen(
    eventId: String,
    onNavigateBack: () -> Unit,
    onModuleClick: (String) -> Unit,
    onDeleteEvent: (() -> Unit)? = null,
    onDuplicateEvent: ((name: String, description: String, location: String, introText: String, theme: String, modules: List<String>) -> Unit)? = null,
    onNavigateToUpgrade: () -> Unit = {}
) {
    var eventName by remember { mutableStateOf("") }
    var eventDescription by remember { mutableStateOf("") }
    var eventIntroText by remember { mutableStateOf("") }
    var eventLocation by remember { mutableStateOf("") }
    var eventTheme by remember { mutableStateOf("") }
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
    var wishlistCount by remember { mutableStateOf(0) }
    val badgeCounts = remember { mutableStateMapOf<String, Int>() }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf(false) }

    var enabledModules by remember { mutableStateOf<List<EventModule>>(emptyList()) }
    var isOrganizer by remember { mutableStateOf(false) }
    var isCreator by remember { mutableStateOf(false) }
    var showAddModuleSheet by remember { mutableStateOf(false) }
    var moduleColors by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    // Drag-and-drop state
    var draggedKey by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    val cardCenters = remember { mutableStateMapOf<String, Offset>() }

    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    var showShareSheet by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showDeleteEventDialog by remember { mutableStateOf(false) }
    var isExportingPdf by remember { mutableStateOf(false) }
    var showExportUpgradeDialog by remember { mutableStateOf(false) }
    var showPdfSectionSheet by remember { mutableStateOf(false) }
    var selectedPdfSections by remember { mutableStateOf(PdfSection.entries.toSet()) }
    val snackbarHostState = remember { SnackbarHostState() }
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
    val wishlistStr = stringResource(R.string.dashboard_wishlist)
    val giftsCountStr = stringResource(R.string.dashboard_gifts_count)
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
    val cateringStr = stringResource(R.string.dashboard_catering)
    val budgetStr = stringResource(R.string.dashboard_budget)
    val addModuleStr = stringResource(R.string.dashboard_add_module)

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
            eventTheme = eventDoc.getString("theme") ?: ""
            startDate = (eventDoc.get("startDate") as? Number)?.toLong()
            endDate = (eventDoc.get("endDate") as? Number)?.toLong()
            coverImageUrl = eventDoc.getString("coverImageUrl") ?: ""
            eventStatus = eventDoc.getString("status") ?: "PLANNING"
            inviteCode = eventDoc.getString("inviteCode") ?: ""
            @Suppress("UNCHECKED_CAST")
            moduleColors = (eventDoc.get("moduleColors") as? Map<String, String>) ?: emptyMap()

            // Read enabledModules
            val rawModules = eventDoc.get("enabledModules")
            if (rawModules == null) {
                // Field doesn't exist (old event) — show all modules
                enabledModules = EventModule.entries.toList()
            } else {
                val moduleNames = (rawModules as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                enabledModules = moduleNames.mapNotNull {
                    try { EventModule.valueOf(it) } catch (_: Exception) { null }
                }
            }

            // Check if current user is organizer or co-organizer
            isCreator = (eventDoc.getString("createdBy") ?: "") == currentUserId
            isOrganizer = isCreator

            isLoading = false
        } catch (_: Exception) {
            // Firestore failed (e.g. PERMISSION_DENIED) — try loading from local Room DB
            try {
                val localDb = com.bettermingle.app.data.database.AppDatabase.getDatabase(context)
                val localEvent = localDb.eventDao().getEventByIdOnce(eventId)
                if (localEvent != null) {
                    eventName = localEvent.name
                    eventDescription = localEvent.description
                    eventIntroText = localEvent.introText
                    eventLocation = localEvent.locationName
                    eventTheme = localEvent.theme
                    startDate = localEvent.startDate
                    endDate = localEvent.endDate
                    coverImageUrl = localEvent.coverImageUrl
                    eventStatus = localEvent.status.name
                    inviteCode = localEvent.inviteCode
                    enabledModules = localEvent.enabledModules.ifEmpty { EventModule.entries.toList() }
                    moduleColors = localEvent.moduleColors
                    isCreator = localEvent.createdBy == currentUserId
                    isOrganizer = isCreator
                    isLoading = false
                } else {
                    loadError = true
                    isLoading = false
                }
            } catch (_: Exception) {
                loadError = true
                isLoading = false
            }
        }

        coroutineScope {
            val participantsDeferred = async {
                try { eventRef.collection("participants").get().await() } catch (_: Exception) { null }
            }
            val pollsDeferred = async {
                try { eventRef.collection("polls").get().await() } catch (_: Exception) { null }
            }
            val expensesDeferred = async {
                try { eventRef.collection("expenses").get().await() } catch (_: Exception) { null }
            }
            val ridesDeferred = async {
                try { eventRef.collection("carpoolRides").get().await() } catch (_: Exception) { null }
            }
            val roomsDeferred = async {
                try { eventRef.collection("rooms").get().await() } catch (_: Exception) { null }
            }
            val scheduleDeferred = async {
                try { eventRef.collection("schedule").get().await() } catch (_: Exception) { null }
            }
            val messagesDeferred = async {
                try { eventRef.collection("messages").get().await() } catch (_: Exception) { null }
            }
            val tasksDeferred = async {
                try { eventRef.collection("tasks").get().await() } catch (_: Exception) { null }
            }
            val packingDeferred = async {
                try { eventRef.collection("packingItems").get().await() } catch (_: Exception) { null }
            }
            val wishlistDeferred = async {
                try { eventRef.collection("wishlistItems").get().await() } catch (_: Exception) { null }
            }

            participantsDeferred.await()?.let { parts ->
                participantCount = parts.size()
                if (!isOrganizer && currentUserId.isNotEmpty()) {
                    val userParticipant = parts.documents.firstOrNull { it.getString("userId") == currentUserId }
                    val role = userParticipant?.getString("role") ?: ""
                    if (role.equals("CO_ORGANIZER", ignoreCase = true)) {
                        isOrganizer = true
                    }
                }
            }
            pollsDeferred.await()?.let { polls ->
                pollCount = polls.documents.count { !(it.getBoolean("isClosed") ?: false) }
            }
            expensesDeferred.await()?.let { expenses ->
                val total = expenses.documents.sumOf { (it.get("amount") as? Number)?.toDouble() ?: 0.0 }
                expenseTotal = if (total > 0) "${String.format("%,.0f", total)} $currencyCzk" else ""
            }
            ridesDeferred.await()?.let { rideCount = it.size() }
            roomsDeferred.await()?.let { roomCount = it.size() }
            scheduleDeferred.await()?.let { scheduleCount = it.size() }
            messagesDeferred.await()?.let { messageCount = it.size() }
            tasksDeferred.await()?.let { taskCount = it.size() }
            packingDeferred.await()?.let { packingCount = it.size() }
            wishlistDeferred.await()?.let { wishlistCount = it.size() }
        }

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
                    "schedule" to "schedule",
                    "wishlist" to "wishlistItems"
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

    // Delete module handler
    val handleDeleteModule: (String) -> Unit = { moduleKey ->
        val enumVal = MODULE_KEY_TO_ENUM[moduleKey]
        if (enumVal != null) {
            enabledModules = enabledModules.filter { it != enumVal }
            FirebaseFirestore.getInstance().collection("events").document(eventId)
                .update("enabledModules", enabledModules.map { it.name })
        }
    }

    // Add module handler
    val handleAddModule: (EventModule) -> Unit = { module ->
        enabledModules = enabledModules + module
        FirebaseFirestore.getInstance().collection("events").document(eventId)
            .update("enabledModules", enabledModules.map { it.name })
        showAddModuleSheet = false
    }

    // Save module order to Firestore
    val saveModuleOrder: () -> Unit = {
        FirebaseFirestore.getInstance().collection("events").document(eventId)
            .update("enabledModules", enabledModules.map { it.name })
    }

    // Build module info for a given EventModule
    fun buildUserModuleInfo(module: EventModule): ModuleInfo {
        val customColor = moduleColors[module.name]?.let { hexToColor(it) }
        return when (module) {
            EventModule.VOTING -> ModuleInfo("voting", votingStr, if (pollCount > 0) "$pollCount $activeCountStr" else "", Icons.TwoTone.Ballot, customColor ?: PrimaryBlue, badgeCounts["voting"] ?: 0)
            EventModule.EXPENSES -> ModuleInfo("expenses", expensesStr, expenseTotal, Icons.TwoTone.Payments, customColor ?: AccentOrange, badgeCounts["expenses"] ?: 0)
            EventModule.CARPOOL -> ModuleInfo("carpool", carpoolStr, if (rideCount > 0) "$rideCount $ridesCountStr" else "", Icons.TwoTone.DirectionsCar, customColor ?: Success, badgeCounts["carpool"] ?: 0)
            EventModule.ROOMS -> ModuleInfo("rooms", roomsStr, if (roomCount > 0) "$roomCount $roomsCountStr" else "", Icons.TwoTone.Hotel, customColor ?: AccentGold, badgeCounts["rooms"] ?: 0)
            EventModule.CHAT -> ModuleInfo("chat", chatStr, if (messageCount > 0) "$messageCount $messagesCountStr" else "", Icons.TwoTone.ChatBubble, customColor ?: AccentOrange, badgeCounts["chat"] ?: 0)
            EventModule.SCHEDULE -> ModuleInfo("schedule", scheduleStr, if (scheduleCount > 0) "$scheduleCount $itemsCountStr" else "", Icons.TwoTone.CalendarMonth, customColor ?: PrimaryBlue, badgeCounts["schedule"] ?: 0)
            EventModule.TASKS -> ModuleInfo("tasks", tasksStr, if (taskCount > 0) "$taskCount $tasksCountStr" else "", Icons.TwoTone.Checklist, customColor ?: AccentPink, badgeCounts["tasks"] ?: 0)
            EventModule.PACKING_LIST -> ModuleInfo("packing", packingStr, if (packingCount > 0) "$packingCount $thingsCountStr" else "", Icons.TwoTone.Backpack, customColor ?: Success)
            EventModule.WISHLIST -> ModuleInfo("wishlist", wishlistStr, if (wishlistCount > 0) "$wishlistCount $giftsCountStr" else "", Icons.TwoTone.CardGiftcard, customColor ?: AccentPink, badgeCounts["wishlist"] ?: 0)
            EventModule.CATERING -> ModuleInfo("catering", cateringStr, "", Icons.TwoTone.Restaurant, customColor ?: Success)
            EventModule.BUDGET -> ModuleInfo("budget", budgetStr, "", Icons.TwoTone.Payments, customColor ?: AccentGold)
        }
    }

    val settingsColor = MaterialTheme.colorScheme.onSurfaceVariant

    // Build user modules from enabledModules order
    val userModules = remember(enabledModules, pollCount, expenseTotal, rideCount, roomCount, scheduleCount, messageCount, taskCount, packingCount, wishlistCount, badgeCounts.toMap(), moduleColors) {
        enabledModules.map { buildUserModuleInfo(it) }
    }

    // System modules (always visible)
    val systemModules = remember(participantCount, eventStatus, settingsColor) {
        buildList {
            add(ModuleInfo("participants", participantsStr, if (participantCount > 0) "$participantCount $peopleCountStr" else "", Icons.TwoTone.Groups, AccentPink))
            add(ModuleInfo("activity", activityStr, "", Icons.TwoTone.Timeline, PrimaryBlue))
            add(ModuleInfo("rating", ratingStr, "", Icons.TwoTone.StarRate, AccentGold))
            if (eventStatus == "COMPLETED") {
                add(ModuleInfo("summary", summaryStr, "", Icons.TwoTone.Assessment, AccentPink))
            }
            add(ModuleInfo("settings", settingsStr, "", Icons.TwoTone.Settings, settingsColor))
        }
    }

    // Available modules to add
    val availableToAdd = EventModule.entries.filter { it !in enabledModules }

    // Build grid items
    val gridItems = buildList<GridItem> {
        userModules.forEach { add(GridItem.ModuleItem(it, isSystem = false)) }
        systemModules.forEach { add(GridItem.ModuleItem(it, isSystem = true)) }
        if (isOrganizer && availableToAdd.isNotEmpty()) add(GridItem.AddItem)
    }

    // Add module bottom sheet
    if (showAddModuleSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddModuleSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md)
            ) {
                Text(
                    text = addModuleStr,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(Spacing.md))

                val premiumTier = settings?.premiumTier ?: PremiumTier.FREE
                val premiumLockedStr = stringResource(R.string.module_premium_locked)
                val businessLockedStr = stringResource(R.string.module_business_locked)
                availableToAdd.forEach { module ->
                    val info = buildUserModuleInfo(module)
                    val isLocked = !TierLimits.canUseModule(premiumTier, module)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                if (isLocked) onNavigateToUpgrade() else handleAddModule(module)
                            }
                            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        Icon(
                            imageVector = if (isLocked) Icons.Default.Lock else info.icon,
                            contentDescription = null,
                            tint = if (isLocked) AccentGold else info.iconTint,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = info.title,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        if (isLocked) {
                            val isBusiness = module in TierLimits.BUSINESS_MODULES
                            Text(
                                text = if (isBusiness) businessLockedStr else premiumLockedStr,
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentGold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.lg))
            }
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

    // Delete event confirmation dialog
    if (showDeleteEventDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteEventDialog = false },
            title = { Text(stringResource(R.string.event_settings_delete_title)) },
            text = { Text(stringResource(R.string.event_settings_delete_confirm, eventName)) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    scope.launch {
                        try {
                            val firestore = FirebaseFirestore.getInstance()
                            val eventRef = firestore.collection("events").document(eventId)
                            val subcollections = listOf("participants", "polls", "expenses", "carpoolRides", "rooms", "schedule", "messages")
                            for (sub in subcollections) {
                                val docs = eventRef.collection(sub).get().await()
                                for (doc in docs.documents) {
                                    doc.reference.delete().await()
                                }
                            }
                            eventRef.delete().await()
                            showDeleteEventDialog = false
                            if (onDeleteEvent != null) {
                                onDeleteEvent()
                            } else {
                                onNavigateBack()
                            }
                        } catch (_: Exception) { }
                    }
                }) {
                    Text(stringResource(R.string.common_delete), color = AccentOrange)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showDeleteEventDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    if (showExportUpgradeDialog) {
        AlertDialog(
            onDismissRequest = { showExportUpgradeDialog = false },
            title = { Text(stringResource(R.string.export_pdf_upgrade_title)) },
            text = { Text(stringResource(R.string.export_pdf_upgrade_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showExportUpgradeDialog = false
                    onNavigateToUpgrade()
                }) { Text(stringResource(R.string.tier_upgrade_button)) }
            },
            dismissButton = {
                TextButton(onClick = { showExportUpgradeDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    // PDF section picker bottom sheet
    if (showPdfSectionSheet) {
        val availableSections = remember(enabledModules) {
            val available = mutableSetOf(PdfSection.PARTICIPANTS)
            for (m in enabledModules) {
                when (m) {
                    EventModule.VOTING -> available.add(PdfSection.POLLS)
                    EventModule.EXPENSES -> available.add(PdfSection.EXPENSES)
                    EventModule.CARPOOL -> available.add(PdfSection.CARPOOL)
                    EventModule.TASKS -> available.add(PdfSection.TASKS)
                    EventModule.PACKING_LIST -> available.add(PdfSection.PACKING)
                    EventModule.WISHLIST -> available.add(PdfSection.WISHLIST)
                    EventModule.BUDGET -> available.add(PdfSection.BUDGET)
                    else -> { }
                }
            }
            available.toList().sortedBy { it.ordinal }
        }

        fun sectionLabel(section: PdfSection): Int = when (section) {
            PdfSection.PARTICIPANTS -> R.string.export_pdf_section_participants
            PdfSection.POLLS -> R.string.export_pdf_section_polls
            PdfSection.BUDGET -> R.string.export_pdf_section_budget
            PdfSection.EXPENSES -> R.string.export_pdf_section_expenses
            PdfSection.WISHLIST -> R.string.export_pdf_section_wishlist
            PdfSection.TASKS -> R.string.export_pdf_section_tasks
            PdfSection.PACKING -> R.string.export_pdf_section_packing
            PdfSection.CARPOOL -> R.string.export_pdf_section_carpool
        }

        fun sectionIcon(section: PdfSection): ImageVector = when (section) {
            PdfSection.PARTICIPANTS -> Icons.TwoTone.Groups
            PdfSection.POLLS -> Icons.TwoTone.Ballot
            PdfSection.BUDGET -> Icons.TwoTone.Assessment
            PdfSection.EXPENSES -> Icons.TwoTone.Payments
            PdfSection.WISHLIST -> Icons.TwoTone.CardGiftcard
            PdfSection.TASKS -> Icons.TwoTone.Checklist
            PdfSection.PACKING -> Icons.TwoTone.Backpack
            PdfSection.CARPOOL -> Icons.TwoTone.DirectionsCar
        }

        fun sharePdfFile(file: File) {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, context.getString(R.string.export_pdf_chooser)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }

        ModalBottomSheet(
            onDismissRequest = { showPdfSectionSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md)
            ) {
                Text(
                    text = stringResource(R.string.export_pdf_select_sections),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(Spacing.md))

                for (section in availableSections) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = sectionIcon(section),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = PrimaryBlue
                        )
                        Spacer(modifier = Modifier.size(Spacing.sm))
                        Text(
                            text = stringResource(sectionLabel(section)),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = section in selectedPdfSections,
                            onCheckedChange = { checked ->
                                selectedPdfSections = if (checked) {
                                    selectedPdfSections + section
                                } else {
                                    selectedPdfSections - section
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.lg))

                BetterMingleButton(
                    text = stringResource(R.string.export_pdf_export_button),
                    onClick = {
                        showPdfSectionSheet = false
                        isExportingPdf = true
                        scope.launch {
                            try {
                                val report = withContext(Dispatchers.IO) {
                                    loadDetailedEventReport(eventId)
                                }
                                val file = withContext(Dispatchers.IO) {
                                    EventPdfGenerator(context).generateDetailed(report, selectedPdfSections)
                                }
                                sharePdfFile(file)
                            } catch (_: Exception) {
                                snackbarHostState.showSnackbar(context.getString(R.string.export_pdf_error))
                            } finally {
                                isExportingPdf = false
                            }
                        }
                    },
                    enabled = selectedPdfSections.isNotEmpty(),
                    isCta = true
                )

                Spacer(modifier = Modifier.height(Spacing.lg))
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    // Export PDF button (all participants)
                    IconButton(
                        onClick = {
                            val tier = settings?.premiumTier ?: PremiumTier.FREE
                            if (tier == PremiumTier.FREE) {
                                showExportUpgradeDialog = true
                            } else {
                                // Build available sections based on enabled modules
                                val available = mutableSetOf(PdfSection.PARTICIPANTS)
                                for (m in enabledModules) {
                                    when (m) {
                                        EventModule.VOTING -> available.add(PdfSection.POLLS)
                                        EventModule.EXPENSES -> available.add(PdfSection.EXPENSES)
                                        EventModule.CARPOOL -> available.add(PdfSection.CARPOOL)
                                        EventModule.TASKS -> available.add(PdfSection.TASKS)
                                        EventModule.PACKING_LIST -> available.add(PdfSection.PACKING)
                                        EventModule.WISHLIST -> available.add(PdfSection.WISHLIST)
                                        EventModule.BUDGET -> available.add(PdfSection.BUDGET)
                                        else -> { }
                                    }
                                }
                                selectedPdfSections = available
                                showPdfSectionSheet = true
                            }
                        },
                        enabled = !isExportingPdf
                    ) {
                        if (isExportingPdf) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Description, contentDescription = stringResource(R.string.export_pdf_button))
                        }
                    }
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
                    if (isOrganizer) {
                        Box {
                            IconButton(onClick = { showOverflowMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false }
                            ) {
                                if (inviteCode.isNotEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.common_share)) },
                                        onClick = {
                                            showOverflowMenu = false
                                            showShareSheet = true
                                        }
                                    )
                                }
                                if (onDuplicateEvent != null) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.event_settings_repeat)) },
                                        onClick = {
                                            showOverflowMenu = false
                                            onDuplicateEvent(
                                                eventName,
                                                eventDescription,
                                                eventLocation,
                                                eventIntroText,
                                                eventTheme,
                                                enabledModules.map { it.name }
                                            )
                                        }
                                    )
                                }
                                if (isCreator) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.event_settings_delete_action), color = AccentOrange) },
                                        onClick = {
                                            showOverflowMenu = false
                                            showDeleteEventDialog = true
                                        }
                                    )
                                }
                            }
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
                .verticalScroll(rememberScrollState())
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
                    .fillMaxWidth()
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

            CoachMarkBanner(
                id = CoachMarkIds.DASHBOARD_MODULES,
                message = stringResource(R.string.coach_mark_dashboard),
                modifier = Modifier.padding(horizontal = Spacing.xs, vertical = Spacing.xs)
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Non-lazy grid with drag-and-drop support
            val chunked = gridItems.chunked(3)
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier = Modifier.fillMaxWidth()
            ) {
                chunked.forEachIndexed { rowIdx, row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        row.forEachIndexed { colIdx, item ->
                            val flatIndex = rowIdx * 3 + colIdx

                            when (item) {
                                is GridItem.ModuleItem -> {
                                    val module = item.info
                                    val isDragged = draggedKey == module.key
                                    val isUserModule = !item.isSystem
                                    val canDrag = isOrganizer && isUserModule

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .then(
                                                if (isDragged) Modifier.zIndex(1f) else Modifier
                                            )
                                            .onGloballyPositioned { coords ->
                                                val pos = coords.positionInRoot()
                                                val size = coords.size
                                                cardCenters[module.key] = Offset(
                                                    pos.x + size.width / 2f,
                                                    pos.y + size.height / 2f
                                                )
                                            }
                                            .then(
                                                if (canDrag) {
                                                    Modifier.pointerInput(module.key) {
                                                        detectDragGesturesAfterLongPress(
                                                            onDragStart = {
                                                                draggedKey = module.key
                                                                dragOffset = Offset.Zero
                                                            },
                                                            onDrag = { change, dragAmount ->
                                                                change.consume()
                                                                dragOffset += dragAmount
                                                            },
                                                            onDragEnd = {
                                                                val draggedCenter = cardCenters[draggedKey]
                                                                if (draggedCenter != null && draggedKey != null) {
                                                                    val dropPos = draggedCenter + dragOffset
                                                                    val target = cardCenters.entries
                                                                        .filter { it.key != draggedKey && it.key !in SYSTEM_MODULE_KEYS }
                                                                        .minByOrNull { (dropPos - it.value).getDistance() }

                                                                    if (target != null) {
                                                                        val fromEnum = MODULE_KEY_TO_ENUM[draggedKey]
                                                                        val toEnum = MODULE_KEY_TO_ENUM[target.key]
                                                                        if (fromEnum != null && toEnum != null) {
                                                                            val newList = enabledModules.toMutableList()
                                                                            val fromIdx = newList.indexOf(fromEnum)
                                                                            val toIdx = newList.indexOf(toEnum)
                                                                            if (fromIdx >= 0 && toIdx >= 0) {
                                                                                Collections.swap(newList, fromIdx, toIdx)
                                                                                enabledModules = newList
                                                                                saveModuleOrder()
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                                draggedKey = null
                                                                dragOffset = Offset.Zero
                                                            },
                                                            onDragCancel = {
                                                                draggedKey = null
                                                                dragOffset = Offset.Zero
                                                            }
                                                        )
                                                    }
                                                } else Modifier
                                            )
                                            .then(
                                                if (isDragged) {
                                                    Modifier.graphicsLayer {
                                                        translationX = dragOffset.x
                                                        translationY = dragOffset.y
                                                    }
                                                } else Modifier
                                            )
                                    ) {
                                        androidx.compose.animation.AnimatedVisibility(
                                            visible = visible,
                                            enter = fadeIn(tween(BetterMingleMotion.STANDARD, delayMillis = 100 + flatIndex * 40)) +
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
                                                onClick = { handleModuleClick(module.key) },
                                                enablePressAnimation = !canDrag
                                            )
                                        }
                                    }
                                }

                                is GridItem.AddItem -> {
                                    Box(modifier = Modifier.weight(1f)) {
                                        androidx.compose.animation.AnimatedVisibility(
                                            visible = visible,
                                            enter = fadeIn(tween(BetterMingleMotion.STANDARD, delayMillis = 100 + flatIndex * 40)) +
                                                    scaleIn(
                                                        spring(
                                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                                            stiffness = Spring.StiffnessMedium
                                                        ),
                                                        initialScale = 0.7f
                                                    )
                                        ) {
                                            AddModuleCard(
                                                label = addModuleStr,
                                                onClick = { showAddModuleSheet = true }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        // Fill remaining slots in the row
                        repeat(3 - row.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))
            }
        }
        }
    }
}

@Composable
private fun AddModuleCard(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = BetterMingleThemeColors.extended

    Column(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 120.dp)
            .clip(RoundedCornerShape(com.bettermingle.app.ui.theme.CornerRadius.card))
            .background(ext.pastelGray)
            .clickable(onClick = onClick)
            .padding(Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
