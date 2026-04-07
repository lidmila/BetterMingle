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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.AccessTime
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bettermingle.app.data.model.ScheduleItem
import com.bettermingle.app.ui.component.BetterMingleCard
import com.bettermingle.app.ui.component.BetterMingleTextField
import com.bettermingle.app.ui.component.EmptyState
import com.bettermingle.app.ui.component.PlacesAutocompleteField
import com.bettermingle.app.ui.component.ModuleColorPickerDialog
import com.bettermingle.app.data.repository.EventRepository
import androidx.compose.material.icons.filled.Palette
import com.bettermingle.app.ui.theme.AccentOrange
import com.bettermingle.app.ui.theme.PrimaryBlue
import com.bettermingle.app.ui.theme.Spacing
import com.bettermingle.app.ui.theme.TextOnColor

import com.bettermingle.app.utils.DateFormatUtils
import com.bettermingle.app.utils.ActivityLogger
import com.bettermingle.app.utils.removeModuleFromEvent
import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import com.bettermingle.app.utils.safeDocuments

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    eventId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val items = remember { mutableStateListOf<ScheduleItem>() }
    var showCreateDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<ScheduleItem?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var isOrganizer by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }

    LaunchedEffect(eventId) {
        try {
            val eventDoc = FirebaseFirestore.getInstance()
                .collection("events").document(eventId).get().await()
            isOrganizer = eventDoc.getString("createdBy") == currentUserId
        } catch (_: Exception) { }
    }

    fun loadSchedule() {
        scope.launch {
        try {
            val snapshot = FirebaseFirestore.getInstance()
                .collection("events").document(eventId)
                .collection("schedule").get().await()

            val loaded = snapshot.safeDocuments.map { doc ->
                val data = doc.data ?: emptyMap()
                ScheduleItem(
                    id = doc.id,
                    eventId = eventId,
                    title = data["title"] as? String ?: "",
                    description = data["description"] as? String ?: "",
                    startTime = (data["startTime"] as? Number)?.toLong(),
                    endTime = (data["endTime"] as? Number)?.toLong(),
                    location = data["location"] as? String ?: ""
                )
            }.sortedBy { it.startTime }

            items.clear()
            items.addAll(loaded)
            isLoading = false
        } catch (_: Exception) {
            isLoading = false
            scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.error_load_failed)) }
        }
        }
    }

    LaunchedEffect(eventId) { loadSchedule() }

    if (showCreateDialog) {
        AddScheduleItemDialog(
            eventId = eventId,
            onDismiss = { showCreateDialog = false },
            onCreated = {
                showCreateDialog = false
                loadSchedule()
            }
        )
    }

    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text(stringResource(R.string.schedule_delete_title)) },
            text = { Text(stringResource(R.string.schedule_delete_confirm, itemToDelete!!.title)) },
            confirmButton = {
                TextButton(onClick = {
                    val item = itemToDelete!!
                    itemToDelete = null
                    scope.launch {
                        try {
                            FirebaseFirestore.getInstance()
                                .collection("events").document(eventId)
                                .collection("schedule").document(item.id)
                                .delete().await()
                            items.removeAll { it.id == item.id }
                            ActivityLogger.log(eventId, "schedule", context.getString(R.string.activity_removed_from_schedule, item.title))
                        } catch (_: Exception) {
                            scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.error_delete_failed)) }
                        }
                    }
                }) {
                    Text(stringResource(R.string.common_delete), color = AccentOrange)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }

    if (showColorPicker) {
        ModuleColorPickerDialog(
            currentColor = PrimaryBlue,
            onColorSelected = { option ->
                showColorPicker = false
                scope.launch {
                    EventRepository(context).updateModuleColor(eventId, "SCHEDULE", option.hex)
                }
            },
            onDismiss = { showColorPicker = false }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.schedule_title), style = MaterialTheme.typography.titleMedium) },
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
                                        removeModuleFromEvent(eventId, "SCHEDULE")
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
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.schedule_add))
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                loadSchedule()
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
                        icon = Icons.Default.CalendarMonth,
                        illustration = R.drawable.il_empty_schedule,
                        title = stringResource(R.string.schedule_empty_title),
                        description = stringResource(R.string.schedule_empty_description),
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(Spacing.screenPadding),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    items(items, key = { it.id }) { item ->
                        ScheduleItemCard(
                            item = item,
                            onDelete = { itemToDelete = item }
                        )
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun ScheduleItemCard(item: ScheduleItem, onDelete: () -> Unit) {
    val context = LocalContext.current
    BetterMingleCard {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(80.dp)
            ) {
                item.startTime?.let { start ->
                    Text(
                        text = DateFormatUtils.formatDayMonth(start),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = DateFormatUtils.formatTime(start),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryBlue
                    )
                }
                item.endTime?.let { end ->
                    Text(
                        text = DateFormatUtils.formatTime(end),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(Spacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )

                if (item.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (item.location.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = item.location,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (item.startTime != null) {
                IconButton(onClick = {
                    val intent = Intent(Intent.ACTION_INSERT).apply {
                        data = CalendarContract.Events.CONTENT_URI
                        putExtra(CalendarContract.Events.TITLE, item.title)
                        putExtra(CalendarContract.Events.DESCRIPTION, item.description)
                        putExtra(CalendarContract.Events.EVENT_LOCATION, item.location)
                        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, item.startTime)
                        item.endTime?.let { putExtra(CalendarContract.EXTRA_EVENT_END_TIME, it) }
                    }
                    context.startActivity(intent)
                }) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = stringResource(R.string.schedule_add_to_calendar),
                        tint = PrimaryBlue
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.common_delete),
                    tint = AccentOrange
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddScheduleItemDialog(
    eventId: String,
    onDismiss: () -> Unit,
    onCreated: () -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var startTimeMillis by remember { mutableStateOf<Long?>(null) }
    var endTimeMillis by remember { mutableStateOf<Long?>(null) }
    var location by remember { mutableStateOf("") }
    var showDatePickerFor by remember { mutableStateOf<String?>(null) } // "start" or "end"
    var showTimePickerFor by remember { mutableStateOf<String?>(null) } // "start" or "end"
    var pendingDateMillis by remember { mutableStateOf<Long?>(null) }
    val scope = rememberCoroutineScope()

    // DatePicker dialog
    if (showDatePickerFor != null) {
        val existing = if (showDatePickerFor == "start") startTimeMillis else endTimeMillis
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = existing)
        DatePickerDialog(
            onDismissRequest = { showDatePickerFor = null },
            confirmButton = {
                TextButton(onClick = {
                    pendingDateMillis = datePickerState.selectedDateMillis
                    val target = showDatePickerFor
                    showDatePickerFor = null
                    showTimePickerFor = target
                }) { Text(stringResource(R.string.common_continue)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerFor = null }) { Text(stringResource(R.string.common_cancel)) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // TimePicker dialog
    if (showTimePickerFor != null) {
        val existing = if (showTimePickerFor == "start") startTimeMillis else endTimeMillis
        val cal = Calendar.getInstance().apply {
            if (existing != null) timeInMillis = existing
        }
        val timePickerState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = true
        )
        Dialog(
            onDismissRequest = { showTimePickerFor = null },
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
                        text = stringResource(R.string.schedule_select_time),
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
                        TextButton(onClick = { showTimePickerFor = null }) { Text(stringResource(R.string.common_cancel)) }
                        TextButton(onClick = {
                            val calendar = Calendar.getInstance().apply {
                                if (pendingDateMillis != null) {
                                    timeInMillis = pendingDateMillis!!
                                }
                                set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                                set(Calendar.MINUTE, timePickerState.minute)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            val result = calendar.timeInMillis
                            if (showTimePickerFor == "start") {
                                startTimeMillis = result
                            } else {
                                endTimeMillis = result
                            }
                            showTimePickerFor = null
                            pendingDateMillis = null
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
                Text(stringResource(R.string.schedule_add), style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(Spacing.lg))

                BetterMingleTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = stringResource(R.string.schedule_name_label)
                )

                Spacer(modifier = Modifier.height(Spacing.formFieldSpacing))

                BetterMingleTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = stringResource(R.string.schedule_description_label),
                    singleLine = false,
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(Spacing.formFieldSpacing))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showDatePickerFor = "start" }
                    ) {
                        BetterMingleTextField(
                            value = startTimeMillis?.let { DateFormatUtils.formatDateTime(it) } ?: "",
                            onValueChange = {},
                            label = stringResource(R.string.schedule_start_label),
                            enabled = false,
                            leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null) }
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showDatePickerFor = "end" }
                    ) {
                        BetterMingleTextField(
                            value = endTimeMillis?.let { DateFormatUtils.formatDateTime(it) } ?: "",
                            onValueChange = {},
                            label = stringResource(R.string.schedule_end_label),
                            enabled = false,
                            leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.formFieldSpacing))

                PlacesAutocompleteField(
                    value = location,
                    onValueChange = { location = it },
                    onPlaceSelected = { place -> location = place.name },
                    label = stringResource(R.string.schedule_location_label)
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
                                    val itemData = hashMapOf<String, Any?>(
                                        "title" to title,
                                        "description" to description,
                                        "startTime" to startTimeMillis,
                                        "endTime" to endTimeMillis,
                                        "location" to location
                                    )
                                    FirebaseFirestore.getInstance()
                                        .collection("events").document(eventId)
                                        .collection("schedule")
                                        .add(itemData).await()
                                    val locationInfo = if (location.isNotBlank()) " ($location)" else ""
                                    ActivityLogger.log(eventId, "schedule", context.getString(R.string.activity_added_to_schedule, "$title$locationInfo"))
                                    onCreated()
                                } catch (_: Exception) { }
                            }
                        },
                        enabled = title.isNotBlank() && startTimeMillis != null
                    ) {
                        Text(stringResource(R.string.common_add))
                    }
                }
            }
        }
    }
}
