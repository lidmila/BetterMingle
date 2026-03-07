package com.bettermingle.app.ui.screen.event

import com.bettermingle.app.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.bettermingle.app.ui.component.BetterMingleButton
import com.bettermingle.app.ui.component.BetterMingleCard
import com.bettermingle.app.ui.component.EmptyState
import com.bettermingle.app.ui.theme.PrimaryBlue
import com.bettermingle.app.ui.theme.Spacing

import com.bettermingle.app.utils.ActivityLogger
import com.bettermingle.app.data.preferences.PremiumTier
import com.bettermingle.app.data.preferences.SettingsManager
import com.bettermingle.app.data.preferences.TierLimits
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinEventScreen(
    inviteCode: String,
    onNavigateBack: () -> Unit,
    onJoined: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
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
                errorMessage = context.getString(R.string.join_error_not_found)
            }
        } catch (_: Exception) {
            errorMessage = context.getString(R.string.join_error_load_failed)
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.join_title), style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
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
                        icon = Icons.Default.ErrorOutline,
                        title = stringResource(R.string.join_error_title),
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
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Spacer(modifier = Modifier.height(Spacing.lg))

                            BetterMingleButton(
                                text = if (isJoining) stringResource(R.string.join_joining) else stringResource(R.string.join_button),
                                onClick = {
                                    val eid = eventId ?: return@BetterMingleButton
                                    isJoining = true
                                    scope.launch {
                                        try {
                                            // Check participant limit
                                            val settingsManager = SettingsManager(context)
                                            val settings = settingsManager.settingsFlow.first()
                                            val tier = settings.premiumTier
                                            val maxParticipants = TierLimits.maxParticipants(tier)

                                            val participantsSnapshot = FirebaseFirestore.getInstance()
                                                .collection("events").document(eid)
                                                .collection("participants")
                                                .get().await()
                                            val currentParticipantCount = participantsSnapshot.size()

                                            if (currentParticipantCount >= maxParticipants) {
                                                isJoining = false
                                                val tierLabel = when (tier) {
                                                    com.bettermingle.app.data.preferences.PremiumTier.FREE -> context.getString(R.string.join_tier_free)
                                                    com.bettermingle.app.data.preferences.PremiumTier.PRO -> context.getString(R.string.join_tier_pro)
                                                    com.bettermingle.app.data.preferences.PremiumTier.BUSINESS -> context.getString(R.string.join_tier_business)
                                                }
                                                errorMessage = context.getString(R.string.join_participant_limit, maxParticipants, tierLabel)
                                                return@launch
                                            }

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
                                            ActivityLogger.log(eid, "participant", context.getString(R.string.activity_joined_event), eventName = eventName)
                                            onJoined(eid)
                                        } catch (_: Exception) {
                                            isJoining = false
                                            errorMessage = context.getString(R.string.join_error_failed)
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
