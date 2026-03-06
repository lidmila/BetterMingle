package com.bettermingle.app.ui.screen.event

import com.bettermingle.app.R
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Person
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import com.bettermingle.app.data.model.EventRoom
import com.bettermingle.app.ui.component.BetterMingleCard
import com.bettermingle.app.ui.component.BetterMingleTextField
import com.bettermingle.app.ui.component.EmptyState
import com.bettermingle.app.ui.theme.AccentOrange
import com.bettermingle.app.ui.theme.BackgroundPrimary
import com.bettermingle.app.ui.theme.PrimaryBlue
import com.bettermingle.app.ui.theme.Spacing
import com.bettermingle.app.ui.theme.TextOnColor
import com.bettermingle.app.ui.theme.TextSecondary
import com.bettermingle.app.utils.ActivityLogger
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.ui.text.input.KeyboardType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomsScreen(
    eventId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val rooms = remember { mutableStateListOf<EventRoom>() }
    val userNames = remember { mutableMapOf<String, String>() }
    val allParticipants = remember { mutableStateListOf<Pair<String, String>>() } // uid to displayName
    var showCreateDialog by remember { mutableStateOf(false) }
    var assignRoom by remember { mutableStateOf<EventRoom?>(null) }
    var roomToDelete by remember { mutableStateOf<EventRoom?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun loadRooms() {
        scope.launch {
        try {
            val firestore = FirebaseFirestore.getInstance()
            val snapshot = firestore.collection("events").document(eventId)
                .collection("rooms").get().await()

            val loaded = snapshot.documents.map { doc ->
                val data = doc.data ?: emptyMap()
                @Suppress("UNCHECKED_CAST")
                val assignments = (data["assignments"] as? List<String>) ?: emptyList()
                EventRoom(
                    id = doc.id,
                    eventId = eventId,
                    name = data["name"] as? String ?: "",
                    capacity = (data["capacity"] as? Number)?.toInt() ?: 2,
                    notes = data["notes"] as? String ?: "",
                    assignments = assignments
                )
            }

            // Load user names for assignments
            val allUserIds = loaded.flatMap { it.assignments }.distinct()
            for (uid in allUserIds) {
                try {
                    val userDoc = firestore.collection("users").document(uid).get().await()
                    userNames[uid] = userDoc.getString("displayName") ?: uid.take(8)
                } catch (_: Exception) { userNames[uid] = uid.take(8) }
            }

            rooms.clear()
            rooms.addAll(loaded)

            // Load all event participants for assignment
            try {
                val partsSnapshot = firestore.collection("events").document(eventId)
                    .collection("participants").get().await()
                allParticipants.clear()
                partsSnapshot.documents.forEach { doc ->
                    val name = doc.getString("displayName") ?: doc.id.take(8)
                    allParticipants.add(doc.id to name)
                    userNames[doc.id] = name
                }
            } catch (_: Exception) { }
        } catch (_: Exception) { }
        }
    }

    LaunchedEffect(eventId) { loadRooms() }

    if (showCreateDialog) {
        AddRoomDialog(
            eventId = eventId,
            onDismiss = { showCreateDialog = false },
            onCreated = {
                showCreateDialog = false
                loadRooms()
            }
        )
    }

    assignRoom?.let { room ->
        AssignParticipantDialog(
            eventId = eventId,
            room = room,
            allParticipants = allParticipants,
            assignedRooms = rooms,
            onDismiss = { assignRoom = null },
            onUpdated = {
                assignRoom = null
                loadRooms()
            }
        )
    }

    if (roomToDelete != null) {
        AlertDialog(
            onDismissRequest = { roomToDelete = null },
            title = { Text(stringResource(R.string.rooms_delete_title)) },
            text = { Text(stringResource(R.string.rooms_delete_confirm, roomToDelete!!.name)) },
            confirmButton = {
                TextButton(onClick = {
                    val room = roomToDelete!!
                    roomToDelete = null
                    scope.launch {
                        try {
                            FirebaseFirestore.getInstance()
                                .collection("events").document(eventId)
                                .collection("rooms").document(room.id)
                                .delete().await()
                            rooms.removeAll { it.id == room.id }
                            ActivityLogger.log(eventId, "room", context.getString(R.string.activity_deleted_room, room.name))
                        } catch (_: Exception) { }
                    }
                }) {
                    Text(stringResource(R.string.common_delete), color = AccentOrange)
                }
            },
            dismissButton = {
                TextButton(onClick = { roomToDelete = null }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.rooms_title), style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = Color.Transparent,
                contentColor = TextOnColor,
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                modifier = Modifier
                    .shadow(8.dp, CircleShape, ambientColor = AccentOrange.copy(alpha = 0.3f), spotColor = AccentOrange.copy(alpha = 0.3f))
                    .background(AccentOrange, CircleShape)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.rooms_add))
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                loadRooms()
                scope.launch {
                    kotlinx.coroutines.delay(500)
                    isRefreshing = false
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (rooms.isEmpty() && !isRefreshing) {
                EmptyState(
                    icon = Icons.Default.Hotel,
                    illustration = R.drawable.il_empty_rooms,
                    title = stringResource(R.string.rooms_empty_title),
                    description = stringResource(R.string.rooms_empty_description),
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(Spacing.screenPadding),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    items(rooms, key = { it.id }) { room ->
                        RoomCard(
                            room = room,
                            userNames = userNames,
                            onClick = { assignRoom = room },
                            onDelete = { roomToDelete = room }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RoomCard(room: EventRoom, userNames: Map<String, String>, onClick: () -> Unit, onDelete: () -> Unit) {
    BetterMingleCard(onClick = onClick) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = room.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "${room.assignments.size}/${room.capacity}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (room.assignments.size >= room.capacity) AccentOrange else TextSecondary
                )

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.common_delete), tint = AccentOrange, modifier = Modifier.size(20.dp))
                }
            }

            if (room.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = room.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            if (room.assignments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    room.assignments.forEach { userId ->
                        Box(
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.extraSmall)
                                .background(PrimaryBlue.copy(alpha = 0.1f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = userNames[userId] ?: userId.take(8),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PrimaryBlue
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddRoomDialog(
    eventId: String,
    onDismiss: () -> Unit,
    onCreated: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var capacity by remember { mutableStateOf("2") }
    var notes by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rooms_add)) },
        text = {
            Column {
                BetterMingleTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(R.string.rooms_name_label)
                )

                Spacer(modifier = Modifier.height(Spacing.formFieldSpacing))

                BetterMingleTextField(
                    value = capacity,
                    onValueChange = { capacity = it },
                    label = stringResource(R.string.rooms_capacity_label),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(Spacing.formFieldSpacing))

                BetterMingleTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = stringResource(R.string.rooms_notes_label),
                    singleLine = false,
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    scope.launch {
                        try {
                            val roomData = hashMapOf(
                                "name" to name,
                                "capacity" to (capacity.toIntOrNull() ?: 2),
                                "notes" to notes,
                                "assignments" to emptyList<String>()
                            )
                            FirebaseFirestore.getInstance()
                                .collection("events").document(eventId)
                                .collection("rooms")
                                .add(roomData).await()
                            val cap = capacity.toIntOrNull() ?: 2
                            ActivityLogger.log(eventId, "room", context.getString(R.string.activity_created_room, name, cap))
                            onCreated()
                        } catch (_: Exception) { }
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(R.string.common_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AssignParticipantDialog(
    eventId: String,
    room: EventRoom,
    allParticipants: List<Pair<String, String>>,
    assignedRooms: List<EventRoom>,
    onDismiss: () -> Unit,
    onUpdated: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentAssignments = remember { mutableStateListOf(*room.assignments.toTypedArray()) }

    // Participants already assigned to other rooms
    val assignedElsewhere = remember(assignedRooms) {
        assignedRooms.filter { it.id != room.id }.flatMap { it.assignments }.toSet()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rooms_assign_title, room.name)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.rooms_assign_occupancy, currentAssignments.size, room.capacity),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (currentAssignments.size >= room.capacity) AccentOrange else TextSecondary
                )
                Spacer(modifier = Modifier.height(Spacing.sm))

                allParticipants.forEach { (uid, name) ->
                    val isAssigned = uid in currentAssignments
                    val isInOtherRoom = uid in assignedElsewhere
                    val isFull = currentAssignments.size >= room.capacity && !isAssigned

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .clickable(enabled = !isInOtherRoom && !isFull) {
                                if (isAssigned) {
                                    currentAssignments.remove(uid)
                                } else {
                                    currentAssignments.add(uid)
                                }
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isAssigned) Icons.Default.Person else Icons.Default.Add,
                            contentDescription = null,
                            tint = when {
                                isAssigned -> PrimaryBlue
                                isInOtherRoom -> TextSecondary.copy(alpha = 0.4f)
                                else -> TextSecondary
                            },
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Text(
                            text = name + if (isInOtherRoom) stringResource(R.string.rooms_assign_in_other_room) else "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isInOtherRoom) TextSecondary.copy(alpha = 0.5f) else Color.Unspecified
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    scope.launch {
                        try {
                            FirebaseFirestore.getInstance()
                                .collection("events").document(eventId)
                                .collection("rooms").document(room.id)
                                .update("assignments", currentAssignments.toList())
                                .await()
                            ActivityLogger.log(eventId, "room", context.getString(R.string.activity_joined_room, room.name))
                            onUpdated()
                        } catch (_: Exception) { }
                    }
                }
            ) {
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}
