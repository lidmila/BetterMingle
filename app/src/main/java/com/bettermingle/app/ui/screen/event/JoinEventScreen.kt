package com.bettermingle.app.ui.screen.event

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.bettermingle.app.ui.component.BetterMingleButton
import com.bettermingle.app.ui.component.BetterMingleCard
import com.bettermingle.app.ui.component.EmptyState
import com.bettermingle.app.ui.theme.PrimaryBlue
import com.bettermingle.app.ui.theme.Spacing
import com.bettermingle.app.ui.theme.TextSecondary
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinEventScreen(
    inviteCode: String,
    onNavigateBack: () -> Unit,
    onJoined: (String) -> Unit
) {
    var eventId by remember { mutableStateOf<String?>(null) }
    var eventName by remember { mutableStateOf("") }
    var eventDescription by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isJoining by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(inviteCode) {
        try {
            val snapshot = FirebaseFirestore.getInstance()
                .collection("events")
                .whereEqualTo("inviteCode", inviteCode)
                .limit(1)
                .get().await()

            if (snapshot.documents.isNotEmpty()) {
                val doc = snapshot.documents[0]
                eventId = doc.id
                eventName = doc.getString("name") ?: ""
                eventDescription = doc.getString("description") ?: ""
            } else {
                errorMessage = "Pozvánka nebyla nalezena nebo je neplatná."
            }
        } catch (_: Exception) {
            errorMessage = "Nepodařilo se načíst pozvánku."
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pozvánka", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
                errorMessage != null -> {
                    EmptyState(
                        icon = Icons.Default.Celebration,
                        title = "Chyba",
                        description = errorMessage ?: "",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    BetterMingleCard {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = eventName,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )

                            if (eventDescription.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(Spacing.sm))
                                Text(
                                    text = eventDescription,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Spacer(modifier = Modifier.height(Spacing.lg))

                            BetterMingleButton(
                                text = if (isJoining) "Připojuji se..." else "Připojit se",
                                onClick = {
                                    val eid = eventId ?: return@BetterMingleButton
                                    isJoining = true
                                    scope.launch {
                                        try {
                                            val currentUser = FirebaseAuth.getInstance().currentUser
                                                ?: return@launch
                                            val participantData = hashMapOf(
                                                "userId" to currentUser.uid,
                                                "displayName" to (currentUser.displayName ?: ""),
                                                "avatarUrl" to (currentUser.photoUrl?.toString() ?: ""),
                                                "role" to "PARTICIPANT",
                                                "rsvp" to "ACCEPTED",
                                                "joinedAt" to System.currentTimeMillis()
                                            )
                                            FirebaseFirestore.getInstance()
                                                .collection("events").document(eid)
                                                .collection("participants")
                                                .document(currentUser.uid)
                                                .set(participantData)
                                                .await()
                                            onJoined(eid)
                                        } catch (_: Exception) {
                                            isJoining = false
                                            errorMessage = "Nepodařilo se připojit k akci."
                                        }
                                    }
                                },
                                enabled = !isJoining,
                                isCta = true
                            )
                        }
                    }
                }
            }
        }
    }
}
