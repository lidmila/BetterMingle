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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import com.bettermingle.app.data.model.EventRoom
import com.bettermingle.app.ui.component.BetterMingleButton
import com.bettermingle.app.ui.component.BetterMingleCard
import com.bettermingle.app.ui.component.BetterMingleOutlinedButton
import com.bettermingle.app.ui.component.BetterMingleTextField
import com.bettermingle.app.ui.component.EmptyState
import com.bettermingle.app.ui.theme.AccentOrange
import com.bettermingle.app.ui.theme.PrimaryBlue
import com.bettermingle.app.ui.theme.Spacing
import com.bettermingle.app.ui.theme.TextOnColor

import com.bettermingle.app.ui.component.ModuleColorPickerDialog
import com.bettermingle.app.data.repository.EventRepository
import androidx.compose.material.icons.filled.Palette
import com.bettermingle.app.ui.theme.AccentGold
import com.bettermingle.app.utils.ActivityLogger
import com.bettermingle.app.utils.removeModuleFromEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomsScreen(
    eventId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val rooms = remember { mutableStateListOf<EventRoom>() }
    val userNames = remember { mutableMapOf<String, String>() }
    var showCreateDialog by remember { mutableStateOf(false) }
    var roomToDelete by remember { mutableStateOf<EventRoom?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var isOrganizer by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }

    LaunchedEffect(eventId) {
        try {
            val eventDoc = FirebaseFirestore.getInstance()
                .collection("events").document(eventId).get().await()
            isOrganizer = eventDoc.getString("createdBy") == currentUserId
        } catch (_: Exception) { }
    }

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

            // Load current user's name if not already loaded
            try {
                if (currentUserId.isNotEmpty() && currentUserId !in userNames) {
                    val userDoc = firestore.collection("users").document(currentUserId).get().await()
                    userNames[currentUserId] = userDoc.getString("displayName") ?: currentUserId.take(8)
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

    fun handleJoinRoom(room: EventRoom) {
        // Join this room
        scope.launch {
            try {
                val updated = room.assignments + currentUserId
                FirebaseFirestore.getInstance()
                    .collection("events").document(eventId)
                    .collection("rooms").document(room.id)
                    .update("assignments", updated)
                    .await()
                ActivityLogger.log(eventId, "room", context.getString(R.string.activity_joined_room, room.name))
                snackbarHostState.showSnackbar(context.getString(R.string.rooms_joined, room.name))
                loadRooms()
            } catch (_: Exception) { }
        }
    }

    fun handleLeaveRoom(room: EventRoom) {
        // Leave this room
        scope.launch {
            try {
                val updated = room.assignments - currentUserId
                FirebaseFirestore.getInstance()
                    .collection("events").document(eventId)
                    .collection("rooms").document(room.id)
                    .update("assignments", updated)
                    .await()
                ActivityLogger.log(eventId, "room", context.getString(R.string.activity_joined_room, room.name))
                snackbarHostState.showSnackbar(context.getString(R.string.rooms_left, room.name))
                loadRooms()
            } catch (_: Exception) { }
        }
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

    if (showColorPicker) {
        ModuleColorPickerDialog(
            currentColor = AccentGold,
            onColorSelected = { option ->
                showColorPicker = false
                scope.launch {
                    EventRepository(context).updateModuleColor(eventId, "ROOMS", option.hex)
                }
            },
            onDismiss = { showColorPicker = false }
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
                                        removeModuleFromEvent(eventId, "ROOMS")
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                        val isInAnotherRoom = rooms.any { it.id != room.id && currentUserId in it.assignments }
                        RoomCard(
                            room = room,
                            userNames = userNames,
                            currentUserId = currentUserId,
                            isInAnotherRoom = isInAnotherRoom,
                            onJoin = { handleJoinRoom(room) },
                            onLeave = { handleLeaveRoom(room) },
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
private fun RoomCard(
    room: EventRoom,
    userNames: Map<String, String>,
    currentUserId: String,
    isInAnotherRoom: Boolean,
    onJoin: () -> Unit,
    onLeave: () -> Unit,
    onDelete: () -> Unit
) {
    val isInThisRoom = currentUserId in room.assignments
    val isFull = room.assignments.size >= room.capacity

    BetterMingleCard {
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
                    color = if (isFull) AccentOrange else MaterialTheme.colorScheme.onSurfaceVariant
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (room.assignments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    room.assignments.forEach { userId ->
                        val isMe = userId == currentUserId
                        val chipColor = if (isMe) AccentOrange else PrimaryBlue
                        Box(
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.extraSmall)
                                .background(chipColor.copy(alpha = 0.1f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = chipColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = userNames[userId] ?: userId.take(8),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = chipColor,
                                    fontWeight = if (isMe) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            // Action button
            Spacer(modifier = Modifier.height(Spacing.md))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                when {
                    isInThisRoom -> {
                        BetterMingleOutlinedButton(
                            text = stringResource(R.string.rooms_leave_button),
                            onClick = onLeave
                        )
                    }
                    isInAnotherRoom || isFull -> {
                        BetterMingleButton(
                            text = stringResource(R.string.rooms_join_button),
                            onClick = {},
                            enabled = false
                        )
                    }
                    else -> {
                        BetterMingleButton(
                            text = stringResource(R.string.rooms_join_button),
                            onClick = onJoin,
                            isCta = true
                        )
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

