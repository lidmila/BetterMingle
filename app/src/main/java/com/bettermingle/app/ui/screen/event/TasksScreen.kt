package com.bettermingle.app.ui.screen.event

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Person
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.bettermingle.app.data.model.EventLabel
import com.bettermingle.app.data.model.Participant
import com.bettermingle.app.data.model.ParticipantRole
import com.bettermingle.app.data.model.RsvpStatus
import com.bettermingle.app.ui.component.BetterMingleCard
import com.bettermingle.app.ui.component.BetterMingleTextField
import com.bettermingle.app.ui.component.EmptyState
import com.bettermingle.app.ui.theme.AccentGold
import com.bettermingle.app.ui.theme.AccentOrange
import com.bettermingle.app.ui.theme.AccentPink
import com.bettermingle.app.ui.theme.PrimaryBlue
import com.bettermingle.app.ui.theme.Spacing
import com.bettermingle.app.ui.theme.Success
import com.bettermingle.app.ui.theme.TextOnColor
import com.bettermingle.app.ui.theme.TextSecondary
import com.bettermingle.app.utils.DateFormatUtils
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class TaskColor(val name: String, val color: Color)

val taskColors = listOf(
    TaskColor("Modrá", PrimaryBlue),
    TaskColor("Růžová", AccentPink),
    TaskColor("Oranžová", AccentOrange),
    TaskColor("Zlatá", AccentGold),
    TaskColor("Zelená", Success)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    eventId: String,
    onNavigateBack: () -> Unit
) {
    val tasks = remember { mutableStateListOf<EventLabel>() }
    val userNames = remember { mutableMapOf<String, String>() }
    var showCreateDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun loadTasks() {
        scope.launch {
            try {
                val firestore = FirebaseFirestore.getInstance()
                val snapshot = firestore.collection("events").document(eventId)
                    .collection("tasks").get().await()

                val loaded = snapshot.documents.map { doc ->
                    val data = doc.data ?: emptyMap()
                    @Suppress("UNCHECKED_CAST")
                    EventLabel(
                        id = doc.id,
                        eventId = eventId,
                        name = data["name"] as? String ?: "",
                        color = data["color"] as? String ?: "Modrá",
                        assignedTo = (data["assignedTo"] as? List<String>) ?: emptyList(),
                        deadline = (data["deadline"] as? Number)?.toLong(),
                        isCompleted = data["isCompleted"] as? Boolean ?: false
                    )
                }.sortedWith(compareBy({ it.isCompleted }, { it.deadline ?: Long.MAX_VALUE }))

                // Load user names
                val allUserIds = loaded.flatMap { it.assignedTo }.distinct()
                for (uid in allUserIds) {
                    if (uid !in userNames) {
                        try {
                            val userDoc = firestore.collection("users").document(uid).get().await()
                            userNames[uid] = userDoc.getString("displayName") ?: uid.take(8)
                        } catch (_: Exception) { userNames[uid] = uid.take(8) }
                    }
                }

                tasks.clear()
                tasks.addAll(loaded)
            } catch (_: Exception) { }
        }
    }

    LaunchedEffect(eventId) { loadTasks() }

    val snackbarHostState = remember { SnackbarHostState() }
    var taskToDelete by remember { mutableStateOf<EventLabel?>(null) }
    val view = LocalView.current
    var isRefreshing by remember { mutableStateOf(false) }

    if (showCreateDialog) {
        CreateTaskDialog(
            eventId = eventId,
            onDismiss = { showCreateDialog = false },
            onCreated = {
                showCreateDialog = false
                loadTasks()
                scope.launch { snackbarHostState.showSnackbar("Úkol vytvořen") }
            }
        )
    }

    if (taskToDelete != null) {
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text("Smazat úkol") },
            text = { Text("Opravdu chceš smazat tento úkol?") },
            confirmButton = {
                TextButton(onClick = {
                    val task = taskToDelete!!
                    taskToDelete = null
                    scope.launch {
                        try {
                            FirebaseFirestore.getInstance()
                                .collection("events").document(eventId)
                                .collection("tasks").document(task.id)
                                .delete().await()
                            tasks.removeAll { it.id == task.id }
                            snackbarHostState.showSnackbar("Úkol smazán")
                        } catch (_: Exception) { }
                    }
                }) {
                    Text("Smazat", color = AccentOrange)
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) { Text("Zrušit") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Úkoly", style = MaterialTheme.typography.titleMedium) },
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
                Icon(Icons.Default.Add, contentDescription = "Nový úkol")
            }
        }
    ) { innerPadding ->
        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                loadTasks()
                scope.launch {
                    kotlinx.coroutines.delay(500)
                    isRefreshing = false
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
        if (tasks.isEmpty() && !isRefreshing) {
            EmptyState(
                icon = Icons.AutoMirrored.Filled.Assignment,
                iconDescription = "Ikona úkolů",
                title = "Zatím žádné úkoly",
                description = "Přidej úkoly a přiřaď je účastníkům.",
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(Spacing.screenPadding),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                items(tasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        eventId = eventId,
                        userNames = userNames,
                        onToggleCompleted = {
                            scope.launch {
                                try {
                                    val newCompleted = !task.isCompleted
                                    FirebaseFirestore.getInstance()
                                        .collection("events").document(eventId)
                                        .collection("tasks").document(task.id)
                                        .update("isCompleted", newCompleted).await()
                                    val idx = tasks.indexOfFirst { it.id == task.id }
                                    if (idx >= 0) tasks[idx] = task.copy(isCompleted = newCompleted)
                                    if (newCompleted) {
                                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                    } else {
                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    }
                                } catch (_: Exception) { }
                            }
                        },
                        onDelete = { taskToDelete = task }
                    )
                }
            }
        }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaskCard(
    task: EventLabel,
    eventId: String,
    userNames: Map<String, String>,
    onToggleCompleted: () -> Unit,
    onDelete: () -> Unit
) {
    val taskColor = taskColors.find { it.name == task.color }?.color ?: PrimaryBlue

    // 2.3 - Animate scale + alpha on completion toggle
    val targetScale = if (task.isCompleted) 0.97f else 1f
    val targetAlpha = if (task.isCompleted) 0.7f else 1f
    val animatedScale by animateFloatAsState(targetValue = targetScale, animationSpec = tween(300), label = "taskScale")
    val animatedAlpha by animateFloatAsState(targetValue = targetAlpha, animationSpec = tween(300), label = "taskAlpha")

    BetterMingleCard(
        modifier = Modifier.graphicsLayer {
            scaleX = animatedScale
            scaleY = animatedScale
            alpha = animatedAlpha
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggleCompleted() },
                colors = CheckboxDefaults.colors(checkedColor = taskColor)
            )

            Spacer(modifier = Modifier.width(Spacing.sm))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (task.isCompleted) TextSecondary else MaterialTheme.colorScheme.onSurface
                )

                task.deadline?.let { deadline ->
                    Text(
                        text = "Termín: ${DateFormatUtils.formatDate(deadline)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (!task.isCompleted && deadline < System.currentTimeMillis()) AccentOrange else TextSecondary
                    )
                }

                if (task.assignedTo.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        task.assignedTo.forEach { userId ->
                            Box(
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.extraSmall)
                                    .background(taskColor.copy(alpha = 0.1f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = "Přiřazeno",
                                        tint = taskColor,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = userNames[userId] ?: userId.take(8),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = taskColor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Delete button
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Smazat úkol",
                    tint = AccentOrange
                )
            }

            // Color indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(taskColor)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateTaskDialog(
    eventId: String,
    onDismiss: () -> Unit,
    onCreated: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("Modrá") }
    val selectedParticipants = remember { mutableStateListOf<String>() }
    val participants = remember { mutableStateListOf<Participant>() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(eventId) {
        try {
            val snapshot = FirebaseFirestore.getInstance()
                .collection("events").document(eventId)
                .collection("participants").get().await()
            participants.addAll(snapshot.documents.map { doc ->
                val data = doc.data ?: emptyMap()
                Participant(
                    id = doc.id,
                    eventId = eventId,
                    userId = data["userId"] as? String ?: "",
                    displayName = data["displayName"] as? String ?: doc.id.take(8),
                    role = ParticipantRole.PARTICIPANT,
                    rsvp = RsvpStatus.ACCEPTED,
                    joinedAt = 0
                )
            })
        } catch (_: Exception) { }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nový úkol") },
        text = {
            Column {
                BetterMingleTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Název úkolu"
                )

                Spacer(modifier = Modifier.height(Spacing.sm))

                Text(
                    text = "Barva",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    taskColors.forEach { tc ->
                        FilterChip(
                            selected = selectedColor == tc.name,
                            onClick = { selectedColor = tc.name },
                            label = { Text(tc.name, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = tc.color.copy(alpha = 0.12f),
                                selectedLabelColor = tc.color
                            )
                        )
                    }
                }

                if (participants.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Text(
                        text = "Přiřadit",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        participants.forEach { p ->
                            val isSelected = p.userId in selectedParticipants
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (isSelected) selectedParticipants.remove(p.userId)
                                    else selectedParticipants.add(p.userId)
                                },
                                label = { Text(p.displayName.ifEmpty { p.userId.take(8) }, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryBlue.copy(alpha = 0.12f),
                                    selectedLabelColor = PrimaryBlue
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    scope.launch {
                        try {
                            val taskData = hashMapOf(
                                "name" to name,
                                "color" to selectedColor,
                                "assignedTo" to selectedParticipants.toList(),
                                "isCompleted" to false,
                                "createdAt" to System.currentTimeMillis()
                            )
                            FirebaseFirestore.getInstance()
                                .collection("events").document(eventId)
                                .collection("tasks")
                                .add(taskData).await()
                            onCreated()
                        } catch (_: Exception) { }
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Vytvořit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Zrušit") }
        }
    )
}
