package com.bettermingle.app.ui.screen.event

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.bettermingle.app.ui.theme.AccentOrange
import com.bettermingle.app.ui.theme.AccentPink
import com.bettermingle.app.ui.theme.Error
import com.bettermingle.app.ui.theme.PrimaryBlue
import com.bettermingle.app.ui.theme.Spacing
import com.bettermingle.app.ui.theme.Success
import com.bettermingle.app.ui.theme.TextOnColor
import com.bettermingle.app.ui.theme.TextSecondary
import com.bettermingle.app.utils.DateFormatUtils
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
    val tabs = listOf("Nabídky", "Poptávky")
    var showCreateDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

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
            } catch (_: Exception) { }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Spolujízda", style = MaterialTheme.typography.titleMedium) },
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
                Icon(Icons.Default.Add, contentDescription = if (selectedTab == 0) "Nabídnout jízdu" else "Poptat jízdu")
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
                        unselectedContentColor = TextSecondary
                    )
                }
            }

            if (filteredRides.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.DirectionsCar,
                    title = if (selectedTab == 0) "Zatím žádné nabídky" else "Zatím žádné poptávky",
                    description = if (selectedTab == 0)
                        "Nabídni spolujízdu ostatním účastníkům."
                    else
                        "Poptej jízdu od ostatních účastníků.",
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
                                        loadRides()
                                    } catch (_: Exception) { }
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

@Composable
private fun CarpoolRideCard(
    ride: CarpoolRide,
    eventId: String,
    currentUserId: String,
    onClose: () -> Unit,
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
                    text = ride.driverName.ifEmpty { if (ride.type == CarpoolType.OFFER) "Řidič" else "Žadatel" },
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
                            text = "Uzavřeno",
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
                            text = "${ride.availableSeats} míst",
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
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = ride.departureLocation,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            ride.departureTime?.let { time ->
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = DateFormatUtils.formatDateTime(time),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            if (ride.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = ride.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            // Passengers section
            if (passengers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = "Spolujezdci",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
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
                            PassengerStatus.APPROVED -> "Schváleno"
                            PassengerStatus.REJECTED -> "Zamítnuto"
                            PassengerStatus.PENDING -> "Čeká"
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
                                Icon(Icons.Default.Check, contentDescription = "Schválit", tint = Success, modifier = Modifier.size(20.dp))
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
                                Icon(Icons.Default.Close, contentDescription = "Zamítnout", tint = Error, modifier = Modifier.size(20.dp))
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
                        BetterMingleButton(
                            text = "Uzavřít",
                            onClick = onClose
                        )
                    } else if (ride.type == CarpoolType.OFFER) {
                        BetterMingleButton(
                            text = "Požádat o spolujízdu",
                            onClick = onRequestRide,
                            isCta = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateRideDialog(
    eventId: String,
    type: CarpoolType,
    onDismiss: () -> Unit,
    onCreated: () -> Unit
) {
    var departureLocation by remember { mutableStateOf("") }
    var departureTime by remember { mutableStateOf("") }
    var availableSeats by remember { mutableStateOf("4") }
    var notes by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (type == CarpoolType.OFFER) "Nabídnout jízdu" else "Poptat jízdu"
            )
        },
        text = {
            Column {
                BetterMingleTextField(
                    value = departureLocation,
                    onValueChange = { departureLocation = it },
                    label = "Místo odjezdu"
                )

                Spacer(modifier = Modifier.height(Spacing.formFieldSpacing))

                BetterMingleTextField(
                    value = departureTime,
                    onValueChange = { departureTime = it },
                    label = "Čas odjezdu (např. 15:00)"
                )

                if (type == CarpoolType.OFFER) {
                    Spacer(modifier = Modifier.height(Spacing.formFieldSpacing))

                    BetterMingleTextField(
                        value = availableSeats,
                        onValueChange = { availableSeats = it },
                        label = "Počet volných míst",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.formFieldSpacing))

                BetterMingleTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = "Poznámky",
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
                            val currentUser = FirebaseAuth.getInstance().currentUser
                            // Parse departure time "HH:mm" to epoch
                            val parsedTime = departureTime.trim().split(":").let { parts ->
                                if (parts.size == 2) {
                                    val h = parts[0].toIntOrNull()
                                    val m = parts[1].toIntOrNull()
                                    if (h != null && m != null && h in 0..23 && m in 0..59) {
                                        java.util.Calendar.getInstance().apply {
                                            set(java.util.Calendar.HOUR_OF_DAY, h)
                                            set(java.util.Calendar.MINUTE, m)
                                            set(java.util.Calendar.SECOND, 0)
                                            set(java.util.Calendar.MILLISECOND, 0)
                                        }.timeInMillis
                                    } else null
                                } else null
                            }
                            val rideData = hashMapOf<String, Any?>(
                                "driverId" to (currentUser?.uid ?: ""),
                                "departureLocation" to departureLocation,
                                "departureTime" to parsedTime,
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
                            onCreated()
                        } catch (_: Exception) { }
                    }
                },
                enabled = departureLocation.isNotBlank()
            ) {
                Text("Vytvořit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Zrušit") }
        }
    )
}
