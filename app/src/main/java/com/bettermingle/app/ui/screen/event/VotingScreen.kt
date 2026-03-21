package com.bettermingle.app.ui.screen.event

import com.bettermingle.app.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bettermingle.app.utils.performHapticClick
import com.bettermingle.app.ui.component.BetterMingleTextField
import com.bettermingle.app.data.model.Poll
import com.bettermingle.app.data.model.PollOption
import com.bettermingle.app.data.model.PollType
import com.bettermingle.app.ui.component.BetterMingleCard
import com.bettermingle.app.ui.component.EmptyState
import com.bettermingle.app.ui.component.ModuleColorPickerDialog
import com.bettermingle.app.data.repository.EventRepository
import androidx.compose.material.icons.filled.Palette
import com.bettermingle.app.ui.theme.AccentGold
import com.bettermingle.app.ui.theme.AccentOrange
import com.bettermingle.app.ui.theme.AccentPink
import com.bettermingle.app.ui.theme.PrimaryBlue
import com.bettermingle.app.ui.theme.Spacing
import com.bettermingle.app.ui.theme.Success
import com.bettermingle.app.ui.theme.TextOnColor

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.bettermingle.app.data.preferences.PremiumTier
import com.bettermingle.app.data.preferences.SettingsManager
import com.bettermingle.app.data.preferences.TierLimits
import com.bettermingle.app.utils.ActivityLogger
import com.bettermingle.app.utils.removeModuleFromEvent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class PollWithOptions(
    val poll: Poll,
    val options: List<PollOption>,
    val voteCounts: Map<String, Int>, // optionId -> count
    val userVotes: Set<String> // optionIds user has voted for
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VotingScreen(
    eventId: String,
    onNavigateBack: () -> Unit,
    onNavigateToUpgrade: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val hapticView = LocalView.current
    val pollsWithOptions = remember { mutableStateListOf<PollWithOptions>() }
    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    val scope = rememberCoroutineScope()
    var showCreateDialog by remember { mutableStateOf(false) }
    var showPollLimitDialog by remember { mutableStateOf(false) }
    var editingPoll by remember { mutableStateOf<PollWithOptions?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var isOrganizer by remember { mutableStateOf(false) }
    var showClosePollDialog by remember { mutableStateOf<PollWithOptions?>(null) }
    var showColorPicker by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(eventId) {
        try {
            val eventDoc = FirebaseFirestore.getInstance()
                .collection("events").document(eventId).get().await()
            isOrganizer = eventDoc.getString("createdBy") == currentUserId
        } catch (_: Exception) { }
    }

    fun loadPolls() {
        scope.launch {
        try {
            val firestore = FirebaseFirestore.getInstance()
            val pollsSnapshot = firestore.collection("events").document(eventId)
                .collection("polls").get().await()

            val loaded = mutableListOf<PollWithOptions>()
            for (pollDoc in pollsSnapshot.documents) {
                val data = pollDoc.data ?: continue
                val pollType = try {
                    PollType.valueOf((data["pollType"] as? String ?: "CUSTOM").uppercase())
                } catch (_: Exception) { PollType.CUSTOM }

                val poll = Poll(
                    id = pollDoc.id,
                    eventId = eventId,
                    createdBy = data["createdBy"] as? String ?: "",
                    title = data["title"] as? String ?: "",
                    pollType = pollType,
                    allowMultiple = data["allowMultiple"] as? Boolean ?: false,
                    isAnonymous = data["isAnonymous"] as? Boolean ?: false,
                    deadline = (data["deadline"] as? Number)?.toLong(),
                    isClosed = data["isClosed"] as? Boolean ?: false,
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0
                )

                val optionsSnapshot = firestore.collection("events").document(eventId)
                    .collection("polls").document(pollDoc.id)
                    .collection("options").get().await()

                val options = optionsSnapshot.documents.map { optDoc ->
                    val optData = optDoc.data ?: emptyMap()
                    PollOption(
                        id = optDoc.id,
                        pollId = pollDoc.id,
                        label = optData["label"] as? String ?: "",
                        description = optData["description"] as? String ?: "",
                        sortOrder = (optData["sortOrder"] as? Number)?.toInt() ?: 0
                    )
                }.sortedBy { it.sortOrder }

                // Load vote counts and user votes
                val voteCounts = mutableMapOf<String, Int>()
                val userVotes = mutableSetOf<String>()
                for (option in options) {
                    val votesSnapshot = firestore.collection("events").document(eventId)
                        .collection("polls").document(pollDoc.id)
                        .collection("options").document(option.id)
                        .collection("votes").get().await()
                    voteCounts[option.id] = votesSnapshot.size()
                    if (votesSnapshot.documents.any { (it.data?.get("userId") as? String) == currentUserId }) {
                        userVotes.add(option.id)
                    }
                }

                loaded.add(PollWithOptions(poll, options, voteCounts, userVotes))
            }

            pollsWithOptions.clear()
            pollsWithOptions.addAll(loaded.sortedByDescending { it.poll.createdAt })
            isLoading = false
        } catch (_: Exception) { isLoading = false }
        }
    }

    LaunchedEffect(eventId) { loadPolls() }

    if (showPollLimitDialog) {
        AlertDialog(
            onDismissRequest = { showPollLimitDialog = false },
            title = { Text(stringResource(R.string.voting_limit_title)) },
            text = {
                Text(stringResource(R.string.voting_limit_message))
            },
            confirmButton = {
                TextButton(onClick = {
                    showPollLimitDialog = false
                    onNavigateToUpgrade()
                }) { Text(stringResource(R.string.voting_limit_upgrade)) }
            },
            dismissButton = {
                TextButton(onClick = { showPollLimitDialog = false }) { Text(stringResource(R.string.common_back)) }
            }
        )
    }

    if (showCreateDialog) {
        CreatePollDialog(
            eventId = eventId,
            onDismiss = { showCreateDialog = false },
            onCreated = {
                showCreateDialog = false
                loadPolls()
                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.voting_created)) }
            }
        )
    }

    if (editingPoll != null) {
        EditPollDialog(
            eventId = eventId,
            pollData = editingPoll!!,
            onDismiss = { editingPoll = null },
            onSaved = {
                editingPoll = null
                loadPolls()
                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.voting_updated)) }
            }
        )
    }

    if (showClosePollDialog != null) {
        AlertDialog(
            onDismissRequest = { showClosePollDialog = null },
            title = { Text(stringResource(R.string.voting_close_poll_confirm_title)) },
            text = { Text(stringResource(R.string.voting_close_poll_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    val pollId = showClosePollDialog!!.poll.id
                    showClosePollDialog = null
                    scope.launch {
                        try {
                            FirebaseFirestore.getInstance()
                                .collection("events").document(eventId)
                                .collection("polls").document(pollId)
                                .update("isClosed", true).await()
                            loadPolls()
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar(context.getString(R.string.error_save_failed))
                        }
                    }
                }) { Text(stringResource(R.string.common_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showClosePollDialog = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    if (showColorPicker) {
        ModuleColorPickerDialog(
            currentColor = PrimaryBlue,
            onColorSelected = { option ->
                showColorPicker = false
                scope.launch {
                    EventRepository(context).updateModuleColor(eventId, "VOTING", option.hex)
                }
            },
            onDismiss = { showColorPicker = false }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.voting_title), style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    if (isOrganizer) {
                        var menuExpanded by remember { mutableStateOf(false) }
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = null)
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_change_color)) },
                                onClick = {
                                    menuExpanded = false
                                    showColorPicker = true
                                },
                                leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.dashboard_remove_module)) },
                                onClick = {
                                    menuExpanded = false
                                    scope.launch {
                                        removeModuleFromEvent(eventId, "VOTING")
                                        onNavigateBack()
                                    }
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                            )
                        }
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
                    scope.launch {
                        val settingsManager = SettingsManager(context)
                        val settings = settingsManager.settingsFlow.first()
                        val maxPolls = TierLimits.maxPolls(settings.premiumTier)
                        if (pollsWithOptions.size >= maxPolls) {
                            showPollLimitDialog = true
                        } else {
                            showCreateDialog = true
                        }
                    }
                },
                containerColor = Color.Transparent,
                contentColor = TextOnColor,
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                modifier = Modifier
                    .shadow(8.dp, CircleShape, ambientColor = AccentOrange.copy(alpha = 0.3f), spotColor = AccentOrange.copy(alpha = 0.3f))
                    .background(AccentOrange, CircleShape)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.voting_new))
            }
        }
    ) { innerPadding ->
        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                loadPolls()
                scope.launch {
                    kotlinx.coroutines.delay(500)
                    isRefreshing = false
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
        when {
            isLoading && pollsWithOptions.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator()
                }
            }
            pollsWithOptions.isEmpty() && !isRefreshing -> {
                EmptyState(
                    icon = Icons.Default.HowToVote,
                    illustration = R.drawable.il_empty_voting,
                    iconDescription = stringResource(R.string.voting_empty_icon),
                    title = stringResource(R.string.voting_empty_title),
                    description = stringResource(R.string.voting_empty_description),
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(Spacing.screenPadding),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                items(pollsWithOptions, key = { it.poll.id }) { pollData ->
                    PollCard(
                        pollData = pollData,
                        eventId = eventId,
                        currentUserId = currentUserId,
                        onVoted = { loadPolls() },
                        onEdit = { editingPoll = pollData },
                        onClose = {
                            showClosePollDialog = pollData
                        },
                        scope = scope
                    )
                }
            }
            }
        }
        }
    }
}

