package com.bettermingle.app.ui.screen.event

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Share
import android.content.Intent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bettermingle.app.data.model.Participant
import com.bettermingle.app.data.model.ParticipantRole
import com.bettermingle.app.data.model.RsvpStatus
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
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import android.widget.Toast

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

    LaunchedEffect(eventId) {
        try {
            val eventDoc = FirebaseFirestore.getInstance()
                .collection("events").document(eventId).get().await()
            inviteCode = eventDoc.getString("inviteCode") ?: ""
            eventName = eventDoc.getString("name") ?: ""
        } catch (_: Exception) { }
        try {
            val snapshot = FirebaseFirestore.getInstance()
                .collection("events").document(eventId)
                .collection("participants")
                .get().await()

            // Also load user display names from users collection
            val userIds = snapshot.documents.mapNotNull { it.getString("userId") }.distinct()
            val userNames = mutableMapOf<String, String>()
            for (uid in userIds) {
                try {
                    val userDoc = FirebaseFirestore.getInstance()
                        .collection("users").document(uid).get().await()
                    userNames[uid] = userDoc.getString("displayName") ?: ""
                } catch (_: Exception) { }
            }

            val loaded = snapshot.documents.map { doc ->
                val data = doc.data ?: emptyMap()
                val userId = data["userId"] as? String ?: ""
                val role = try {
                    ParticipantRole.valueOf((data["role"] as? String ?: "PARTICIPANT").uppercase())
                } catch (_: Exception) { ParticipantRole.PARTICIPANT }
                val rsvp = try {
                    RsvpStatus.valueOf((data["rsvp"] as? String ?: "PENDING").uppercase())
                } catch (_: Exception) { RsvpStatus.PENDING }

                Participant(
                    id = doc.id,
                    eventId = eventId,
                    userId = userId,
                    displayName = (data["displayName"] as? String)?.ifEmpty { null }
                        ?: userNames[userId] ?: userId.take(8),
                    avatarUrl = data["avatarUrl"] as? String ?: "",
                    role = role,
                    rsvp = rsvp,
                    joinedAt = (data["joinedAt"] as? Number)?.toLong() ?: 0
                )
            }.sortedBy { if (it.role == ParticipantRole.ORGANIZER) 0 else 1 }

            participants.clear()
            participants.addAll(loaded)
        } catch (_: Exception) { }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Účastníci", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val shareText = "Ahoj! Pojď se připojit k akci \"$eventName\" na BetterMingle: https://bettermingle.app/invite/$inviteCode"
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(intent, "Sdílet pozvánku"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Sdílet pozvánku")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val shareText = "Ahoj! Pojď se připojit k akci \"$eventName\" na BetterMingle: https://bettermingle.app/invite/$inviteCode"
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(intent, "Pozvat účastníka"))
                },
                containerColor = AccentOrange,
                contentColor = TextOnColor
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Přidat účastníka")
            }
        }
    ) { innerPadding ->
        if (participants.isEmpty()) {
            EmptyState(
                icon = Icons.Default.People,
                title = "Zatím žádní účastníci",
                description = "Pozvi kamarády sdílením odkazu na akci.",
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
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                items(participants, key = { it.id }) { participant ->
                    ParticipantItem(participant = participant)
                }
            }
        }
    }
}

@Composable
private fun ParticipantItem(participant: Participant) {
    BetterMingleCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = participant.displayName.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextOnColor
                )
            }

            Spacer(modifier = Modifier.width(Spacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = participant.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = when (participant.role) {
                        ParticipantRole.ORGANIZER -> "Organizátor"
                        ParticipantRole.CO_ORGANIZER -> "Spoluorganizátor"
                        ParticipantRole.PARTICIPANT -> "Účastník"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            RsvpBadge(status = participant.rsvp)
        }
    }
}

@Composable
private fun RsvpBadge(status: RsvpStatus) {
    val (text, color) = when (status) {
        RsvpStatus.ACCEPTED -> "Přijato" to Success
        RsvpStatus.DECLINED -> "Odmítnuto" to AccentOrange
        RsvpStatus.MAYBE -> "Možná" to AccentGold
        RsvpStatus.PENDING -> "Čeká" to TextSecondary
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
