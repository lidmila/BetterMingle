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
import androidx.compose.material.icons.filled.RadioButtonChecked
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
    val snackbarHostState = remember { SnackbarHostState() }

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
        } catch (_: Exception) { }
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
        if (pollsWithOptions.isEmpty() && !isRefreshing) {
            EmptyState(
                icon = Icons.Default.HowToVote,
                illustration = R.drawable.il_empty_voting,
                iconDescription = stringResource(R.string.voting_empty_icon),
                title = stringResource(R.string.voting_empty_title),
                description = stringResource(R.string.voting_empty_description),
                modifier = Modifier.fillMaxSize()
            )
        } else {
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
                            scope.launch {
                                try {
                                    FirebaseFirestore.getInstance()
                                        .collection("events").document(eventId)
                                        .collection("polls").document(pollData.poll.id)
                                        .update("isClosed", true).await()
                                    loadPolls()
                                } catch (_: Exception) { }
                            }
                        },
                        scope = scope
                    )
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
                    if (isCreator && !pollData.poll.isClosed) {
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
                if (pollData.poll.isClosed) {
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
                } else {
                    Text(
                        text = if (pollData.poll.allowMultiple) stringResource(R.string.voting_multiple_answers) else stringResource(R.string.voting_single_answer),
                        style = MaterialTheme.typography.bodySmall,
                        color = PrimaryBlue
                    )
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
                        if (!pollData.poll.isClosed && !isSelected) {
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

@OptIn(ExperimentalLayoutApi::class)
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
                            val pollData = hashMapOf(
                                "createdBy" to (currentUser?.uid ?: ""),
                                "title" to title,
                                "pollType" to selectedType.name,
                                "allowMultiple" to allowMultiple,
                                "isAnonymous" to isAnonymous,
                                "isClosed" to false,
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
