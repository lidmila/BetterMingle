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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bettermingle.app.R
import com.bettermingle.app.ui.component.EmptyState
import com.bettermingle.app.ui.component.UserAvatar
import com.bettermingle.app.ui.theme.AccentOrange
import com.bettermingle.app.ui.theme.PrimaryBlue
import com.bettermingle.app.ui.theme.Spacing
import com.bettermingle.app.ui.theme.Success
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private data class ParticipantDietary(
    val displayName: String,
    val avatarUrl: String,
    val preferences: List<String>
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CateringScreen(
    eventId: String,
    onNavigateBack: () -> Unit
) {
    var participants by remember { mutableStateOf<List<ParticipantDietary>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    suspend fun loadData() {
        try {
            val firestore = FirebaseFirestore.getInstance()
            val partDocs = firestore.collection("events").document(eventId)
                .collection("participants").get().await()

            val userIds = partDocs.documents.mapNotNull { it.getString("userId") }.distinct()
            val result = mutableListOf<ParticipantDietary>()

            // Batch fetch users in chunks of 30 (Firestore whereIn limit)
            for (chunk in userIds.chunked(30)) {
                try {
                    val userDocs = firestore.collection("users")
                        .whereIn(FieldPath.documentId(), chunk)
                        .get().await()
                    for (userDoc in userDocs.documents) {
                        val prefs = (userDoc.get("dietaryPreferences") as? List<*>)
                            ?.filterIsInstance<String>() ?: emptyList()
                        if (prefs.isNotEmpty()) {
                            result.add(
                                ParticipantDietary(
                                    displayName = userDoc.getString("displayName") ?: userDoc.id.take(8),
                                    avatarUrl = userDoc.getString("avatarUrl") ?: "",
                                    preferences = prefs
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.error_load_failed)) }
                }
            }

            participants = result
        } catch (e: Exception) {
            scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.error_load_failed)) }
        }
        isLoading = false
        isRefreshing = false
    }

    LaunchedEffect(eventId) {
        loadData()
    }

    // Aggregate preference counts
    val preferenceSummary = remember(participants) {
        participants.flatMap { it.preferences }
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.catering_title), style = MaterialTheme.typography.titleMedium) },
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            participants.isEmpty() -> {
                EmptyState(
                    icon = Icons.Default.Restaurant,
                    title = stringResource(R.string.catering_empty),
                    description = "",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
            else -> {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        isRefreshing = true
                        scope.launch { loadData() }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(Spacing.screenPadding),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        // Summary section
                        if (preferenceSummary.isNotEmpty()) {
                            item(key = "summary_header") {
                                Text(
                                    text = stringResource(R.string.catering_summary_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(Spacing.sm))
                            }

                            item(key = "summary_cards") {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                                ) {
                                    preferenceSummary.forEach { (pref, count) ->
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = PrimaryBlue.copy(alpha = 0.08f)
                                            ),
                                            shape = MaterialTheme.shapes.small
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                                            ) {
                                                Text(
                                                    text = pref,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium,
                                                    color = PrimaryBlue
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .clip(MaterialTheme.shapes.extraSmall)
                                                        .background(AccentOrange.copy(alpha = 0.15f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = if (count == 1) stringResource(R.string.catering_participant_count, count)
                                                               else stringResource(R.string.catering_participants_count, count),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = AccentOrange
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(Spacing.lg))
                            }
                        }

                        // Individual participants
                        items(participants, key = { it.displayName }) { participant ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                ),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(Spacing.md),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    UserAvatar(
                                        avatarUrl = participant.avatarUrl,
                                        displayName = participant.displayName,
                                        size = 40.dp
                                    )
                                    Spacer(modifier = Modifier.width(Spacing.md))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = participant.displayName,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                                            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                                        ) {
                                            participant.preferences.forEach { pref ->
                                                Box(
                                                    modifier = Modifier
                                                        .clip(MaterialTheme.shapes.extraSmall)
                                                        .background(Success.copy(alpha = 0.12f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
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
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
