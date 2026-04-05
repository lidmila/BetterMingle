package com.bettermingle.app.ui.screen.event

import android.content.Intent
import android.net.Uri
import com.bettermingle.app.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bettermingle.app.data.model.WishlistItem
import com.bettermingle.app.data.model.WishlistItemStatus
import com.bettermingle.app.ui.component.BetterMingleCard
import com.bettermingle.app.ui.component.BetterMingleTextField
import com.bettermingle.app.ui.component.EmptyState
import com.bettermingle.app.ui.theme.AccentOrange
import com.bettermingle.app.ui.theme.AccentPink
import com.bettermingle.app.ui.theme.PrimaryBlue
import com.bettermingle.app.ui.theme.Spacing
import com.bettermingle.app.ui.theme.Success
import com.bettermingle.app.ui.theme.TextOnColor
import com.bettermingle.app.ui.component.ModuleColorPickerDialog
import com.bettermingle.app.data.repository.EventRepository
import androidx.compose.material.icons.filled.Palette
import androidx.compose.foundation.clickable
import androidx.compose.material3.ListItem
import com.bettermingle.app.data.model.Participant
import com.bettermingle.app.data.model.ParticipantRole
import com.bettermingle.app.data.model.RsvpStatus
import com.bettermingle.app.utils.ActivityLogger
import com.bettermingle.app.utils.ParticipantUtils
import com.bettermingle.app.utils.removeModuleFromEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistScreen(
    eventId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val items = remember { mutableStateListOf<WishlistItem>() }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCreateDialog by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var isOrganizer by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    val currentUser = remember { FirebaseAuth.getInstance().currentUser }
    val currentUserId = currentUser?.uid ?: ""
    val manualParticipants = remember { mutableStateListOf<Participant>() }
    var showClaimForGuestDialog by remember { mutableStateOf<WishlistItem?>(null) }

    // Check if current user is the event organizer
    LaunchedEffect(eventId) {
        try {
            val eventDoc = FirebaseFirestore.getInstance()
                .collection("events").document(eventId).get().await()
            val createdBy = eventDoc.getString("createdBy") ?: ""
            isOrganizer = createdBy == currentUserId
        } catch (_: Exception) { }
        // Load manual participants for claiming on behalf
        try {
            val snapshot = FirebaseFirestore.getInstance()
                .collection("events").document(eventId)
                .collection("participants").get().await()
            manualParticipants.clear()
            manualParticipants.addAll(snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                val isManual = data["isManual"] as? Boolean ?: false
                if (!isManual) return@mapNotNull null
                Participant(
                    id = doc.id,
                    eventId = eventId,
                    userId = data["userId"] as? String ?: "",
                    displayName = data["displayName"] as? String ?: doc.id.take(8),
                    isManual = true
                )
            })
        } catch (_: Exception) { }
    }

    fun loadItems() {
        scope.launch {
            try {
                val snapshot = FirebaseFirestore.getInstance()
                    .collection("events").document(eventId)
                    .collection("wishlistItems").get().await()

                val loaded = snapshot.documents.map { doc ->
                    val data = doc.data ?: emptyMap()
                    val statusStr = data["status"] as? String
                    val status = try {
                        WishlistItemStatus.valueOf(statusStr ?: "FREE")
                    } catch (_: Exception) {
                        WishlistItemStatus.FREE
                    }
                    WishlistItem(
                        id = doc.id,
                        eventId = eventId,
                        name = data["name"] as? String ?: "",
                        price = data["price"] as? String,
                        productUrl = data["productUrl"] as? String,
                        description = data["description"] as? String,
                        status = status,
                        claimedBy = data["claimedBy"] as? String,
                        claimedByName = data["claimedByName"] as? String,
                        addedBy = data["addedBy"] as? String ?: "",
                        createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L
                    )
                }.sortedByDescending { it.createdAt }

                items.clear()
                items.addAll(loaded)
                isLoading = false
            } catch (_: Exception) {
                isLoading = false
                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.error_load_failed)) }
            }
        }
    }

    LaunchedEffect(eventId) { loadItems() }

    if (showCreateDialog) {
        AddWishlistItemDialog(
            eventId = eventId,
            onDismiss = { showCreateDialog = false },
            onCreated = {
                showCreateDialog = false
                loadItems()
            }
        )
    }

    if (showColorPicker) {
        ModuleColorPickerDialog(
            currentColor = AccentPink,
            onColorSelected = { option ->
                showColorPicker = false
                scope.launch {
                    EventRepository(context).updateModuleColor(eventId, "WISHLIST", option.hex)
                }
            },
            onDismiss = { showColorPicker = false }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.wishlist_title), style = MaterialTheme.typography.titleMedium) },
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
                                        removeModuleFromEvent(eventId, "WISHLIST")
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
            if (isOrganizer) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    contentColor = TextOnColor,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                    modifier = Modifier
                        .shadow(8.dp, CircleShape, ambientColor = AccentPink.copy(alpha = 0.3f), spotColor = AccentPink.copy(alpha = 0.3f))
                        .background(AccentPink, CircleShape)
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.wishlist_add))
                }
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                loadItems()
                scope.launch {
                    kotlinx.coroutines.delay(500)
                    isRefreshing = false
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                isLoading && items.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        androidx.compose.material3.CircularProgressIndicator()
                    }
                }
                items.isEmpty() && !isRefreshing -> {
                    EmptyState(
                        icon = Icons.Default.CardGiftcard,
                        illustration = R.drawable.il_empty_wishlist,
                        title = stringResource(R.string.wishlist_empty_title),
                        description = stringResource(R.string.wishlist_empty_description),
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                // Claim for guest dialog
                showClaimForGuestDialog?.let { wishItem ->
                    AlertDialog(
                        onDismissRequest = { showClaimForGuestDialog = null },
                        title = { Text(stringResource(R.string.wishlist_claim_for_title)) },
                        text = {
                            Column {
                                Text(stringResource(R.string.wishlist_select_guest), style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(Spacing.sm))
                                manualParticipants.forEach { guest ->
                                    ListItem(
                                        headlineContent = { Text(guest.displayName) },
                                        modifier = Modifier.clickable {
                                            val wi = wishItem
                                            val g = guest
                                            showClaimForGuestDialog = null
                                            val idx = items.indexOfFirst { it.id == wi.id }
                                            if (idx >= 0) {
                                                items[idx] = items[idx].copy(
                                                    status = WishlistItemStatus.RESERVED,
                                                    claimedBy = g.userId,
                                                    claimedByName = g.displayName
                                                )
                                            }
                                            scope.launch {
                                                try {
                                                    FirebaseFirestore.getInstance()
                                                        .collection("events").document(eventId)
                                                        .collection("wishlistItems").document(wi.id)
                                                        .update(
                                                            hashMapOf<String, Any?>(
                                                                "status" to WishlistItemStatus.RESERVED.name,
                                                                "claimedBy" to g.userId,
                                                                "claimedByName" to g.displayName
                                                            )
                                                        ).await()
                                                    ActivityLogger.log(eventId, "wishlist", context.getString(R.string.activity_wishlist_reserved, wi.name))
                                                } catch (_: Exception) {
                                                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.error_save_failed)) }
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        },
                        confirmButton = {},
                        dismissButton = {
                            TextButton(onClick = { showClaimForGuestDialog = null }) {
                                Text(stringResource(R.string.common_cancel))
                            }
                        }
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(Spacing.screenPadding),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    items(items, key = { it.id }) { item ->
                        WishlistItemCard(
                            item = item,
                            isOrganizer = isOrganizer,
                            currentUserId = currentUserId,
                            hasManualParticipants = isOrganizer && manualParticipants.isNotEmpty(),
                            onClaimForGuest = { showClaimForGuestDialog = item },
                            onStatusChange = { newStatus ->
                                val idx = items.indexOfFirst { it.id == item.id }
                                if (idx >= 0) {
                                    val userName = currentUser?.displayName ?: ""
                                    val updatedItem = if (newStatus == WishlistItemStatus.FREE) {
                                        items[idx].copy(
                                            status = WishlistItemStatus.FREE,
                                            claimedBy = null,
                                            claimedByName = null
                                        )
                                    } else {
                                        items[idx].copy(
                                            status = newStatus,
                                            claimedBy = currentUserId,
                                            claimedByName = userName
                                        )
                                    }
                                    items[idx] = updatedItem
                                    scope.launch {
                                        try {
                                            val updates = if (newStatus == WishlistItemStatus.FREE) {
                                                hashMapOf<String, Any?>(
                                                    "status" to newStatus.name,
                                                    "claimedBy" to null,
                                                    "claimedByName" to null
                                                )
                                            } else {
                                                hashMapOf<String, Any?>(
                                                    "status" to newStatus.name,
                                                    "claimedBy" to currentUserId,
                                                    "claimedByName" to userName
                                                )
                                            }
                                            FirebaseFirestore.getInstance()
                                                .collection("events").document(eventId)
                                                .collection("wishlistItems").document(item.id)
                                                .update(updates).await()

                                            val activityMsg = when (newStatus) {
                                                WishlistItemStatus.RESERVED -> context.getString(R.string.activity_wishlist_reserved, item.name)
                                                WishlistItemStatus.BOUGHT -> context.getString(R.string.activity_wishlist_bought, item.name)
                                                WishlistItemStatus.FREE -> context.getString(R.string.activity_wishlist_released, item.name)
                                            }
                                            ActivityLogger.log(eventId, "wishlist", activityMsg)
                                        } catch (_: Exception) {
                                            scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.error_save_failed)) }
                                        }
                                    }
                                }
                            },
                            onDelete = {
                                scope.launch {
                                    try {
                                        FirebaseFirestore.getInstance()
                                            .collection("events").document(eventId)
                                            .collection("wishlistItems").document(item.id)
                                            .delete().await()
                                        items.removeAll { it.id == item.id }
                                        ActivityLogger.log(eventId, "wishlist", context.getString(R.string.activity_removed_from_wishlist, item.name))
                                    } catch (_: Exception) {
                                        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.error_delete_failed)) }
                                    }
                                }
                            }
                        )
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun WishlistItemCard(
    item: WishlistItem,
    isOrganizer: Boolean,
    currentUserId: String,
    hasManualParticipants: Boolean = false,
    onClaimForGuest: () -> Unit = {},
    onStatusChange: (WishlistItemStatus) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val isMine = item.claimedBy == currentUserId
    val isFree = item.status == WishlistItemStatus.FREE
    val isReserved = item.status == WishlistItemStatus.RESERVED
    val isBought = item.status == WishlistItemStatus.BOUGHT

    BetterMingleCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CardGiftcard,
                    contentDescription = null,
                    tint = AccentPink,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (!item.price.isNullOrBlank()) {
                        Text(
                            text = item.price,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (isOrganizer) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.common_delete), tint = AccentOrange, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // Product URL
            if (!item.productUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        try {
                            val url = if (item.productUrl.startsWith("http")) item.productUrl else "https://${item.productUrl}"
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        } catch (_: Exception) { }
                    }
                ) {
                    Icon(
                        Icons.Default.Link,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.wishlist_open_url),
                        style = MaterialTheme.typography.bodySmall,
                        color = PrimaryBlue
                    )
                }
            }

            // Description
            if (!item.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Status info badge (when claimed by someone)
            if (!isFree) {
                val badgeColor = if (isBought) Success else PrimaryBlue
                val badgeIcon = if (isBought) Icons.Default.ShoppingCart else Icons.Default.CheckCircle
                val badgeText = if (isBought) {
                    stringResource(R.string.wishlist_bought_by, item.claimedByName ?: "")
                } else {
                    stringResource(R.string.wishlist_reserved_by, item.claimedByName ?: "")
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(badgeColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(badgeIcon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text(text = badgeText, style = MaterialTheme.typography.bodySmall, color = badgeColor)
                }
                Spacer(modifier = Modifier.height(Spacing.xs))
            }

            // Claim for guest button
            if (isFree && hasManualParticipants) {
                TextButton(onClick = onClaimForGuest) {
                    Text(
                        text = stringResource(R.string.wishlist_claim_for_guest),
                        color = AccentOrange
                    )
                }
            }

            // Status toggle chips - show when item is free (anyone) or claimed by me
            if (isFree || isMine) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    FilterChip(
                        selected = isFree,
                        onClick = { if (!isFree) onStatusChange(WishlistItemStatus.FREE) },
                        label = { Text(stringResource(R.string.wishlist_status_free)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            selectedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    FilterChip(
                        selected = isReserved,
                        onClick = { if (!isReserved) onStatusChange(WishlistItemStatus.RESERVED) },
                        label = { Text(stringResource(R.string.wishlist_status_reserved)) },
                        leadingIcon = if (isReserved) { { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) } } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlue.copy(alpha = 0.15f),
                            selectedLabelColor = PrimaryBlue
                        )
                    )
                    FilterChip(
                        selected = isBought,
                        onClick = { if (!isBought) onStatusChange(WishlistItemStatus.BOUGHT) },
                        label = { Text(stringResource(R.string.wishlist_status_bought)) },
                        leadingIcon = if (isBought) { { Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp)) } } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Success.copy(alpha = 0.15f),
                            selectedLabelColor = Success
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun AddWishlistItemDialog(
    eventId: String,
    onDismiss: () -> Unit,
    onCreated: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var productUrl by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.wishlist_add)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                BetterMingleTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(R.string.wishlist_name_label)
                )
                BetterMingleTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = stringResource(R.string.wishlist_price_label)
                )
                BetterMingleTextField(
                    value = productUrl,
                    onValueChange = { productUrl = it },
                    label = stringResource(R.string.wishlist_url_label)
                )
                BetterMingleTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = stringResource(R.string.wishlist_description_label)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    scope.launch {
                        try {
                            val currentUser = FirebaseAuth.getInstance().currentUser
                            val itemData = hashMapOf<String, Any?>(
                                "name" to name,
                                "price" to price.ifBlank { null },
                                "productUrl" to productUrl.ifBlank { null },
                                "description" to description.ifBlank { null },
                                "status" to WishlistItemStatus.FREE.name,
                                "claimedBy" to null,
                                "claimedByName" to null,
                                "addedBy" to (currentUser?.uid ?: ""),
                                "createdAt" to System.currentTimeMillis()
                            )
                            FirebaseFirestore.getInstance()
                                .collection("events").document(eventId)
                                .collection("wishlistItems")
                                .add(itemData).await()
                            ActivityLogger.log(eventId, "wishlist", context.getString(R.string.activity_added_to_wishlist, name))
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
