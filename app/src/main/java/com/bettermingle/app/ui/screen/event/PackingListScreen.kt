package com.bettermingle.app.ui.screen.event

import com.bettermingle.app.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.height
import com.bettermingle.app.ui.component.BetterMingleTextField
import com.bettermingle.app.utils.ActivityLogger
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.bettermingle.app.data.model.PackingItem
import com.bettermingle.app.ui.component.BetterMingleCard
import com.bettermingle.app.ui.component.EmptyState
import com.bettermingle.app.ui.theme.AccentOrange
import com.bettermingle.app.ui.theme.PrimaryBlue
import com.bettermingle.app.ui.theme.Spacing
import com.bettermingle.app.ui.theme.Success
import com.bettermingle.app.ui.theme.TextOnColor

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackingListScreen(
    eventId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val items = remember { mutableStateListOf<PackingItem>() }
    val scope = rememberCoroutineScope()
    var showCreateDialog by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    fun loadItems() {
        scope.launch {
            try {
                val snapshot = FirebaseFirestore.getInstance()
                    .collection("events").document(eventId)
                    .collection("packingItems").get().await()

                val loaded = snapshot.documents.map { doc ->
                    val data = doc.data ?: emptyMap()
                    PackingItem(
                        id = doc.id,
                        eventId = eventId,
                        name = data["name"] as? String ?: "",
                        isChecked = data["isChecked"] as? Boolean ?: false,
                        userId = data["userId"] as? String,
                        addedBy = data["addedBy"] as? String ?: ""
                    )
                }.sortedBy { it.name }

                items.clear()
                items.addAll(loaded)
            } catch (_: Exception) { }
        }
    }

    LaunchedEffect(eventId) { loadItems() }

    if (showCreateDialog) {
        AddPackingItemDialog(
            eventId = eventId,
            onDismiss = { showCreateDialog = false },
            onCreated = {
                showCreateDialog = false
                loadItems()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.packing_title), style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
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
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.packing_add))
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
            if (items.isEmpty() && !isRefreshing) {
                EmptyState(
                    icon = Icons.Default.Backpack,
                    illustration = R.drawable.il_empty_packing,
                    title = stringResource(R.string.packing_empty_title),
                    description = stringResource(R.string.packing_empty_description),
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(Spacing.screenPadding),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    items(items, key = { it.id }) { item ->
                        PackingListItem(
                            item = item,
                            onCheckedChange = { checked ->
                                val idx = items.indexOfFirst { it.id == item.id }
                                if (idx >= 0) {
                                    items[idx] = items[idx].copy(isChecked = checked)
                                    scope.launch {
                                        try {
                                            FirebaseFirestore.getInstance()
                                                .collection("events").document(eventId)
                                                .collection("packingItems").document(item.id)
                                                .update("isChecked", checked).await()
                                        } catch (_: Exception) { }
                                    }
                                }
                            },
                            onDelete = {
                                scope.launch {
                                    try {
                                        FirebaseFirestore.getInstance()
                                            .collection("events").document(eventId)
                                            .collection("packingItems").document(item.id)
                                            .delete().await()
                                        items.removeAll { it.id == item.id }
                                        ActivityLogger.log(eventId, "packing", context.getString(R.string.activity_removed_from_packing, item.name))
                                    } catch (_: Exception) { }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PackingListItem(
    item: PackingItem,
    onCheckedChange: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    BetterMingleCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.isChecked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = Success,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.width(Spacing.xs))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (item.isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                if (item.userId != null) {
                    Text(
                        text = stringResource(R.string.packing_personal),
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryBlue
                    )
                } else {
                    Text(
                        text = stringResource(R.string.packing_shared),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.common_delete), tint = AccentOrange, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun AddPackingItemDialog(
    eventId: String,
    onDismiss: () -> Unit,
    onCreated: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.packing_add)) },
        text = {
            Column {
                BetterMingleTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(R.string.packing_name_label)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    scope.launch {
                        try {
                            val currentUser = FirebaseAuth.getInstance().currentUser
                            val itemData = hashMapOf(
                                "name" to name,
                                "isChecked" to false,
                                "addedBy" to (currentUser?.uid ?: ""),
                                "createdAt" to System.currentTimeMillis()
                            )
                            FirebaseFirestore.getInstance()
                                .collection("events").document(eventId)
                                .collection("packingItems")
                                .add(itemData).await()
                            ActivityLogger.log(eventId, "packing", context.getString(R.string.activity_added_to_packing, name))
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
