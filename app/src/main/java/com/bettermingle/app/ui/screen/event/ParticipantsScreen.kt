package com.bettermingle.app.ui.screen.event

import com.bettermingle.app.R
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
import android.content.Intent
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bettermingle.app.data.model.Participant
import com.bettermingle.app.data.model.ParticipantRole
import com.bettermingle.app.data.model.RsvpStatus
import com.bettermingle.app.data.model.UserProfile
import com.bettermingle.app.ui.component.BetterMingleCard
import com.bettermingle.app.ui.component.EmptyState
import com.bettermingle.app.ui.component.UserAvatar
import com.bettermingle.app.ui.theme.AccentGold
import com.bettermingle.app.ui.theme.AccentOrange
import com.bettermingle.app.ui.theme.AccentPink
import com.bettermingle.app.ui.theme.PrimaryBlue
import com.bettermingle.app.ui.theme.Spacing
import com.bettermingle.app.ui.theme.Success
import com.bettermingle.app.ui.theme.TextOnColor

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import com.bettermingle.app.data.ads.AdManager
import com.bettermingle.app.data.preferences.PremiumTier
import com.bettermingle.app.data.preferences.SettingsManager
import com.bettermingle.app.data.preferences.TierLimits
import com.bettermingle.app.ui.component.BetterMingleTextField
import com.bettermingle.app.ui.component.NativeAdCard
import com.bettermingle.app.utils.ActivityLogger
import com.bettermingle.app.utils.ManualParticipantLinker
import com.bettermingle.app.utils.ParticipantUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.bettermingle.app.utils.safeDocuments

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParticipantsScreen(
    eventId: String,
    onNavigateBack: () -> Unit
) {
    val participants = remember { mutableStateListOf<Participant>() }
    var inviteCode by remember { mutableStateOf("") }
    var eventName by remember { mutableStateOf("") }
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val settings by settingsManager.settingsFlow.collectAsState(initial = null)
    val showAds = settings?.let { AdManager.hasAds(it.premiumTier) } ?: false

    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    var isOrganizer by remember { mutableStateOf(false) }
    val premiumTier = settings?.premiumTier ?: PremiumTier.FREE

    // Bottom sheet state for participant profile
    var selectedProfile by remember { mutableStateOf<UserProfile?>(null) }
    var selectedCustomRole by remember { mutableStateOf("") }
    var isLoadingProfile by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isRefreshing by remember { mutableStateOf(false) }

    // Role management dialog
    var roleDialogParticipant by remember { mutableStateOf<Participant?>(null) }
    var showTierLimitDialog by remember { mutableStateOf(false) }

    // Participant management dialogs
    var showAddOptionsSheet by remember { mutableStateOf(false) }
    var showAddManualDialog by remember { mutableStateOf(false) }
    var showLinkDialog by remember { mutableStateOf<Participant?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Participant?>(null) }
    var showRsvpDialog by remember { mutableStateOf<Participant?>(null) }
    var showEditDialog by remember { mutableStateOf<Participant?>(null) }

    fun loadParticipants() {
        scope.launch {
            try {
                val eventDoc = FirebaseFirestore.getInstance()
                    .collection("events").document(eventId).get().await()
                inviteCode = eventDoc.getString("inviteCode") ?: ""
                eventName = eventDoc.getString("name") ?: ""
                val createdBy = eventDoc.getString("createdBy") ?: ""
                isOrganizer = createdBy == currentUserId
            } catch (e: Exception) {
                Log.e("ParticipantsScreen", "Failed to load event", e)
            }
            try {
                val snapshot = FirebaseFirestore.getInstance()
                    .collection("events").document(eventId)
                    .collection("participants")
                    .get().await()

                val userIds = snapshot.safeDocuments.mapNotNull { it.getString("userId") }.distinct()
                val userNames = mutableMapOf<String, String>()
                for (uid in userIds) {
                    if (ParticipantUtils.isManualId(uid)) continue
                    try {
                        val userDoc = FirebaseFirestore.getInstance()
                            .collection("users").document(uid).get().await()
                        userNames[uid] = userDoc.getString("displayName") ?: ""
                    } catch (_: Exception) { }
                }

                val loaded = snapshot.safeDocuments.map { doc ->
                    val data = doc.data ?: emptyMap()
                    val userId = data["userId"] as? String ?: ""
                    val role = try {
                        ParticipantRole.valueOf((data["role"] as? String ?: "PARTICIPANT").uppercase())
                    } catch (_: Exception) { ParticipantRole.PARTICIPANT }
                    val rsvp = try {
                        RsvpStatus.valueOf((data["rsvp"] as? String ?: "PENDING").uppercase())
                    } catch (_: Exception) { RsvpStatus.PENDING }

                    val isManual = data["isManual"] as? Boolean ?: ParticipantUtils.isManualId(userId)
                    Participant(
                        id = doc.id,
                        eventId = eventId,
                        userId = userId,
                        displayName = (data["displayName"] as? String)?.ifEmpty { null }
                            ?: userNames[userId] ?: userId.take(8),
                        avatarUrl = data["avatarUrl"] as? String ?: "",
                        role = role,
                        customRole = data["customRole"] as? String ?: "",
                        rsvp = rsvp,
                        joinedAt = (data["joinedAt"] as? Number)?.toLong() ?: 0,
                        isManual = isManual,
                        linkedUserId = data["linkedUserId"] as? String
                    )
                }.sortedBy { when (it.role) {
                    ParticipantRole.ORGANIZER -> 0
                    ParticipantRole.CO_ORGANIZER -> 1
                    ParticipantRole.PARTICIPANT -> 2
                } }

                participants.clear()
                participants.addAll(loaded)
            } catch (_: Exception) {
                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.error_load_failed)) }
            }
        }
    }

    LaunchedEffect(eventId) { loadParticipants() }

    // Tier limit dialog for co-organizer feature
    if (showTierLimitDialog) {
        AlertDialog(
            onDismissRequest = { showTierLimitDialog = false },
            title = { Text(stringResource(R.string.participants_coorg_limit_title)) },
            text = { Text(stringResource(R.string.participants_coorg_limit_message)) },
            confirmButton = {
                TextButton(onClick = { showTierLimitDialog = false }) {
                    Text(stringResource(R.string.common_close))
                }
            }
        )
    }

    // Role management dialog
    roleDialogParticipant?.let { participant ->
        val isCoOrganizer = participant.role == ParticipantRole.CO_ORGANIZER
        AlertDialog(
            onDismissRequest = { roleDialogParticipant = null },
            title = { Text(participant.displayName) },
            text = {
                Text(
                    if (isCoOrganizer) stringResource(R.string.participants_demote_message)
                    else stringResource(R.string.participants_promote_message)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val targetParticipant = participant
                    roleDialogParticipant = null
                    scope.launch {
                        try {
                            val newRole = if (isCoOrganizer) ParticipantRole.PARTICIPANT else ParticipantRole.CO_ORGANIZER
                            FirebaseFirestore.getInstance()
                                .collection("events").document(eventId)
                                .collection("participants").document(targetParticipant.id)
                                .update("role", newRole.name).await()
                            loadParticipants()
                            snackbarHostState.showSnackbar(context.getString(R.string.participants_role_updated))
                        } catch (e: Exception) {
                            Log.e("ParticipantsScreen", "Failed to update role", e)
                            snackbarHostState.showSnackbar(context.getString(R.string.participants_role_update_error))
                        }
                    }
                }) {
                    Text(stringResource(R.string.common_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { roleDialogParticipant = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    // Add options bottom sheet (for organizer FAB)
    if (showAddOptionsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddOptionsSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md)
            ) {
                Text(
                    text = stringResource(R.string.participants_add_options_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(Spacing.md))
                ListItem(
                    headlineContent = { Text(stringResource(R.string.participants_share_invite)) },
                    leadingContent = { Icon(Icons.Default.Share, contentDescription = null, tint = PrimaryBlue) },
                    modifier = Modifier.clickable {
                        showAddOptionsSheet = false
                        val shareText = context.getString(R.string.participants_share_text, eventName, "https://bettermingle.app/invite/$inviteCode")
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(intent, context.getString(R.string.participants_share_title)))
                    }
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.participants_add_manual)) },
                    leadingContent = { Icon(Icons.Default.PersonAdd, contentDescription = null, tint = AccentOrange) },
                    modifier = Modifier.clickable {
                        showAddOptionsSheet = false
                        showAddManualDialog = true
                    }
                )
                Spacer(modifier = Modifier.height(Spacing.lg))
            }
        }
    }

    // Add manual participant dialog
    if (showAddManualDialog) {
        AddManualParticipantDialog(
            eventId = eventId,
            currentParticipantCount = participants.size,
            premiumTier = premiumTier,
            onDismiss = { showAddManualDialog = false },
            onCreated = {
                showAddManualDialog = false
                loadParticipants()
            }
        )
    }

    // Delete participant dialog
    showDeleteDialog?.let { participant ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = {
                Text(
                    if (participant.isManual) stringResource(R.string.participants_delete_manual)
                    else stringResource(R.string.participants_delete_participant)
                )
            },
            text = {
                Text(
                    if (participant.isManual) stringResource(R.string.participants_delete_manual_confirm, participant.displayName)
                    else stringResource(R.string.participants_delete_participant_confirm, participant.displayName)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val p = participant
                    showDeleteDialog = null
                    scope.launch {
                        try {
                            FirebaseFirestore.getInstance()
                                .collection("events").document(eventId)
                                .collection("participants").document(p.id)
                                .delete().await()
                            ActivityLogger.log(eventId, "participant", context.getString(R.string.activity_removed_participant, p.displayName))
                            loadParticipants()
                            snackbarHostState.showSnackbar(context.getString(R.string.participants_removed_success))
                        } catch (e: Exception) {
                            Log.e("ParticipantsScreen", "Failed to delete participant", e)
                            snackbarHostState.showSnackbar(context.getString(R.string.participants_removed_error))
                        }
                    }
                }) {
                    Text(stringResource(R.string.common_confirm), color = AccentOrange)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    // RSVP change dialog for manual participants
    showRsvpDialog?.let { participant ->
        AlertDialog(
            onDismissRequest = { showRsvpDialog = null },
            title = { Text(stringResource(R.string.participants_change_rsvp_title, participant.displayName)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    listOf(
                        RsvpStatus.ACCEPTED to stringResource(R.string.participants_rsvp_accepted),
                        RsvpStatus.DECLINED to stringResource(R.string.participants_rsvp_declined),
                        RsvpStatus.MAYBE to stringResource(R.string.participants_rsvp_maybe),
                        RsvpStatus.PENDING to stringResource(R.string.participants_rsvp_pending)
                    ).forEach { (status, label) ->
                        ListItem(
                            headlineContent = { Text(label, fontWeight = if (participant.rsvp == status) FontWeight.Bold else FontWeight.Normal) },
                            leadingContent = {
                                RsvpBadge(status = status)
                            },
                            modifier = Modifier.clickable {
                                val p = participant
                                showRsvpDialog = null
                                scope.launch {
                                    try {
                                        FirebaseFirestore.getInstance()
                                            .collection("events").document(eventId)
                                            .collection("participants").document(p.id)
                                            .update("rsvp", status.name).await()
                                        loadParticipants()
                                    } catch (e: Exception) {
                                        Log.e("ParticipantsScreen", "Failed to update RSVP", e)
                                    }
                                }
                            }
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showRsvpDialog = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    // Edit participant dialog
    showEditDialog?.let { participant ->
        EditParticipantDialog(
            eventId = eventId,
            participant = participant,
            onDismiss = { showEditDialog = null },
            onSaved = {
                showEditDialog = null
                loadParticipants()
                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.participants_edit_saved)) }
            }
        )
    }

    // Link manual participant dialog
    showLinkDialog?.let { manualParticipant ->
        val realParticipants = participants.filter { !it.isManual && it.role != ParticipantRole.ORGANIZER && it.userId != currentUserId }
        AlertDialog(
            onDismissRequest = { showLinkDialog = null },
            title = { Text(stringResource(R.string.participants_link_title)) },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.participants_link_description),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    if (realParticipants.isEmpty()) {
                        Text(
                            text = stringResource(R.string.participants_empty_title),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        realParticipants.forEach { rp ->
                            ListItem(
                                headlineContent = { Text(rp.displayName) },
                                leadingContent = {
                                    UserAvatar(
                                        avatarUrl = rp.avatarUrl,
                                        displayName = rp.displayName,
                                        size = 32.dp
                                    )
                                },
                                modifier = Modifier.clickable {
                                    val mp = manualParticipant
                                    showLinkDialog = null
                                    scope.launch {
                                        val result = ManualParticipantLinker.linkManualParticipant(
                                            eventId = eventId,
                                            manualParticipantId = mp.id,
                                            realUserId = rp.userId
                                        )
                                        if (result.isSuccess) {
                                            ActivityLogger.log(eventId, "participant", context.getString(R.string.activity_linked_participant, mp.displayName, rp.displayName))
                                            snackbarHostState.showSnackbar(context.getString(R.string.participants_link_success))
                                            loadParticipants()
                                        } else {
                                            snackbarHostState.showSnackbar(context.getString(R.string.participants_link_error))
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLinkDialog = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    // Profile bottom sheet
    if (selectedProfile != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedProfile = null },
            sheetState = sheetState
        ) {
            ParticipantProfileSheet(
                profile = selectedProfile!!,
                customRole = selectedCustomRole
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.participants_title), style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val shareText = context.getString(R.string.participants_share_text, eventName, "https://bettermingle.app/invite/$inviteCode")
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(intent, context.getString(R.string.participants_share_title)))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = stringResource(R.string.participants_share_title))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (isOrganizer) {
                        showAddOptionsSheet = true
                    } else {
                        val shareText = context.getString(R.string.participants_share_text, eventName, "https://bettermingle.app/invite/$inviteCode")
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(intent, context.getString(R.string.participants_invite_title)))
                    }
                },
                containerColor = Color.Transparent,
                contentColor = TextOnColor,
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                modifier = Modifier
                    .shadow(8.dp, CircleShape, ambientColor = AccentOrange.copy(alpha = 0.3f), spotColor = AccentOrange.copy(alpha = 0.3f))
                    .background(AccentOrange, CircleShape)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = stringResource(R.string.participants_add))
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                loadParticipants()
                scope.launch {
                    kotlinx.coroutines.delay(500)
                    isRefreshing = false
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
        if (participants.isEmpty() && !isRefreshing) {
            EmptyState(
                icon = Icons.Default.People,
                illustration = R.drawable.il_empty_participants,
                title = stringResource(R.string.participants_empty_title),
                description = stringResource(R.string.participants_empty_description),
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(Spacing.screenPadding),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                items(participants, key = { it.id }) { participant ->
                    ParticipantItem(
                        participant = participant,
                        showRoleAction = isOrganizer && participant.role != ParticipantRole.ORGANIZER,
                        showLinkAction = isOrganizer && participant.isManual,
                        showRsvpAction = isOrganizer && participant.role != ParticipantRole.ORGANIZER,
                        showDeleteAction = isOrganizer && participant.role != ParticipantRole.ORGANIZER,
                        showEditAction = isOrganizer && participant.role != ParticipantRole.ORGANIZER,
                        onRoleClick = {
                            if (!TierLimits.canAddCoOrganizers(premiumTier)) {
                                showTierLimitDialog = true
                            } else {
                                roleDialogParticipant = participant
                            }
                        },
                        onLinkClick = { showLinkDialog = participant },
                        onRsvpClick = { showRsvpDialog = participant },
                        onDeleteClick = { showDeleteDialog = participant },
                        onEditClick = { showEditDialog = participant },
                        onClick = {
                            if (participant.isManual) {
                                // Show simplified profile for manual participants
                                selectedCustomRole = participant.customRole
                                selectedProfile = UserProfile(
                                    id = participant.userId,
                                    displayName = participant.displayName,
                                    email = "",
                                    avatarUrl = "",
                                    phone = "",
                                    contactEmail = "",
                                    department = "",
                                    bio = context.getString(R.string.participants_manual_no_profile),
                                    dietaryPreferences = emptyList()
                                )
                            } else if (participant.userId.isNotEmpty()) {
                                isLoadingProfile = true
                                selectedCustomRole = participant.customRole
                                scope.launch {
                                    try {
                                        val doc = FirebaseFirestore.getInstance()
                                            .collection("users").document(participant.userId)
                                            .get().await()
                                        if (doc.exists()) {
                                            selectedProfile = UserProfile(
                                                id = participant.userId,
                                                displayName = doc.getString("displayName") ?: participant.displayName,
                                                email = doc.getString("email") ?: "",
                                                avatarUrl = doc.getString("avatarUrl") ?: "",
                                                phone = doc.getString("phone") ?: "",
                                                contactEmail = doc.getString("contactEmail") ?: "",
                                                department = doc.getString("department") ?: "",
                                                bio = doc.getString("bio") ?: "",
                                                dietaryPreferences = (doc.get("dietaryPreferences") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                                            )
                                        }
                                    } catch (_: Exception) { }
                                    isLoadingProfile = false
                                }
                            }
                        }
                    )
                }

                if (showAds && participants.isNotEmpty()) {
                    item(key = "native_ad") {
                        NativeAdCard(
                            modifier = Modifier.padding(vertical = Spacing.sm)
                        )
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun ParticipantItem(
    participant: Participant,
    showRoleAction: Boolean = false,
    showLinkAction: Boolean = false,
    showRsvpAction: Boolean = false,
    showDeleteAction: Boolean = false,
    showEditAction: Boolean = false,
    onRoleClick: () -> Unit = {},
    onLinkClick: () -> Unit = {},
    onRsvpClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onEditClick: () -> Unit = {},
    onClick: () -> Unit
) {
    BetterMingleCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (participant.isManual) {
                // Guest avatar placeholder
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(AccentOrange.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PersonOutline,
                        contentDescription = null,
                        tint = AccentOrange,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                UserAvatar(
                    avatarUrl = participant.avatarUrl,
                    displayName = participant.displayName,
                    size = 40.dp
                )
            }

            Spacer(modifier = Modifier.width(Spacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = participant.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Row {
                    Text(
                        text = when (participant.role) {
                            ParticipantRole.ORGANIZER -> stringResource(R.string.participants_role_organizer)
                            ParticipantRole.CO_ORGANIZER -> stringResource(R.string.participants_role_co_organizer)
                            ParticipantRole.PARTICIPANT -> stringResource(R.string.participants_role_participant)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (participant.role == ParticipantRole.CO_ORGANIZER) AccentGold else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (participant.customRole.isNotEmpty()) {
                        Text(
                            text = " · ${participant.customRole}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AccentPink,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (showLinkAction) {
                IconButton(onClick = onLinkClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = stringResource(R.string.participants_link_account),
                        tint = PrimaryBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (showEditAction) {
                IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.participants_edit_title),
                        tint = PrimaryBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (showRoleAction) {
                IconButton(onClick = onRoleClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = if (participant.role == ParticipantRole.CO_ORGANIZER)
                            Icons.Default.Person else Icons.Default.PersonAdd,
                        contentDescription = stringResource(R.string.participants_manage_role),
                        tint = AccentGold,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (showDeleteAction) {
                IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.participants_delete_participant),
                        tint = AccentOrange,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (participant.isManual) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    GuestBadge()
                    if (showRsvpAction) {
                        Box(modifier = Modifier.clickable { onRsvpClick() }) {
                            RsvpBadge(status = participant.rsvp)
                        }
                    } else {
                        RsvpBadge(status = participant.rsvp)
                    }
                }
            } else {
                if (showRsvpAction) {
                    Box(modifier = Modifier.clickable { onRsvpClick() }) {
                        RsvpBadge(status = participant.rsvp)
                    }
                } else {
                    RsvpBadge(status = participant.rsvp)
                }
            }
        }
    }
}

@Composable
private fun ParticipantProfileSheet(
    profile: UserProfile,
    customRole: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar
        UserAvatar(
            avatarUrl = profile.avatarUrl,
            displayName = profile.displayName,
            size = 80.dp
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        Text(
            text = profile.displayName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        if (customRole.isNotEmpty()) {
            Text(
                text = customRole,
                style = MaterialTheme.typography.bodyMedium,
                color = AccentPink
            )
        }

        if (profile.bio.isNotEmpty()) {
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = profile.bio,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        @OptIn(ExperimentalLayoutApi::class)
        if (profile.dietaryPreferences.isNotEmpty()) {
            Spacer(modifier = Modifier.height(Spacing.sm))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                modifier = Modifier.fillMaxWidth()
            ) {
                profile.dietaryPreferences.forEach { pref ->
                    Box(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.small)
                            .background(Success.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = pref,
                            style = MaterialTheme.typography.labelSmall,
                            color = Success
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        // Contact info
        val hasInfo = profile.contactEmail.isNotEmpty() || profile.phone.isNotEmpty()
                || profile.department.isNotEmpty() || profile.email.isNotEmpty()

        if (hasInfo) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                if (profile.email.isNotEmpty()) {
                    ProfileInfoRow(Icons.Default.Email, stringResource(R.string.participants_email_label), profile.email)
                }
                if (profile.contactEmail.isNotEmpty()) {
                    ProfileInfoRow(Icons.Default.Email, stringResource(R.string.participants_contact_email_label), profile.contactEmail)
                }
                if (profile.phone.isNotEmpty()) {
                    ProfileInfoRow(Icons.Default.Phone, stringResource(R.string.participants_phone_label), profile.phone)
                }
                if (profile.department.isNotEmpty()) {
                    ProfileInfoRow(Icons.Default.Business, stringResource(R.string.participants_department_label), profile.department)
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.lg))
    }
}

@Composable
private fun ProfileInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PrimaryBlue,
            modifier = Modifier.size(Spacing.iconSM)
        )
        Spacer(modifier = Modifier.width(Spacing.sm))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun RsvpBadge(status: RsvpStatus) {
    val (text, color) = when (status) {
        RsvpStatus.ACCEPTED -> stringResource(R.string.participants_rsvp_accepted) to Success
        RsvpStatus.DECLINED -> stringResource(R.string.participants_rsvp_declined) to AccentOrange
        RsvpStatus.MAYBE -> stringResource(R.string.participants_rsvp_maybe) to AccentGold
        RsvpStatus.PENDING -> stringResource(R.string.participants_rsvp_pending) to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
private fun GuestBadge() {
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(AccentOrange.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = stringResource(R.string.participants_manual_badge),
            style = MaterialTheme.typography.labelSmall,
            color = AccentOrange
        )
    }
}

@Composable
private fun AddManualParticipantDialog(
    eventId: String,
    currentParticipantCount: Int,
    premiumTier: PremiumTier,
    onDismiss: () -> Unit,
    onCreated: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var customRole by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.participants_add_manual_title)) },
        text = {
            Column {
                BetterMingleTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(R.string.participants_add_manual_name_label)
                )
                Spacer(modifier = Modifier.height(Spacing.formFieldSpacing))
                BetterMingleTextField(
                    value = customRole,
                    onValueChange = { customRole = it },
                    label = stringResource(R.string.participants_add_manual_role_label)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    scope.launch {
                        try {
                            val maxParticipants = TierLimits.maxParticipants(premiumTier)
                            if (currentParticipantCount >= maxParticipants) {
                                return@launch
                            }
                            val manualId = ParticipantUtils.generateManualId()
                            val data = hashMapOf(
                                "userId" to manualId,
                                "displayName" to name.trim(),
                                "avatarUrl" to "",
                                "role" to ParticipantRole.PARTICIPANT.name,
                                "customRole" to customRole.trim(),
                                "rsvp" to RsvpStatus.ACCEPTED.name,
                                "isManual" to true,
                                "linkedUserId" to null,
                                "joinedAt" to System.currentTimeMillis()
                            )
                            FirebaseFirestore.getInstance()
                                .collection("events").document(eventId)
                                .collection("participants").document(manualId)
                                .set(data)
                                .await()
                            ActivityLogger.log(eventId, "participant", context.getString(R.string.activity_added_manual_participant, name.trim()))
                            onCreated()
                        } catch (e: Exception) {
                            Log.e("ParticipantsScreen", "Failed to add manual participant", e)
                        }
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(R.string.participants_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}

@Composable
private fun EditParticipantDialog(
    eventId: String,
    participant: Participant,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    var displayName by remember { mutableStateOf(participant.displayName) }
    var customRole by remember { mutableStateOf(participant.customRole) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.participants_edit_title)) },
        text = {
            Column {
                BetterMingleTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = stringResource(R.string.participants_edit_name_label),
                    enabled = participant.isManual
                )
                Spacer(modifier = Modifier.height(Spacing.formFieldSpacing))
                BetterMingleTextField(
                    value = customRole,
                    onValueChange = { customRole = it },
                    label = stringResource(R.string.participants_edit_role_label)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    scope.launch {
                        try {
                            val updates = mutableMapOf<String, Any>(
                                "customRole" to customRole.trim()
                            )
                            if (participant.isManual) {
                                updates["displayName"] = displayName.trim()
                            }
                            FirebaseFirestore.getInstance()
                                .collection("events").document(eventId)
                                .collection("participants").document(participant.id)
                                .update(updates).await()
                            onSaved()
                        } catch (e: Exception) {
                            Log.e("ParticipantsScreen", "Failed to edit participant", e)
                        }
                    }
                },
                enabled = if (participant.isManual) displayName.isNotBlank() else true
            ) {
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}
