package com.bettermingle.app.ui.screen.event

import com.bettermingle.app.R
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bettermingle.app.data.model.CarpoolPassenger
import com.bettermingle.app.data.model.CarpoolRide
import com.bettermingle.app.data.model.CarpoolType
import com.bettermingle.app.data.model.PassengerStatus
import com.bettermingle.app.ui.component.BetterMingleButton
import com.bettermingle.app.ui.component.BetterMingleCard
import com.bettermingle.app.ui.component.BetterMingleTextField
import com.bettermingle.app.ui.component.EmptyState
import com.bettermingle.app.ui.component.PlacesAutocompleteField
import com.bettermingle.app.ui.theme.AccentOrange
import com.bettermingle.app.ui.theme.AccentPink
import com.bettermingle.app.ui.theme.Error
import com.bettermingle.app.ui.theme.PrimaryBlue
import com.bettermingle.app.ui.theme.Spacing
import com.bettermingle.app.ui.theme.Success
import com.bettermingle.app.ui.theme.TextOnColor

import com.bettermingle.app.utils.DateFormatUtils
import com.bettermingle.app.utils.ActivityLogger
import com.bettermingle.app.utils.removeModuleFromEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarpoolScreen(
    eventId: String,
    onNavigateBack: () -> Unit
) {
    val rides = remember { mutableStateListOf<CarpoolRide>() }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.carpool_tab_offers),
        stringResource(R.string.carpool_tab_requests)
    )
    var showCreateDialog by remember { mutableStateOf(false) }
    var rideToDelete by remember { mutableStateOf<CarpoolRide?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var isOrganizer by remember { mutableStateOf(false) }

    LaunchedEffect(eventId) {
        try {
            val eventDoc = FirebaseFirestore.getInstance()
                .collection("events").document(eventId).get().await()
            isOrganizer = eventDoc.getString("createdBy") == currentUserId
        } catch (_: Exception) { }
    }

    fun loadRides() {
        scope.launch {
            try {
                val firestore = FirebaseFirestore.getInstance()
                val snapshot = firestore.collection("events").document(eventId)
                    .collection("carpoolRides").get().await()

                val driverIds = snapshot.documents.mapNotNull { it.getString("driverId") }.distinct()
                val driverNames = mutableMapOf<String, String>()
                for (uid in driverIds) {
                    try {
                        val userDoc = firestore.collection("users").document(uid).get().await()
                        driverNames[uid] = userDoc.getString("displayName") ?: uid.take(8)
                    } catch (_: Exception) {
                        driverNames[uid] = uid.take(8)
                    }
                }

                val loaded = snapshot.documents.map { doc ->
                    val data = doc.data ?: emptyMap()
                    val driverId = data["driverId"] as? String ?: ""
                    val typeStr = data["type"] as? String ?: "OFFER"
                    CarpoolRide(
                        id = doc.id,
                        eventId = eventId,
                        driverId = driverId,
                        driverName = driverNames[driverId] ?: driverId.take(8),
                        departureLocation = data["departureLocation"] as? String ?: "",
                        departureLat = (data["departureLat"] as? Number)?.toDouble(),
                        departureLng = (data["departureLng"] as? Number)?.toDouble(),
                        departureTime = (data["departureTime"] as? Number)?.toLong(),
                        availableSeats = (data["availableSeats"] as? Number)?.toInt() ?: 4,
                        notes = data["notes"] as? String ?: "",
                        type = try { CarpoolType.valueOf(typeStr) } catch (_: Exception) { CarpoolType.OFFER },
                        isClosed = data["isClosed"] as? Boolean ?: false
                    )
                }.sortedBy { it.departureTime }

                rides.clear()
                rides.addAll(loaded)
            } catch (_: Exception) {
                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.error_load_failed)) }
            }
        }
    }

    LaunchedEffect(eventId) { loadRides() }

    val filteredRides = rides.filter {
        if (selectedTab == 0) it.type == CarpoolType.OFFER else it.type == CarpoolType.REQUEST
    }

    if (showCreateDialog) {
        CreateRideDialog(
            eventId = eventId,
            type = if (selectedTab == 0) CarpoolType.OFFER else CarpoolType.REQUEST,
            onDismiss = { showCreateDialog = false },
            onCreated = {
                showCreateDialog = false
                loadRides()
            }
        )
    }

    if (rideToDelete != null) {
        AlertDialog(
            onDismissRequest = { rideToDelete = null },
            title = { Text(stringResource(R.string.carpool_delete_title)) },
            text = { Text(stringResource(R.string.carpool_delete_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    val ride = rideToDelete!!
                    rideToDelete = null
                    scope.launch {
                        try {
                            FirebaseFirestore.getInstance()
                                .collection("events").document(eventId)
                                .collection("carpoolRides").document(ride.id)
                                .delete().await()
                            rides.removeAll { it.id == ride.id }
                            ActivityLogger.log(eventId, "carpool", context.getString(R.string.activity_deleted_ride, ride.departureLocation))
                        } catch (_: Exception) {
                            scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.error_delete_failed)) }
                        }
                    }
                }) {
                    Text(stringResource(R.string.common_delete), color = AccentOrange)
                }
            },
            dismissButton = {
                TextButton(onClick = { rideToDelete = null }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.carpool_title), style = MaterialTheme.typography.titleMedium) },
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
                                text = { Text(stringResource(R.string.dashboard_remove_module)) },
                                onClick = {
                                    menuExpanded = false
                                    scope.launch {
                                        removeModuleFromEvent(eventId, "CARPOOL")
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
                Icon(Icons.Default.Add, contentDescription = if (selectedTab == 0) stringResource(R.string.carpool_offer_ride) else stringResource(R.string.carpool_request_ride))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = PrimaryBlue,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = PrimaryBlue
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        selectedContentColor = PrimaryBlue,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    loadRides()
                    scope.launch {
                        kotlinx.coroutines.delay(500)
                        isRefreshing = false
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) {
                if (filteredRides.isEmpty() && !isRefreshing) {
                    EmptyState(
                        icon = Icons.Default.DirectionsCar,
                        illustration = R.drawable.il_empty_carpool,
                        title = if (selectedTab == 0) stringResource(R.string.carpool_empty_offers_title) else stringResource(R.string.carpool_empty_requests_title),
                        description = if (selectedTab == 0)
                            stringResource(R.string.carpool_empty_offers_desc)
                        else
                            stringResource(R.string.carpool_empty_requests_desc),
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(Spacing.screenPadding),
                        verticalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        items(filteredRides, key = { it.id }) { ride ->
                            CarpoolRideCard(
                                ride = ride,
                                eventId = eventId,
                                currentUserId = currentUserId,
                                onClose = {
                                    scope.launch {
                                        try {
                                            FirebaseFirestore.getInstance()
                                                .collection("events").document(eventId)
                                                .collection("carpoolRides").document(ride.id)
                                                .update("isClosed", true).await()
                                            loadRides()
                                        } catch (_: Exception) { }
                                    }
                                },
                                onDelete = { rideToDelete = ride },
                                onRequestRide = {
                                    scope.launch {
                                        try {
                                            val currentUser = FirebaseAuth.getInstance().currentUser
                                            val passengerData = hashMapOf(
                                                "userId" to (currentUser?.uid ?: ""),
                                                "displayName" to (currentUser?.displayName ?: ""),
                                                "status" to "PENDING",
                                                "pickupLocation" to "",
                                                "createdAt" to System.currentTimeMillis()
                                            )
                                            FirebaseFirestore.getInstance()
                                                .collection("events").document(eventId)
                                                .collection("carpoolRides").document(ride.id)
                                                .collection("passengers")
                                                .add(passengerData).await()
                                            ActivityLogger.log(eventId, "carpool", context.getString(R.string.activity_requested_ride, ride.departureLocation))
                                            loadRides()
                                        } catch (_: Exception) {
                                            scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.error_save_failed)) }
                                        }
                                    }
                                },
                                onPassengerUpdated = { loadRides() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CarpoolRideCard(
    ride: CarpoolRide,
    eventId: String,
    currentUserId: String,
    onClose: () -> Unit,
    onDelete: () -> Unit,
    onRequestRide: () -> Unit,
    onPassengerUpdated: () -> Unit
) {
    val isOwner = ride.driverId == currentUserId
    val passengers = remember { mutableStateListOf<CarpoolPassenger>() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(ride.id) {
        try {
            val snapshot = FirebaseFirestore.getInstance()
                .collection("events").document(eventId)
                .collection("carpoolRides").document(ride.id)
                .collection("passengers").get().await()
            passengers.clear()
            passengers.addAll(snapshot.documents.map { doc ->
                val data = doc.data ?: emptyMap()
                CarpoolPassenger(
                    id = doc.id,
                    rideId = ride.id,
                    userId = data["userId"] as? String ?: "",
                    displayName = data["displayName"] as? String ?: "",
                    status = try { PassengerStatus.valueOf((data["status"] as? String ?: "PENDING").uppercase()) } catch (_: Exception) { PassengerStatus.PENDING },
                    pickupLocation = data["pickupLocation"] as? String ?: ""
                )
            })
        } catch (_: Exception) { }
    }

    BetterMingleCard {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = ride.driverName.ifEmpty { if (ride.type == CarpoolType.OFFER) stringResource(R.string.carpool_driver) else stringResource(R.string.carpool_requester) },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                if (ride.isClosed) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Block,
                            contentDescription = null,
                            tint = Error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.carpool_closed),
                            style = MaterialTheme.typography.bodySmall,
                            color = Error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else if (ride.type == CarpoolType.OFFER) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.EventSeat,
                            contentDescription = null,
                            tint = Success,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${ride.availableSeats} ${stringResource(R.string.carpool_seats_count)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Success
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            if (ride.departureLocation.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = ride.departureLocation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            ride.departureTime?.let { time ->
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = DateFormatUtils.formatDateTime(time),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (ride.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = ride.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Passengers section
            if (passengers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = stringResource(R.string.carpool_passengers),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                passengers.forEach { passenger ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = passenger.displayName.ifEmpty { passenger.userId.take(8) },
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        val statusColor = when (passenger.status) {
                            PassengerStatus.APPROVED -> Success
                            PassengerStatus.REJECTED -> Error
                            PassengerStatus.PENDING -> AccentPink
                        }
                        val statusText = when (passenger.status) {
                            PassengerStatus.APPROVED -> stringResource(R.string.carpool_approved)
                            PassengerStatus.REJECTED -> stringResource(R.string.carpool_rejected)
                            PassengerStatus.PENDING -> stringResource(R.string.carpool_pending)
                        }
                        if (isOwner && passenger.status == PassengerStatus.PENDING) {
                            IconButton(onClick = {
                                scope.launch {
                                    try {
                                        FirebaseFirestore.getInstance()
                                            .collection("events").document(eventId)
                                            .collection("carpoolRides").document(ride.id)
                                            .collection("passengers").document(passenger.id)
                                            .update("status", "APPROVED").await()
                                        val idx = passengers.indexOfFirst { it.id == passenger.id }
                                        if (idx >= 0) passengers[idx] = passenger.copy(status = PassengerStatus.APPROVED)
                                        onPassengerUpdated()
                                    } catch (_: Exception) { }
                                }
                            }) {
                                Icon(Icons.Default.Check, contentDescription = stringResource(R.string.carpool_approve), tint = Success, modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = {
                                scope.launch {
                                    try {
                                        FirebaseFirestore.getInstance()
                                            .collection("events").document(eventId)
                                            .collection("carpoolRides").document(ride.id)
                                            .collection("passengers").document(passenger.id)
                                            .update("status", "REJECTED").await()
                                        val idx = passengers.indexOfFirst { it.id == passenger.id }
                                        if (idx >= 0) passengers[idx] = passenger.copy(status = PassengerStatus.REJECTED)
                                        onPassengerUpdated()
                                    } catch (_: Exception) { }
                                }
                            }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.carpool_reject), tint = Error, modifier = Modifier.size(20.dp))
                            }
                        } else {
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor
                            )
                        }
                    }
                }
            }

            // Action buttons
            if (!ride.isClosed) {
                Spacer(modifier = Modifier.height(Spacing.md))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isOwner) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.common_delete), tint = AccentOrange)
                        }
                        BetterMingleButton(
                            text = stringResource(R.string.carpool_close_button),
                            onClick = onClose
                        )
                    } else if (ride.type == CarpoolType.OFFER) {
                        BetterMingleButton(
                            text = stringResource(R.string.carpool_request_button),
                            onClick = onRequestRide,
                            isCta = true
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateRideDialog(
    eventId: String,
    type: CarpoolType,
    onDismiss: () -> Unit,
    onCreated: () -> Unit
) {
    var departureLocation by remember { mutableStateOf("") }
    var departureTimeMillis by remember { mutableStateOf<Long?>(null) }
    var availableSeats by remember { mutableStateOf("4") }
    var notes by remember { mutableStateOf("") }
    var showTimePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // TimePicker dialog
    if (showTimePicker) {
        val cal = java.util.Calendar.getInstance().apply {
            if (departureTimeMillis != null) timeInMillis = departureTimeMillis!!
        }
        val timePickerState = rememberTimePickerState(
            initialHour = cal.get(java.util.Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(java.util.Calendar.MINUTE),
            is24Hour = true
        )
        Dialog(
            onDismissRequest = { showTimePicker = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 6.dp,
                modifier = Modifier.padding(horizontal = Spacing.lg)
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.carpool_select_time),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = Spacing.lg)
                    )
                    TimePicker(state = timePickerState)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.common_cancel)) }
                        TextButton(onClick = {
                            val calendar = java.util.Calendar.getInstance().apply {
                                set(java.util.Calendar.HOUR_OF_DAY, timePickerState.hour)
                                set(java.util.Calendar.MINUTE, timePickerState.minute)
                                set(java.util.Calendar.SECOND, 0)
                                set(java.util.Calendar.MILLISECOND, 0)
                            }
                            departureTimeMillis = calendar.timeInMillis
                            showTimePicker = false
                        }) { Text(stringResource(R.string.common_confirm)) }
                    }
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier.padding(horizontal = Spacing.lg)
        ) {
            Column(modifier = Modifier.padding(Spacing.lg)) {
                Text(
                    if (type == CarpoolType.OFFER) stringResource(R.string.carpool_dialog_offer_title) else stringResource(R.string.carpool_dialog_request_title),
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(Spacing.lg))

                PlacesAutocompleteField(
                    value = departureLocation,
                    onValueChange = { departureLocation = it },
                    onPlaceSelected = { place -> departureLocation = place.name },
                    label = stringResource(R.string.carpool_departure_location)
                )

                Spacer(modifier = Modifier.height(Spacing.formFieldSpacing))

                Box(modifier = Modifier.clickable { showTimePicker = true }) {
                    BetterMingleTextField(
                        value = departureTimeMillis?.let { DateFormatUtils.formatTime(it) } ?: "",
                        onValueChange = {},
                        label = stringResource(R.string.carpool_departure_time),
                        enabled = false,
                        leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null) }
                    )
                }

                if (type == CarpoolType.OFFER) {
                    Spacer(modifier = Modifier.height(Spacing.formFieldSpacing))

                    BetterMingleTextField(
                        value = availableSeats,
                        onValueChange = { availableSeats = it },
                        label = stringResource(R.string.carpool_available_seats),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.formFieldSpacing))

                BetterMingleTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = stringResource(R.string.carpool_notes),
                    singleLine = false,
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(Spacing.lg))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
                    TextButton(
                        onClick = {
                            scope.launch {
                                try {
                                    val currentUser = FirebaseAuth.getInstance().currentUser
                                    val rideData = hashMapOf<String, Any?>(
                                        "driverId" to (currentUser?.uid ?: ""),
                                        "departureLocation" to departureLocation,
                                        "departureTime" to departureTimeMillis,
                                        "availableSeats" to (availableSeats.toIntOrNull() ?: 4),
                                        "notes" to notes,
                                        "type" to type.name,
                                        "isClosed" to false,
                                        "createdAt" to System.currentTimeMillis()
                                    )
                                    FirebaseFirestore.getInstance()
                                        .collection("events").document(eventId)
                                        .collection("carpoolRides")
                                        .add(rideData).await()
                                    val typeLabel = if (type.name == "OFFER") context.getString(R.string.activity_offered_ride) else context.getString(R.string.activity_looking_for_ride)
                                    val seatsInfo = availableSeats.toIntOrNull()?.let { " ($it ${context.getString(R.string.carpool_seats_count)})" } ?: ""
                                    ActivityLogger.log(eventId, "carpool", "$typeLabel z $departureLocation$seatsInfo")
                                    onCreated()
                                } catch (_: Exception) { }
                            }
                        },
                        enabled = departureLocation.isNotBlank()
                    ) {
                        Text(stringResource(R.string.common_create))
                    }
                }
            }
        }
    }
}
