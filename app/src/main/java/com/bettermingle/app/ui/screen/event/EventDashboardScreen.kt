package com.bettermingle.app.ui.screen.event

import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.bettermingle.app.ui.component.CountdownTimer
import com.bettermingle.app.ui.component.FeatureModuleCard
import com.bettermingle.app.ui.theme.AccentGold
import com.bettermingle.app.ui.theme.AccentOrange
import com.bettermingle.app.ui.theme.AccentPink
import com.bettermingle.app.ui.theme.BackgroundSecondary
import com.bettermingle.app.ui.theme.PrimaryBlue
import com.bettermingle.app.ui.theme.BetterMingleMotion
import com.bettermingle.app.ui.theme.Spacing
import com.bettermingle.app.ui.theme.Success
import com.bettermingle.app.ui.theme.TextSecondary
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class ModuleInfo(
    val key: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconTint: Color,
    val badgeCount: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDashboardScreen(
    eventId: String,
    onNavigateBack: () -> Unit,
    onModuleClick: (String) -> Unit
) {
    var eventName by remember { mutableStateOf("") }
    var eventDescription by remember { mutableStateOf("") }
    var eventLocation by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf<Long?>(null) }
    var endDate by remember { mutableStateOf<Long?>(null) }
    var participantCount by remember { mutableStateOf(0) }
    val context = LocalContext.current
    var pollCount by remember { mutableStateOf(0) }
    var expenseTotal by remember { mutableStateOf("") }
    var rideCount by remember { mutableStateOf(0) }
    var roomCount by remember { mutableStateOf(0) }
    var scheduleCount by remember { mutableStateOf(0) }
    var messageCount by remember { mutableStateOf(0) }
    var taskCount by remember { mutableStateOf(0) }
    val badgeCounts = remember { mutableStateMapOf<String, Int>() }

    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }

    LaunchedEffect(eventId) {
        val firestore = FirebaseFirestore.getInstance()
        val eventRef = firestore.collection("events").document(eventId)

        try {
            val eventDoc = eventRef.get().await()
            eventName = eventDoc.getString("name") ?: ""
            eventDescription = eventDoc.getString("description") ?: ""
            eventLocation = eventDoc.getString("locationName") ?: ""
            startDate = (eventDoc.get("startDate") as? Number)?.toLong()
            endDate = (eventDoc.get("endDate") as? Number)?.toLong()
        } catch (_: Exception) { }

        try {
            val parts = eventRef.collection("participants").get().await()
            participantCount = parts.size()
        } catch (_: Exception) { }

        try {
            val polls = eventRef.collection("polls").get().await()
            pollCount = polls.documents.count { !(it.getBoolean("isClosed") ?: false) }
        } catch (_: Exception) { }

        try {
            val expenses = eventRef.collection("expenses").get().await()
            val total = expenses.documents.sumOf { (it.get("amount") as? Number)?.toDouble() ?: 0.0 }
            expenseTotal = if (total > 0) "${String.format("%,.0f", total)} Kč" else ""
        } catch (_: Exception) { }

        try { rideCount = eventRef.collection("carpoolRides").get().await().size() } catch (_: Exception) { }
        try { roomCount = eventRef.collection("rooms").get().await().size() } catch (_: Exception) { }
        try { scheduleCount = eventRef.collection("schedule").get().await().size() } catch (_: Exception) { }
        try { messageCount = eventRef.collection("messages").get().await().size() } catch (_: Exception) { }
        try { taskCount = eventRef.collection("tasks").get().await().size() } catch (_: Exception) { }

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
                    "schedule" to "schedule"
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

    val modules = remember(participantCount, pollCount, expenseTotal, rideCount, roomCount, scheduleCount, messageCount, taskCount, badgeCounts.toMap()) {
        listOf(
            ModuleInfo("voting", "Hlasování", if (pollCount > 0) "$pollCount aktivní" else "", Icons.Default.HowToVote, PrimaryBlue, badgeCounts["voting"] ?: 0),
            ModuleInfo("participants", "Účastníci", if (participantCount > 0) "$participantCount lidí" else "", Icons.Default.People, AccentPink),
            ModuleInfo("expenses", "Výdaje", expenseTotal, Icons.Default.Payments, AccentOrange, badgeCounts["expenses"] ?: 0),
            ModuleInfo("carpool", "Spolujízda", if (rideCount > 0) "$rideCount jízd" else "", Icons.Default.DirectionsCar, Success, badgeCounts["carpool"] ?: 0),
            ModuleInfo("rooms", "Ubytování", if (roomCount > 0) "$roomCount pokojů" else "", Icons.Default.Hotel, AccentGold, badgeCounts["rooms"] ?: 0),
            ModuleInfo("schedule", "Harmonogram", if (scheduleCount > 0) "$scheduleCount bodů" else "", Icons.Default.CalendarMonth, PrimaryBlue, badgeCounts["schedule"] ?: 0),
            ModuleInfo("chat", "Chat", if (messageCount > 0) "$messageCount zpráv" else "", Icons.AutoMirrored.Filled.Chat, AccentOrange, badgeCounts["chat"] ?: 0),
            ModuleInfo("tasks", "Úkoly", if (taskCount > 0) "$taskCount úkolů" else "", Icons.AutoMirrored.Filled.Assignment, AccentPink, badgeCounts["tasks"] ?: 0),
            ModuleInfo("rating", "Hodnocení", "", Icons.Default.Star, AccentGold),
            ModuleInfo("settings", "Nastavení", "", Icons.Default.Settings, TextSecondary)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        eventName.ifEmpty { "Načítám..." },
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
                    }
                },
                actions = {
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
                            Icon(Icons.Default.CalendarToday, contentDescription = "Přidat do kalendáře")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { visible = true }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.screenPadding)
        ) {
            Spacer(modifier = Modifier.height(Spacing.sm))

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

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        BackgroundSecondary,
                        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
                    .padding(horizontal = Spacing.sm, vertical = Spacing.sm)
            ) {
            Text(
                text = "Moduly",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = Spacing.xs)
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(vertical = Spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(modules, key = { _, module -> module.key }) { index, module ->
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(BetterMingleMotion.STANDARD, delayMillis = 100 + index * 40)) +
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
                            onClick = { handleModuleClick(module.key) }
                        )
                    }
                }
            }
            }
        }
    }
}
