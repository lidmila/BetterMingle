package com.bettermingle.app.ui.screen.event

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ScreenshotMonitor
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bettermingle.app.R
import com.bettermingle.app.ui.component.BetterMingleButton
import com.bettermingle.app.ui.component.BetterMingleCard
import com.bettermingle.app.ui.component.BetterMingleOutlinedButton
import com.bettermingle.app.ui.component.BetterMingleTextField
import com.bettermingle.app.data.model.EventStatus
import com.bettermingle.app.data.model.PREDEFINED_THEMES
import com.bettermingle.app.ui.theme.AccentGold
import com.bettermingle.app.ui.theme.AccentOrange
import com.bettermingle.app.ui.theme.AccentPink
import com.bettermingle.app.ui.theme.BetterMingleThemeColors
import com.bettermingle.app.ui.theme.PrimaryBlue
import com.bettermingle.app.ui.theme.Spacing
import com.bettermingle.app.ui.theme.Success
import com.bettermingle.app.ui.theme.MODULE_COLOR_PALETTE
import com.bettermingle.app.ui.theme.hexToColor
import com.bettermingle.app.ui.component.ModuleColorPickerDialog
import com.bettermingle.app.data.model.EventModule
import com.bettermingle.app.data.repository.EventRepository
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip

import com.bettermingle.app.utils.ActivityLogger
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EventSettingsScreen(
    eventId: String,
    onNavigateBack: () -> Unit,
    onEventDeleted: () -> Unit = onNavigateBack,
    onDuplicateEvent: ((String, String, String, String, String, List<String>) -> Unit)? = null
) {
    var notificationsEnabled by remember { mutableStateOf(true) }
    var inviteCode by remember { mutableStateOf("") }
    var eventName by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var eventDescription by remember { mutableStateOf("") }
    var eventIntroText by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Security state (would be loaded from event data)
    val currentUserId = remember { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    var isOrganizer by remember { mutableStateOf(false) }
    var isCreator by remember { mutableStateOf(false) }
    var securityEnabled by remember { mutableStateOf(false) }
    var hideFinancials by remember { mutableStateOf(false) }
    var screenshotProtection by remember { mutableStateOf(false) }
    var requireApproval by remember { mutableStateOf(false) }
    var eventStatus by remember { mutableStateOf(EventStatus.PLANNING) }
    var eventTheme by remember { mutableStateOf("") }
    var eventLocationName by remember { mutableStateOf("") }
    var enabledModuleNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var moduleColors by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var colorPickerModuleName by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(eventId) {
        try {
            val doc = FirebaseFirestore.getInstance()
                .collection("events").document(eventId).get().await()
            inviteCode = doc.getString("inviteCode") ?: ""
            eventName = doc.getString("name") ?: ""
            eventDescription = doc.getString("description") ?: ""
            eventIntroText = doc.getString("introText") ?: ""
            securityEnabled = doc.getBoolean("securityEnabled") ?: false
            hideFinancials = doc.getBoolean("hideFinancials") ?: false
            screenshotProtection = doc.getBoolean("screenshotProtection") ?: false
            requireApproval = doc.getBoolean("requireApproval") ?: false
            eventTheme = doc.getString("theme") ?: ""
            eventLocationName = doc.getString("locationName") ?: ""
            @Suppress("UNCHECKED_CAST")
            enabledModuleNames = (doc.get("enabledModules") as? List<String>) ?: emptyList()
            @Suppress("UNCHECKED_CAST")
            moduleColors = (doc.get("moduleColors") as? Map<String, String>) ?: emptyMap()
            val statusStr = doc.getString("status") ?: "PLANNING"
            eventStatus = try { EventStatus.valueOf(statusStr) } catch (_: Exception) { EventStatus.PLANNING }
            val createdBy = doc.getString("createdBy") ?: ""
            isCreator = createdBy == currentUserId
            isOrganizer = isCreator
        } catch (_: Exception) {
            scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.error_load_failed)) }
        }

        // Check if current user is a co-organizer
        if (!isOrganizer && currentUserId.isNotEmpty()) {
            try {
                val parts = FirebaseFirestore.getInstance()
                    .collection("events").document(eventId)
                    .collection("participants").get().await()
                val userParticipant = parts.documents.firstOrNull { it.getString("userId") == currentUserId }
                val role = userParticipant?.getString("role") ?: ""
                if (role.equals("CO_ORGANIZER", ignoreCase = true)) {
                    isOrganizer = true
                }
            } catch (_: Exception) { }
        }

        // Load notification preference
        try {
            val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                val lastSeenDoc = FirebaseFirestore.getInstance()
                    .collection("events").document(eventId)
                    .collection("lastSeen").document(uid).get().await()
                notificationsEnabled = lastSeenDoc.getBoolean("notificationsEnabled") ?: true
            }
        } catch (_: Exception) { }
    }

    val inviteLink = "https://bettermingle.app/invite/$inviteCode"
    val shareText = stringResource(R.string.event_settings_share_text, eventName, inviteLink)

    if (showPinDialog) {
        var newPin by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text(stringResource(R.string.event_settings_pin_dialog_title)) },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.event_settings_pin_dialog_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    BetterMingleTextField(
                        value = newPin,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) newPin = it },
                        label = stringResource(R.string.event_settings_pin_label)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                FirebaseFirestore.getInstance()
                                    .collection("events").document(eventId)
                                    .update("eventPin", newPin).await()
                                Toast.makeText(context, context.getString(R.string.event_settings_pin_changed), Toast.LENGTH_SHORT).show()
                                showPinDialog = false
                            } catch (_: Exception) {
                                Toast.makeText(context, context.getString(R.string.event_settings_pin_error), Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = newPin.length == 4
                ) { Text(stringResource(R.string.common_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }

    if (showEditDialog) {
        var editName by remember { mutableStateOf(eventName) }
        var editDescription by remember { mutableStateOf(eventDescription) }
        var editTheme by remember { mutableStateOf(eventTheme) }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text(stringResource(R.string.event_settings_edit_title)) },
            text = {
                Column {
                    BetterMingleTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = stringResource(R.string.event_settings_edit_name)
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    BetterMingleTextField(
                        value = editDescription,
                        onValueChange = { editDescription = it },
                        label = stringResource(R.string.event_settings_edit_description),
                        singleLine = false,
                        maxLines = 5
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = stringResource(R.string.event_settings_edit_formatting),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))
                    BetterMingleTextField(
                        value = editTheme,
                        onValueChange = { editTheme = it },
                        label = stringResource(R.string.event_settings_edit_theme)
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        PREDEFINED_THEMES.forEach { t ->
                            FilterChip(
                                selected = editTheme == t,
                                onClick = { editTheme = if (editTheme == t) "" else t },
                                label = { Text(t, style = MaterialTheme.typography.labelSmall) },
                                shape = RoundedCornerShape(100.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentPink.copy(alpha = 0.12f),
                                    selectedLabelColor = AccentPink
                                )
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
                                    .update(
                                        mapOf(
                                            "name" to editName,
                                            "description" to editDescription,
                                            "theme" to editTheme
                                        )
                                    ).await()
                                val changes = mutableListOf<String>()
                                if (editName != eventName) changes.add(context.getString(R.string.activity_change_name))
                                if (editDescription != eventDescription) changes.add(context.getString(R.string.activity_change_description))
                                if (editTheme != eventTheme) changes.add(context.getString(R.string.activity_change_theme))
                                eventName = editName
                                eventDescription = editDescription
                                eventTheme = editTheme
                                if (changes.isNotEmpty()) {
                                    ActivityLogger.log(eventId, "settings", context.getString(R.string.activity_edited_event, changes.joinToString(", ")), eventName = editName)
                                }
                                Toast.makeText(context, context.getString(R.string.event_settings_edit_success), Toast.LENGTH_SHORT).show()
                                showEditDialog = false
                            } catch (_: Exception) {
                                Toast.makeText(context, context.getString(R.string.event_settings_edit_error), Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = editName.isNotBlank()
                ) { Text(stringResource(R.string.common_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }

    if (showDeleteDialog) {
        var isDeleting by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!isDeleting) showDeleteDialog = false },
            title = { Text(stringResource(R.string.event_settings_delete_title)) },
            text = { Text(stringResource(R.string.event_settings_delete_confirm, eventName)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        isDeleting = true
                        scope.launch {
                            try {
                                EventRepository(context).deleteEvent(eventId)
                                ActivityLogger.log(eventId, "settings", context.getString(R.string.activity_deleted_event, eventName), eventName = eventName)
                                showDeleteDialog = false
                                onEventDeleted()
                            } catch (_: Exception) {
                                isDeleting = false
                                snackbarHostState.showSnackbar(context.getString(R.string.error_delete_failed))
                            }
                        }
                    },
                    enabled = !isDeleting
                ) {
                    if (isDeleting) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = AccentOrange
                        )
                    } else {
                        Text(stringResource(R.string.common_delete), color = AccentOrange)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    enabled = !isDeleting
                ) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.event_settings_title), style = MaterialTheme.typography.titleMedium) },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(Spacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // Invite link
            item {
                BetterMingleCard {
                    Column {
                        Text(
                            text = stringResource(R.string.event_settings_invite_section),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(Spacing.sm))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            BetterMingleOutlinedButton(
                                text = stringResource(R.string.event_settings_copy_link),
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("invite_link", inviteLink))
                                    Toast.makeText(context, context.getString(R.string.event_settings_link_copied), Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f)
                            )

                            BetterMingleButton(
                                text = stringResource(R.string.event_settings_share),
                                onClick = {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    }
                                    context.startActivity(Intent.createChooser(intent, context.getString(R.string.event_settings_share_chooser)))
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Notifications
            item {
                BetterMingleCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.event_settings_notifications),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = stringResource(R.string.event_settings_notifications_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { enabled ->
                                notificationsEnabled = enabled
                                val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                                if (uid != null) {
                                    FirebaseFirestore.getInstance()
                                        .collection("events").document(eventId)
                                        .collection("lastSeen").document(uid)
                                        .set(mapOf("notificationsEnabled" to enabled), com.google.firebase.firestore.SetOptions.merge())
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = PrimaryBlue
                            )
                        )
                    }
                }
            }

            // Event status (only for organizer)
            if (isOrganizer) { item {
                BetterMingleCard {
                    Column {
                        Text(
                            text = stringResource(R.string.event_settings_status),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            data class StatusOption(val status: EventStatus, val label: String, val bgColor: androidx.compose.ui.graphics.Color, val textColor: androidx.compose.ui.graphics.Color)
                            val statusPlanning = stringResource(R.string.event_status_planning)
                            val statusConfirmed = stringResource(R.string.event_status_confirmed)
                            val statusOngoing = stringResource(R.string.event_status_ongoing)
                            val statusCompleted = stringResource(R.string.event_status_completed)
                            val statusCancelled = stringResource(R.string.event_status_cancelled)
                            val ext = BetterMingleThemeColors.extended
                            val options = listOf(
                                StatusOption(EventStatus.PLANNING, statusPlanning, ext.pastelGold, AccentGold),
                                StatusOption(EventStatus.CONFIRMED, statusConfirmed, ext.pastelBlue, PrimaryBlue),
                                StatusOption(EventStatus.ONGOING, statusOngoing, ext.pastelGreen, Success),
                                StatusOption(EventStatus.COMPLETED, statusCompleted, ext.pastelGray, MaterialTheme.colorScheme.onSurfaceVariant),
                                StatusOption(EventStatus.CANCELLED, statusCancelled, ext.pastelOrange, AccentOrange)
                            )
                            options.forEach { opt ->
                                FilterChip(
                                    selected = eventStatus == opt.status,
                                    onClick = {
                                        if (eventStatus != opt.status) {
                                            val oldStatus = eventStatus
                                            eventStatus = opt.status
                                            scope.launch {
                                                try {
                                                    FirebaseFirestore.getInstance()
                                                        .collection("events").document(eventId)
                                                        .update("status", opt.status.name).await()
                                                    ActivityLogger.log(eventId, "settings", context.getString(R.string.activity_changed_status, opt.label), eventName = eventName)
                                                } catch (_: Exception) {
                                                    eventStatus = oldStatus
                                                    Toast.makeText(context, context.getString(R.string.event_settings_status_error), Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    },
                                    label = { Text(opt.label) },
                                    shape = RoundedCornerShape(100.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = opt.bgColor,
                                        selectedLabelColor = opt.textColor
                                    )
                                )
                            }
                        }
                    }
                }
            } }

            // Security section (only for organizer)
            if (isOrganizer) { item {
                BetterMingleCard {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Security,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(Spacing.sm))
                                Column {
                                    Text(
                                        text = stringResource(R.string.event_settings_security),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (securityEnabled) stringResource(R.string.event_settings_security_active) else stringResource(R.string.event_settings_security_disabled),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (securityEnabled) Success else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Switch(
                                checked = securityEnabled,
                                onCheckedChange = {
                                    securityEnabled = it
                                    scope.launch {
                                        try {
                                            FirebaseFirestore.getInstance()
                                                .collection("events").document(eventId)
                                                .update("securityEnabled", it).await()
                                        } catch (_: Exception) { }
                                    }
                                },
                                colors = SwitchDefaults.colors(checkedTrackColor = PrimaryBlue)
                            )
                        }

                        if (securityEnabled) {
                            Spacer(modifier = Modifier.height(Spacing.md))

                            SecuritySettingRow(
                                icon = Icons.Default.VisibilityOff,
                                title = stringResource(R.string.event_settings_hide_financials),
                                description = stringResource(R.string.event_settings_hide_financials_desc),
                                checked = hideFinancials,
                                onCheckedChange = {
                                    hideFinancials = it
                                    scope.launch { try { FirebaseFirestore.getInstance().collection("events").document(eventId).update("hideFinancials", it).await() } catch (_: Exception) { } }
                                }
                            )

                            Spacer(modifier = Modifier.height(Spacing.sm))

                            SecuritySettingRow(
                                icon = Icons.Default.ScreenshotMonitor,
                                title = stringResource(R.string.event_settings_screenshot),
                                description = stringResource(R.string.event_settings_screenshot_desc),
                                checked = screenshotProtection,
                                onCheckedChange = {
                                    screenshotProtection = it
                                    scope.launch { try { FirebaseFirestore.getInstance().collection("events").document(eventId).update("screenshotProtection", it).await() } catch (_: Exception) { } }
                                }
                            )

                            Spacer(modifier = Modifier.height(Spacing.sm))

                            SecuritySettingRow(
                                icon = Icons.Default.VerifiedUser,
                                title = stringResource(R.string.event_settings_approval),
                                description = stringResource(R.string.event_settings_approval_desc),
                                checked = requireApproval,
                                onCheckedChange = {
                                    requireApproval = it
                                    scope.launch { try { FirebaseFirestore.getInstance().collection("events").document(eventId).update("requireApproval", it).await() } catch (_: Exception) { } }
                                }
                            )

                            Spacer(modifier = Modifier.height(Spacing.sm))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(Spacing.sm))
                                BetterMingleOutlinedButton(
                                    text = stringResource(R.string.event_settings_change_pin),
                                    onClick = { showPinDialog = true },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            } }

            // Edit event (only for organizer)
            if (isOrganizer) {
                item {
                    SettingsActionItem(
                        icon = Icons.Default.Edit,
                        title = stringResource(R.string.event_settings_edit_action),
                        description = stringResource(R.string.event_settings_edit_action_desc),
                        onClick = { showEditDialog = true }
                    )
                }

                // Duplicate event
                item {
                    SettingsActionItem(
                        icon = Icons.Default.ContentCopy,
                        title = stringResource(R.string.event_settings_repeat),
                        description = stringResource(R.string.event_settings_repeat_desc),
                        onClick = {
                            onDuplicateEvent?.invoke(
                                eventName,
                                eventDescription,
                                eventLocationName,
                                eventIntroText,
                                eventTheme,
                                enabledModuleNames
                            )
                        }
                    )
                }

                // Module colors section
                if (enabledModuleNames.isNotEmpty()) {
                    item {
                        // Color picker dialog
                        colorPickerModuleName?.let { moduleName ->
                            val currentColorHex = moduleColors[moduleName]
                            val currentColor = currentColorHex?.let { hexToColor(it) }
                                ?: defaultModuleColor(moduleName)
                            ModuleColorPickerDialog(
                                currentColor = currentColor,
                                onColorSelected = { option ->
                                    moduleColors = moduleColors + (moduleName to option.hex)
                                    colorPickerModuleName = null
                                    scope.launch {
                                        EventRepository(context).updateModuleColor(eventId, moduleName, option.hex)
                                    }
                                },
                                onDismiss = { colorPickerModuleName = null }
                            )
                        }

                        BetterMingleCard {
                            Column {
                                Text(
                                    text = stringResource(R.string.module_colors_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(Spacing.sm))
                                enabledModuleNames.forEach { moduleName ->
                                    val colorHex = moduleColors[moduleName]
                                    val color = colorHex?.let { hexToColor(it) }
                                        ?: defaultModuleColor(moduleName)
                                    val displayName = moduleDisplayName(moduleName)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = Spacing.xs),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(color, CircleShape)
                                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
                                                .clickable { colorPickerModuleName = moduleName }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Danger zone (only for event creator, not co-organizers)
            if (isCreator) {
                item {
                    Spacer(modifier = Modifier.height(Spacing.md))
                    Text(
                        text = stringResource(R.string.event_settings_danger_zone),
                        style = MaterialTheme.typography.titleSmall,
                        color = AccentOrange,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                item {
                    SettingsActionItem(
                        icon = Icons.Default.Delete,
                        title = stringResource(R.string.event_settings_delete_action),
                        description = stringResource(R.string.event_settings_delete_action_desc),
                        onClick = { showDeleteDialog = true },
                        isDangerous = true
                    )
                }
            }
        }
    }
}

@Composable
private fun SecuritySettingRow(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = PrimaryBlue,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(Spacing.sm))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = PrimaryBlue)
        )
    }
}

@Composable
private fun SettingsActionItem(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    isDangerous: Boolean = false
) {
    BetterMingleCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDangerous) AccentOrange else PrimaryBlue,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(Spacing.md))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (isDangerous) AccentOrange else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun defaultModuleColor(moduleName: String): androidx.compose.ui.graphics.Color = when (moduleName) {
    "VOTING" -> PrimaryBlue
    "EXPENSES" -> AccentOrange
    "CARPOOL" -> Success
    "ROOMS" -> AccentGold
    "CHAT" -> AccentOrange
    "SCHEDULE" -> PrimaryBlue
    "TASKS" -> AccentPink
    "PACKING_LIST" -> Success
    "WISHLIST" -> AccentPink
    "CATERING" -> Success
    "BUDGET" -> AccentGold
    else -> PrimaryBlue
}

@Composable
private fun moduleDisplayName(moduleName: String): String = when (moduleName) {
    "VOTING" -> stringResource(R.string.create_event_module_voting)
    "EXPENSES" -> stringResource(R.string.create_event_module_expenses)
    "CARPOOL" -> stringResource(R.string.create_event_module_carpool)
    "ROOMS" -> stringResource(R.string.create_event_module_rooms)
    "CHAT" -> stringResource(R.string.create_event_module_chat)
    "SCHEDULE" -> stringResource(R.string.create_event_module_schedule)
    "TASKS" -> stringResource(R.string.create_event_module_tasks)
    "PACKING_LIST" -> stringResource(R.string.create_event_module_packing)
    "WISHLIST" -> stringResource(R.string.create_event_module_wishlist)
    "CATERING" -> stringResource(R.string.create_event_module_catering)
    "BUDGET" -> stringResource(R.string.create_event_module_budget)
    else -> moduleName
}