@Composable
private fun PollCard(
    pollData: PollWithOptions,
    eventId: String,
    currentUserId: String,
    onVoted: () -> Unit,
    onEdit: () -> Unit,
    onClose: () -> Unit,
    scope: kotlinx.coroutines.CoroutineScope
) {
    val hapticView = LocalView.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val isCreator = pollData.poll.createdBy == currentUserId
    val isExpired = pollData.poll.deadline != null && System.currentTimeMillis() > pollData.poll.deadline
    val isEffectivelyClosed = pollData.poll.isClosed || isExpired

    BetterMingleCard {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = pollData.poll.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isCreator && !isEffectivelyClosed) {
                        IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = stringResource(R.string.voting_close_description),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = stringResource(R.string.voting_edit_description),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    PollTypeBadge(type = pollData.poll.pollType)
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xs))

            // Selection mode indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isEffectivelyClosed) {
                    if (isExpired && !pollData.poll.isClosed) {
                        // Expired badge
                        Box(
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.extraSmall)
                                .background(AccentOrange.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.voting_expired),
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentOrange
                            )
                        }
                    } else {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Success,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.voting_poll_closed),
                            style = MaterialTheme.typography.bodySmall,
                            color = Success
                        )
                    }
                } else {
                    Text(
                        text = if (pollData.poll.allowMultiple) stringResource(R.string.voting_multiple_answers) else stringResource(R.string.voting_single_answer),
                        style = MaterialTheme.typography.bodySmall,
                        color = PrimaryBlue
                    )
                    // Show remaining time if deadline is set
                    if (pollData.poll.deadline != null) {
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Text(
                            text = stringResource(R.string.voting_ends_in, formatRemainingTime(pollData.poll.deadline)),
                            style = MaterialTheme.typography.bodySmall,
                            color = AccentOrange
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            val totalVotes = pollData.voteCounts.values.sum()
            val hasVoted = pollData.userVotes.isNotEmpty()

            for (option in pollData.options) {
                val voteCount = pollData.voteCounts[option.id] ?: 0
                val isSelected = option.id in pollData.userVotes
                PollOptionItem(
                    option = option,
                    voteCount = voteCount,
                    totalVotes = totalVotes,
                    isSelected = isSelected,
                    allowMultiple = pollData.poll.allowMultiple,
                    onVote = {
                        if (!isEffectivelyClosed && !isSelected) {
                            hapticView.performHapticClick()
                            // For single-select: check if user already voted
                            if (!pollData.poll.allowMultiple && hasVoted) {
                                // Remove existing vote first, then add new one
                                scope.launch {
                                    try {
                                        val firestore = FirebaseFirestore.getInstance()
                                        val pollRef = firestore.collection("events").document(eventId)
                                            .collection("polls").document(pollData.poll.id)

                                        // Remove all existing votes by this user
                                        for (existingOptionId in pollData.userVotes) {
                                            val existingVotes = pollRef.collection("options")
                                                .document(existingOptionId)
                                                .collection("votes")
                                                .get().await()
                                            for (voteDoc in existingVotes.documents) {
                                                if ((voteDoc.data?.get("userId") as? String) == currentUserId) {
                                                    voteDoc.reference.delete().await()
                                                }
                                            }
                                        }

                                        // Add new vote
                                        val voteId = UUID.randomUUID().toString()
                                        pollRef.collection("options").document(option.id)
                                            .collection("votes").document(voteId)
                                            .set(mapOf("userId" to currentUserId, "value" to 1))
                                            .await()
                                        ActivityLogger.log(eventId, "vote", context.getString(R.string.activity_voted_for, option.label, pollData.poll.title))
                                        onVoted()
                                    } catch (_: Exception) { }
                                }
                            } else {
                                // Multi-select or first vote
                                scope.launch {
                                    try {
                                        val voteId = UUID.randomUUID().toString()
                                        FirebaseFirestore.getInstance()
                                            .collection("events").document(eventId)
                                            .collection("polls").document(pollData.poll.id)
                                            .collection("options").document(option.id)
                                            .collection("votes").document(voteId)
                                            .set(mapOf("userId" to currentUserId, "value" to 1))
                                            .await()
                                        ActivityLogger.log(eventId, "vote", context.getString(R.string.activity_voted_for, option.label, pollData.poll.title))
                                        onVoted()
                                    } catch (_: Exception) { }
                                }
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
            }
        }
    }
}

@Composable
private fun PollTypeBadge(type: PollType) {
    val (text, color) = when (type) {
        PollType.DATE -> stringResource(R.string.voting_type_date) to PrimaryBlue
        PollType.LOCATION -> stringResource(R.string.voting_type_location) to AccentPink
        PollType.ACTIVITY -> stringResource(R.string.voting_type_activity) to Success
        PollType.PRICE -> stringResource(R.string.voting_type_price) to AccentGold
        PollType.CUSTOM -> stringResource(R.string.voting_type_custom) to MaterialTheme.colorScheme.onSurfaceVariant
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
fun PollOptionItem(
    option: PollOption,
    voteCount: Int,
    totalVotes: Int,
    isSelected: Boolean,
    allowMultiple: Boolean = false,
    onVote: () -> Unit
) {
    val progress = if (totalVotes > 0) voteCount.toFloat() / totalVotes else 0f

    BetterMingleCard(onClick = onVote) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (allowMultiple) {
                            if (isSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank
                        } else {
                            if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked
                        },
                        contentDescription = null,
                        tint = if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }

                Text(
                    text = "$voteCount ${stringResource(R.string.voting_votes_count)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xs))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(MaterialTheme.shapes.extraSmall),
                color = if (isSelected) PrimaryBlue else AccentPink.copy(alpha = 0.5f),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun CreatePollDialog(
    eventId: String,
    onDismiss: () -> Unit,
    onCreated: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(PollType.CUSTOM) }
    var allowMultiple by remember { mutableStateOf(false) }
    var isAnonymous by remember { mutableStateOf(false) }
    var deadlineEnabled by remember { mutableStateOf(false) }
    var deadlineMillis by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val options = remember { mutableStateListOf("", "") }
    val scope = rememberCoroutineScope()

    val pollTypes = listOf(
        PollType.DATE to stringResource(R.string.voting_type_date),
        PollType.LOCATION to stringResource(R.string.voting_type_location),
        PollType.ACTIVITY to stringResource(R.string.voting_type_activity),
        PollType.PRICE to stringResource(R.string.voting_type_price),
        PollType.CUSTOM to stringResource(R.string.voting_type_custom)
    )

    val isValid = title.isNotBlank() && options.count { it.isNotBlank() } >= 2

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.voting_new)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                BetterMingleTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = stringResource(R.string.voting_poll_name_label)
                )

                Spacer(modifier = Modifier.height(Spacing.sm))

                Text(
                    text = stringResource(R.string.voting_poll_type),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    pollTypes.forEach { (type, label) ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            shape = RoundedCornerShape(100.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryBlue.copy(alpha = 0.12f),
                                selectedLabelColor = PrimaryBlue
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.sm))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(stringResource(R.string.voting_multiple_answers), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = if (allowMultiple) stringResource(R.string.voting_multiple_hint) else stringResource(R.string.voting_single_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = allowMultiple,
                        onCheckedChange = { allowMultiple = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = PrimaryBlue)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.voting_anonymous), style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = isAnonymous,
                        onCheckedChange = { isAnonymous = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = PrimaryBlue)
                    )
                }

                // Deadline toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.voting_deadline_toggle), style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = deadlineEnabled,
                        onCheckedChange = { deadlineEnabled = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = PrimaryBlue)
                    )
                }

                if (deadlineEnabled) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        TextButton(onClick = { showDatePicker = true }) {
                            Text(
                                if (deadlineMillis != null) {
                                    java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault())
                                        .format(java.util.Date(deadlineMillis!!))
                                } else {
                                    stringResource(R.string.voting_deadline_select_date)
                                }
                            )
                        }
                        TextButton(onClick = { showTimePicker = true }) {
                            Text(
                                if (deadlineMillis != null) {
                                    java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                                        .format(java.util.Date(deadlineMillis!!))
                                } else {
                                    stringResource(R.string.voting_deadline_select_time)
                                }
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.voting_deadline_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                }

                if (showDatePicker) {
                    val datePickerState = androidx.compose.material3.rememberDatePickerState(
                        initialSelectedDateMillis = deadlineMillis ?: System.currentTimeMillis()
                    )
                    androidx.compose.material3.DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                val selectedDate = datePickerState.selectedDateMillis
                                if (selectedDate != null) {
                                    // Preserve time if already set, otherwise default to 23:59
                                    val cal = java.util.Calendar.getInstance()
                                    if (deadlineMillis != null) {
                                        val timeCal = java.util.Calendar.getInstance()
                                        timeCal.timeInMillis = deadlineMillis!!
                                        cal.timeInMillis = selectedDate
                                        cal.set(java.util.Calendar.HOUR_OF_DAY, timeCal.get(java.util.Calendar.HOUR_OF_DAY))
                                        cal.set(java.util.Calendar.MINUTE, timeCal.get(java.util.Calendar.MINUTE))
                                    } else {
                                        cal.timeInMillis = selectedDate
                                        cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
                                        cal.set(java.util.Calendar.MINUTE, 59)
                                    }
                                    deadlineMillis = cal.timeInMillis
                                }
                                showDatePicker = false
                            }) { Text(stringResource(R.string.common_confirm)) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.common_cancel)) }
                        }
                    ) {
                        androidx.compose.material3.DatePicker(state = datePickerState)
                    }
                }

                if (showTimePicker) {
                    val cal = java.util.Calendar.getInstance()
                    if (deadlineMillis != null) cal.timeInMillis = deadlineMillis!!
                    val timePickerState = androidx.compose.material3.rememberTimePickerState(
                        initialHour = cal.get(java.util.Calendar.HOUR_OF_DAY),
                        initialMinute = cal.get(java.util.Calendar.MINUTE),
                        is24Hour = true
                    )
                    AlertDialog(
                        onDismissRequest = { showTimePicker = false },
                        title = { Text(stringResource(R.string.voting_deadline_select_time)) },
                        text = {
                            androidx.compose.material3.TimePicker(state = timePickerState)
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                val dateCal = java.util.Calendar.getInstance()
                                if (deadlineMillis != null) dateCal.timeInMillis = deadlineMillis!!
                                dateCal.set(java.util.Calendar.HOUR_OF_DAY, timePickerState.hour)
                                dateCal.set(java.util.Calendar.MINUTE, timePickerState.minute)
                                dateCal.set(java.util.Calendar.SECOND, 0)
                                deadlineMillis = dateCal.timeInMillis
                                showTimePicker = false
                            }) { Text(stringResource(R.string.common_confirm)) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.common_cancel)) }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.sm))

                Text(
                    text = stringResource(R.string.voting_options),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(Spacing.xs))

                options.forEachIndexed { index, option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        BetterMingleTextField(
                            value = option,
                            onValueChange = { options[index] = it },
                            label = stringResource(R.string.voting_option_label, index + 1),
                            modifier = Modifier.weight(1f)
                        )
                        if (options.size > 2) {
                            IconButton(onClick = { options.removeAt(index) }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.common_remove), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    if (index < options.lastIndex) {
                        Spacer(modifier = Modifier.height(Spacing.xs))
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.xs))
                TextButton(onClick = { options.add("") }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.voting_add_option))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    scope.launch {
                        try {
                            val currentUser = FirebaseAuth.getInstance().currentUser
                            val firestore = FirebaseFirestore.getInstance()
                            val pollData = hashMapOf<String, Any?>(
                                "createdBy" to (currentUser?.uid ?: ""),
                                "title" to title,
                                "pollType" to selectedType.name,
                                "allowMultiple" to allowMultiple,
                                "isAnonymous" to isAnonymous,
                                "isClosed" to false,
                                "deadline" to if (deadlineEnabled) deadlineMillis else null,
                                "createdAt" to System.currentTimeMillis()
                            )
                            val pollRef = firestore.collection("events").document(eventId)
                                .collection("polls").add(pollData).await()

                            val validOptions = options.filter { it.isNotBlank() }
                            validOptions.forEachIndexed { index, label ->
                                val optionData = hashMapOf(
                                    "label" to label,
                                    "description" to "",
                                    "sortOrder" to index
                                )
                                pollRef.collection("options").add(optionData).await()
                            }
                            ActivityLogger.log(eventId, "vote", context.getString(R.string.activity_created_poll, title))
                            onCreated()
                        } catch (_: Exception) { }
                    }
                },
                enabled = isValid
            ) {
                Text(stringResource(R.string.common_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}

@Composable
private fun EditPollDialog(
    eventId: String,
    pollData: PollWithOptions,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val existingOptions = remember { mutableStateListOf<String>().apply {
        addAll(pollData.options.map { it.label })
    }}
    val newOptions = remember { mutableStateListOf<String>() }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.voting_edit_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = pollData.poll.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(Spacing.md))

                Text(
                    text = stringResource(R.string.voting_existing_options),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(Spacing.xs))

                existingOptions.forEachIndexed { index, label ->
                    BetterMingleTextField(
                        value = label,
                        onValueChange = {},
                        label = stringResource(R.string.voting_option_label, index + 1),
                        enabled = false
                    )
                    if (index < existingOptions.lastIndex || newOptions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(Spacing.xs))
                    }
                }

                if (newOptions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(Spacing.md))
                    Text(
                        text = stringResource(R.string.voting_new_options),
                        style = MaterialTheme.typography.labelMedium,
                        color = PrimaryBlue
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))

                    newOptions.forEachIndexed { index, option ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            BetterMingleTextField(
                                value = option,
                                onValueChange = { newOptions[index] = it },
                                label = stringResource(R.string.voting_new_option_label, index + 1),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { newOptions.removeAt(index) }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.common_remove), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (index < newOptions.lastIndex) {
                            Spacer(modifier = Modifier.height(Spacing.xs))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.xs))
                TextButton(onClick = { newOptions.add("") }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.voting_add_option))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    scope.launch {
                        try {
                            val firestore = FirebaseFirestore.getInstance()
                            val pollRef = firestore.collection("events").document(eventId)
                                .collection("polls").document(pollData.poll.id)

                            val validNew = newOptions.filter { it.isNotBlank() }
                            val nextSortOrder = pollData.options.maxOfOrNull { it.sortOrder }?.plus(1) ?: pollData.options.size

                            validNew.forEachIndexed { index, label ->
                                val optionData = hashMapOf(
                                    "label" to label,
                                    "description" to "",
                                    "sortOrder" to (nextSortOrder + index)
                                )
                                pollRef.collection("options").add(optionData).await()
                            }
                            onSaved()
                        } catch (_: Exception) { }
                    }
                },
                enabled = newOptions.any { it.isNotBlank() }
            ) {
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}

private fun formatRemainingTime(deadlineMillis: Long): String {
    val remaining = deadlineMillis - System.currentTimeMillis()
    if (remaining <= 0) return ""
    val days = remaining / (1000 * 60 * 60 * 24)
    val hours = (remaining % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60)
    val minutes = (remaining % (1000 * 60 * 60)) / (1000 * 60)
    return buildString {
        if (days > 0) append("${days}d ")
        if (hours > 0) append("${hours}h ")
        if (days == 0L && minutes > 0) append("${minutes}m")
    }.trim()
}
