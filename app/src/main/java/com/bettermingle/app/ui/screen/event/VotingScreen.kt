package com.bettermingle.app.ui.screen.event

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
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.bettermingle.app.ui.theme.TextSecondary
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
    onNavigateBack: () -> Unit
) {
    val pollsWithOptions = remember { mutableStateListOf<PollWithOptions>() }
    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    val scope = rememberCoroutineScope()
    var showCreateDialog by remember { mutableStateOf(false) }

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

    if (showCreateDialog) {
        CreatePollDialog(
            eventId = eventId,
            onDismiss = { showCreateDialog = false },
            onCreated = {
                showCreateDialog = false
                loadPolls()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hlasování", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = AccentOrange,
                contentColor = TextOnColor
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nová anketa")
            }
        }
    ) { innerPadding ->
        if (pollsWithOptions.isEmpty()) {
            EmptyState(
                icon = Icons.Default.HowToVote,
                title = "Zatím žádné ankety",
                description = "Vytvoř anketu a nech ostatní hlasovat.",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(Spacing.screenPadding),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                items(pollsWithOptions, key = { it.poll.id }) { pollData ->
                    PollCard(
                        pollData = pollData,
                        eventId = eventId,
                        currentUserId = currentUserId,
                        onVoted = { optionId ->
                            // Update local state after vote
                            val idx = pollsWithOptions.indexOfFirst { it.poll.id == pollData.poll.id }
                            if (idx >= 0) {
                                val old = pollsWithOptions[idx]
                                val newVoteCounts = old.voteCounts.toMutableMap()
                                newVoteCounts[optionId] = (newVoteCounts[optionId] ?: 0) + 1
                                val newUserVotes = old.userVotes + optionId
                                pollsWithOptions[idx] = old.copy(voteCounts = newVoteCounts, userVotes = newUserVotes)
                            }
                        },
                        scope = scope
                    )
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
    onVoted: (String) -> Unit,
    scope: kotlinx.coroutines.CoroutineScope
) {
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

                PollTypeBadge(type = pollData.poll.pollType)
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            if (pollData.poll.isClosed) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Success,
                        modifier = Modifier.height(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Anketa uzavřena",
                        style = MaterialTheme.typography.bodySmall,
                        color = Success
                    )
                }
            } else {
                Text(
                    text = "Aktivní",
                    style = MaterialTheme.typography.bodySmall,
                    color = PrimaryBlue
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            val totalVotes = pollData.voteCounts.values.sum()
            for (option in pollData.options) {
                val voteCount = pollData.voteCounts[option.id] ?: 0
                val isSelected = option.id in pollData.userVotes
                PollOptionItem(
                    option = option,
                    voteCount = voteCount,
                    totalVotes = totalVotes,
                    isSelected = isSelected,
                    onVote = {
                        if (!pollData.poll.isClosed && !isSelected) {
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
                                    onVoted(option.id)
                                } catch (_: Exception) { }
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
        PollType.DATE -> "Datum" to PrimaryBlue
        PollType.LOCATION -> "Místo" to AccentPink
        PollType.ACTIVITY -> "Aktivita" to Success
        PollType.PRICE -> "Cena" to AccentGold
        PollType.CUSTOM -> "Vlastní" to TextSecondary
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
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "$voteCount hlasů",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
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
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(PollType.CUSTOM) }
    var allowMultiple by remember { mutableStateOf(false) }
    var isAnonymous by remember { mutableStateOf(false) }
    val options = remember { mutableStateListOf("", "") }
    val scope = rememberCoroutineScope()

    val pollTypes = listOf(
        PollType.DATE to "Datum",
        PollType.LOCATION to "Místo",
        PollType.ACTIVITY to "Aktivita",
        PollType.PRICE to "Cena",
        PollType.CUSTOM to "Vlastní"
    )

    val isValid = title.isNotBlank() && options.count { it.isNotBlank() } >= 2

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nová anketa") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                BetterMingleTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = "Název ankety"
                )

                Spacer(modifier = Modifier.height(Spacing.sm))

                Text(
                    text = "Typ ankety",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
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
                    Text("Více odpovědí", style = MaterialTheme.typography.bodyMedium)
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
                    Text("Anonymní", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = isAnonymous,
                        onCheckedChange = { isAnonymous = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = PrimaryBlue)
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.sm))

                Text(
                    text = "Možnosti",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
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
                            label = "Možnost ${index + 1}",
                            modifier = Modifier.weight(1f)
                        )
                        if (options.size > 2) {
                            IconButton(onClick = { options.removeAt(index) }) {
                                Icon(Icons.Default.Close, contentDescription = "Odebrat", tint = TextSecondary)
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
                    Text("Přidat možnost")
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
                            onCreated()
                        } catch (_: Exception) { }
                    }
                },
                enabled = isValid
            ) {
                Text("Vytvořit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Zrušit") }
        }
    )
}
