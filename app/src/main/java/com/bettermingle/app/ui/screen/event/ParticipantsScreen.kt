package com.bettermingle.app.ui.screen.event

import com.bettermingle.app.R
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
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
import android.content.Intent
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bettermingle.app.data.model.Participant
import com.bettermingle.app.data.model.ParticipantRole
import com.bettermingle.app.data.model.RsvpStatus
import com.bettermingle.app.data.model.UserProfile
import com.bettermingle.app.ui.component.BetterMingleCard
import com.bettermingle.app.ui.component.EmptyState
import com.bettermingle.app.ui.component.UserAvatar
import com.bettermingle.app.ui.theme.AccentGold
import com.bettermingle.app.ui.theme.AccentOrange
import com.bettermingle.app.ui.theme.AccentPink
import com.bettermingle.app.ui.theme.PrimaryBlue
import com.bettermingle.app.ui.theme.Spacing
import com.bettermingle.app.ui.theme.Success
import com.bettermingle.app.ui.theme.TextOnColor

import androidx.compose.runtime.collectAsState
import com.bettermingle.app.data.ads.AdManager
import com.bettermingle.app.data.preferences.SettingsManager
import com.bettermingle.app.ui.component.NativeAdCard
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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
    val settingsManager = remember { SettingsManager(context) }
    val settings by settingsManager.settingsFlow.collectAsState(initial = null)
    val showAds = settings?.let { AdManager.hasAds(it.premiumTier) } ?: false

    // Bottom sheet state for participant profile
    var selectedProfile by remember { mutableStateOf<UserProfile?>(null) }
    var selectedCustomRole by remember { mutableStateOf("") }
    var isLoadingProfile by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    fun loadParticipants() {
        scope.launch {
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
                        customRole = data["customRole"] as? String ?: "",
                        rsvp = rsvp,
                        joinedAt = (data["joinedAt"] as? Number)?.toLong() ?: 0
                    )
                }.sortedBy { if (it.role == ParticipantRole.ORGANIZER) 0 else 1 }

                participants.clear()
                participants.addAll(loaded)
            } catch (_: Exception) { }
        }
    }

    LaunchedEffect(eventId) { loadParticipants() }

    // Profile bottom sheet
    if (selectedProfile != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedProfile = null },
            sheetState = sheetState
        ) {
            ParticipantProfileSheet(
                profile = selectedProfile!!,
                customRole = selectedCustomRole
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.participants_title), style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val shareText = context.getString(R.string.participants_share_text, eventName, "https://bettermingle.app/invite/$inviteCode")
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(intent, context.getString(R.string.participants_share_title)))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = stringResource(R.string.participants_share_title))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val shareText = context.getString(R.string.participants_share_text, eventName, "https://bettermingle.app/invite/$inviteCode")
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(intent, context.getString(R.string.participants_invite_title)))
                },
                containerColor = Color.Transparent,
                contentColor = TextOnColor,
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                modifier = Modifier
                    .shadow(8.dp, CircleShape, ambientColor = AccentOrange.copy(alpha = 0.3f), spotColor = AccentOrange.copy(alpha = 0.3f))
                    .background(AccentOrange, CircleShape)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = stringResource(R.string.participants_add))
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                loadParticipants()
                scope.launch {
                    kotlinx.coroutines.delay(500)
                    isRefreshing = false
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
        if (participants.isEmpty() && !isRefreshing) {
            EmptyState(
                icon = Icons.Default.People,
                illustration = R.drawable.il_empty_participants,
                title = stringResource(R.string.participants_empty_title),
                description = stringResource(R.string.participants_empty_description),
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(Spacing.screenPadding),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                items(participants, key = { it.id }) { participant ->
                    ParticipantItem(
                        participant = participant,
                        onClick = {
                            if (participant.userId.isNotEmpty()) {
                                isLoadingProfile = true
                                selectedCustomRole = participant.customRole
                                scope.launch {
                                    try {
                                        val doc = FirebaseFirestore.getInstance()
                                            .collection("users").document(participant.userId)
                                            .get().await()
                                        if (doc.exists()) {
                                            selectedProfile = UserProfile(
                                                id = participant.userId,
                                                displayName = doc.getString("displayName") ?: participant.displayName,
                                                email = doc.getString("email") ?: "",
                                                avatarUrl = doc.getString("avatarUrl") ?: "",
                                                phone = doc.getString("phone") ?: "",
                                                contactEmail = doc.getString("contactEmail") ?: "",
                                                department = doc.getString("department") ?: "",
                                                bio = doc.getString("bio") ?: ""
                                            )
                                        }
                                    } catch (_: Exception) { }
                                    isLoadingProfile = false
                                }
                            }
                        }
                    )
                }

                if (showAds && participants.isNotEmpty()) {
                    item(key = "native_ad") {
                        NativeAdCard(
                            modifier = Modifier.padding(vertical = Spacing.sm)
                        )
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun ParticipantItem(
    participant: Participant,
    onClick: () -> Unit
) {
    BetterMingleCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                Row {
                    Text(
                        text = when (participant.role) {
                            ParticipantRole.ORGANIZER -> stringResource(R.string.participants_role_organizer)
                            ParticipantRole.CO_ORGANIZER -> stringResource(R.string.participants_role_co_organizer)
                            ParticipantRole.PARTICIPANT -> stringResource(R.string.participants_role_participant)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (participant.customRole.isNotEmpty()) {
                        Text(
                            text = " · ${participant.customRole}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AccentPink,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            RsvpBadge(status = participant.rsvp)
        }
    }
}

@Composable
private fun ParticipantProfileSheet(
    profile: UserProfile,
    customRole: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar
        UserAvatar(
            avatarUrl = profile.avatarUrl,
            displayName = profile.displayName,
            size = 80.dp
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        Text(
            text = profile.displayName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        if (customRole.isNotEmpty()) {
            Text(
                text = customRole,
                style = MaterialTheme.typography.bodyMedium,
                color = AccentPink
            )
        }

        if (profile.bio.isNotEmpty()) {
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = profile.bio,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        // Contact info
        val hasInfo = profile.contactEmail.isNotEmpty() || profile.phone.isNotEmpty()
                || profile.department.isNotEmpty() || profile.email.isNotEmpty()

        if (hasInfo) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                if (profile.email.isNotEmpty()) {
                    ProfileInfoRow(Icons.Default.Email, stringResource(R.string.participants_email_label), profile.email)
                }
                if (profile.contactEmail.isNotEmpty()) {
                    ProfileInfoRow(Icons.Default.Email, stringResource(R.string.participants_contact_email_label), profile.contactEmail)
                }
                if (profile.phone.isNotEmpty()) {
                    ProfileInfoRow(Icons.Default.Phone, stringResource(R.string.participants_phone_label), profile.phone)
                }
                if (profile.department.isNotEmpty()) {
                    ProfileInfoRow(Icons.Default.Business, stringResource(R.string.participants_department_label), profile.department)
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.lg))
    }
}

@Composable
private fun ProfileInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PrimaryBlue,
            modifier = Modifier.size(Spacing.iconSM)
        )
        Spacer(modifier = Modifier.width(Spacing.sm))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun RsvpBadge(status: RsvpStatus) {
    val (text, color) = when (status) {
        RsvpStatus.ACCEPTED -> stringResource(R.string.participants_rsvp_accepted) to Success
        RsvpStatus.DECLINED -> stringResource(R.string.participants_rsvp_declined) to AccentOrange
        RsvpStatus.MAYBE -> stringResource(R.string.participants_rsvp_maybe) to AccentGold
        RsvpStatus.PENDING -> stringResource(R.string.participants_rsvp_pending) to MaterialTheme.colorScheme.onSurfaceVariant
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
