package com.bettermingle.app.ui.screen.event

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bettermingle.app.R
import com.bettermingle.app.ui.component.BetterMingleTextField
import com.bettermingle.app.ui.component.EmptyState
import com.bettermingle.app.ui.theme.AccentOrange
import com.bettermingle.app.ui.theme.PrimaryBlue
import com.bettermingle.app.ui.theme.Spacing
import com.bettermingle.app.ui.theme.Success
import com.bettermingle.app.ui.theme.TextOnColor
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class BudgetCategory(
    val id: String,
    val name: String,
    val planned: Double,
    val spent: Double,
    val expenses: List<BudgetExpense> = emptyList()
)

private data class BudgetExpense(
    val id: String,
    val amount: Double,
    val note: String,
    val addedBy: String,
    val createdAt: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    eventId: String,
    onNavigateBack: () -> Unit
) {
    var categories by remember { mutableStateOf<List<BudgetCategory>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var isOrganizer by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    val firestore = remember { FirebaseFirestore.getInstance() }
    val budgetRef = remember { firestore.collection("events").document(eventId).collection("budgetCategories") }

    // Dialog states
    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<BudgetCategory?>(null) }
    var showDeleteDialog by remember { mutableStateOf<BudgetCategory?>(null) }
    var showExpenseDialog by remember { mutableStateOf<BudgetCategory?>(null) }
    var expandedCategoryId by remember { mutableStateOf<String?>(null) }

    suspend fun loadData() {
        try {
            // Check organizer
            val eventDoc = firestore.collection("events").document(eventId).get().await()
            val createdBy = eventDoc.getString("createdBy") ?: ""
            isOrganizer = createdBy == currentUserId
            if (!isOrganizer && currentUserId.isNotEmpty()) {
                val partDoc = firestore.collection("events").document(eventId)
                    .collection("participants").document(currentUserId).get().await()
                val role = partDoc.getString("role") ?: ""
                if (role.equals("CO_ORGANIZER", ignoreCase = true)) isOrganizer = true
            }

            // Load categories
            val snapshot = budgetRef.orderBy("createdAt").get().await()
            categories = snapshot.documents.map { doc ->
                // Load expenses per category
                val expensesSnapshot = budgetRef.document(doc.id)
                    .collection("expenses")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get().await()
                val expenses = expensesSnapshot.documents.map { expDoc ->
                    BudgetExpense(
                        id = expDoc.id,
                        amount = (expDoc.get("amount") as? Number)?.toDouble() ?: 0.0,
                        note = expDoc.getString("note") ?: "",
                        addedBy = expDoc.getString("addedByName") ?: "",
                        createdAt = (expDoc.get("createdAt") as? Number)?.toLong() ?: 0L
                    )
                }
                val spent = expenses.sumOf { it.amount }
                BudgetCategory(
                    id = doc.id,
                    name = doc.getString("name") ?: "",
                    planned = (doc.get("planned") as? Number)?.toDouble() ?: 0.0,
                    spent = spent,
                    expenses = expenses
                )
            }
        } catch (e: Exception) {
            scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.error_load_failed)) }
        }
        isLoading = false
        isRefreshing = false
    }

    LaunchedEffect(eventId) { loadData() }

    // --- Add/Edit category dialog ---
    if (showAddEditDialog) {
        val editing = editingCategory
        var categoryName by remember { mutableStateOf(editing?.name ?: "") }
        var categoryAmount by remember { mutableStateOf(if (editing != null) String.format("%.0f", editing.planned) else "") }
        val isEdit = editing != null

        AlertDialog(
            onDismissRequest = { showAddEditDialog = false; editingCategory = null },
            title = { Text(stringResource(if (isEdit) R.string.budget_edit_category else R.string.budget_add_category)) },
            text = {
                Column {
                    BetterMingleTextField(
                        value = categoryName,
                        onValueChange = { if (it.length <= 50) categoryName = it },
                        label = stringResource(R.string.budget_category_name)
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    BetterMingleTextField(
                        value = categoryAmount,
                        onValueChange = { categoryAmount = it.filter { c -> c.isDigit() || c == '.' } },
                        label = stringResource(R.string.budget_category_amount),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val amount = categoryAmount.toDoubleOrNull() ?: 0.0
                        if (categoryName.isNotBlank() && amount > 0) {
                            showAddEditDialog = false
                            val catId = editing?.id
                            editingCategory = null
                            scope.launch {
                                try {
                                    val data = mapOf(
                                        "name" to categoryName.trim(),
                                        "planned" to amount,
                                        "createdAt" to (editing?.let { System.currentTimeMillis() } ?: System.currentTimeMillis())
                                    )
                                    if (catId != null) {
                                        budgetRef.document(catId).update("name", categoryName.trim(), "planned", amount).await()
                                    } else {
                                        budgetRef.add(data).await()
                                    }
                                    loadData()
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar(context.getString(R.string.error_save_failed))
                                }
                            }
                        }
                    },
                    enabled = categoryName.isNotBlank() && (categoryAmount.toDoubleOrNull() ?: 0.0) > 0
                ) { Text(stringResource(if (isEdit) R.string.common_save else R.string.common_add)) }
            },
            dismissButton = {
                TextButton(onClick = { showAddEditDialog = false; editingCategory = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    // --- Delete confirmation dialog ---
    showDeleteDialog?.let { cat ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text(stringResource(R.string.budget_delete_title)) },
            text = { Text(stringResource(R.string.budget_delete_confirm, cat.name)) },
            confirmButton = {
                TextButton(onClick = {
                    val catId = cat.id
                    showDeleteDialog = null
                    scope.launch {
                        try {
                            // Delete expenses subcollection first
                            val expenses = budgetRef.document(catId).collection("expenses").get().await()
                            for (doc in expenses.documents) { doc.reference.delete().await() }
                            budgetRef.document(catId).delete().await()
                            loadData()
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar(context.getString(R.string.error_delete_failed))
                        }
                    }
                }) { Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }

    // --- Add expense dialog ---
    showExpenseDialog?.let { cat ->
        var expenseAmount by remember { mutableStateOf("") }
        var expenseNote by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showExpenseDialog = null },
            title = { Text(stringResource(R.string.budget_add_expense)) },
            text = {
                Column {
                    Text(
                        text = cat.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    BetterMingleTextField(
                        value = expenseAmount,
                        onValueChange = { expenseAmount = it.filter { c -> c.isDigit() || c == '.' } },
                        label = stringResource(R.string.budget_expense_amount),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    BetterMingleTextField(
                        value = expenseNote,
                        onValueChange = { if (it.length <= 100) expenseNote = it },
                        label = stringResource(R.string.budget_expense_note)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val amount = expenseAmount.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            val catId = cat.id
                            showExpenseDialog = null
                            scope.launch {
                                try {
                                    val userName = FirebaseAuth.getInstance().currentUser?.displayName ?: ""
                                    budgetRef.document(catId).collection("expenses").add(mapOf(
                                        "amount" to amount,
                                        "note" to expenseNote.trim(),
                                        "addedBy" to currentUserId,
                                        "addedByName" to userName,
                                        "createdAt" to System.currentTimeMillis()
                                    )).await()
                                    loadData()
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar(context.getString(R.string.error_save_failed))
                                }
                            }
                        }
                    },
                    enabled = (expenseAmount.toDoubleOrNull() ?: 0.0) > 0
                ) { Text(stringResource(R.string.common_add)) }
            },
            dismissButton = {
                TextButton(onClick = { showExpenseDialog = null }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }

    val totalPlanned = categories.sumOf { it.planned }
    val totalSpent = categories.sumOf { it.spent }
    val dateFormat = remember { SimpleDateFormat("d. M. HH:mm", Locale("cs", "CZ")) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.budget_title), style = MaterialTheme.typography.titleMedium) },
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
            if (isOrganizer) {
                FloatingActionButton(
                    onClick = { editingCategory = null; showAddEditDialog = true },
                    containerColor = AccentOrange,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.budget_add_category))
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }
            categories.isEmpty() -> {
                EmptyState(
                    icon = Icons.Default.Payments,
                    title = stringResource(R.string.budget_empty),
                    description = stringResource(R.string.budget_empty_description),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
            else -> {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        isRefreshing = true
                        scope.launch { loadData() }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(Spacing.screenPadding),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        // Summary card
                        item(key = "summary") {
                            val isOver = totalSpent > totalPlanned && totalPlanned > 0
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isOver) MaterialTheme.colorScheme.error else PrimaryBlue
                                ),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Column(modifier = Modifier.padding(Spacing.md)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(R.string.budget_total),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = TextOnColor.copy(alpha = 0.7f)
                                        )
                                        if (isOver) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.Warning,
                                                    contentDescription = null,
                                                    tint = TextOnColor,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = stringResource(R.string.budget_over_budget),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextOnColor
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "${String.format("%,.0f", totalPlanned)} Kč",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextOnColor
                                    )
                                    Spacer(modifier = Modifier.height(Spacing.sm))
                                    LinearProgressIndicator(
                                        progress = { if (totalPlanned > 0) (totalSpent / totalPlanned).toFloat().coerceIn(0f, 1f) else 0f },
                                        modifier = Modifier.fillMaxWidth(),
                                        color = if (isOver) TextOnColor else Success,
                                        trackColor = TextOnColor.copy(alpha = 0.2f)
                                    )
                                    Spacer(modifier = Modifier.height(Spacing.xs))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${stringResource(R.string.budget_spent)}: ${String.format("%,.0f", totalSpent)} Kč",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextOnColor.copy(alpha = 0.8f)
                                        )
                                        Text(
                                            text = "${stringResource(R.string.budget_remaining)}: ${String.format("%,.0f", totalPlanned - totalSpent)} Kč",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextOnColor.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(Spacing.md))
                        }

                        // Category cards
                        items(categories, key = { it.id }) { category ->
                            val isExpanded = expandedCategoryId == category.id
                            val isOverBudget = category.spent > category.planned && category.planned > 0
                            val progress = if (category.planned > 0) (category.spent / category.planned).toFloat().coerceIn(0f, 1f) else 0f

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                ),
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.animateContentSize()
                            ) {
                                Column(modifier = Modifier.padding(Spacing.md)) {
                                    // Header row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = category.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = "${String.format("%,.0f", category.spent)} / ${String.format("%,.0f", category.planned)} Kč",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        // Add expense button (everyone can add)
                                        IconButton(
                                            onClick = { showExpenseDialog = category },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = stringResource(R.string.budget_add_expense),
                                                tint = PrimaryBlue,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        if (isOrganizer) {
                                            IconButton(
                                                onClick = { editingCategory = category; showAddEditDialog = true },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Edit,
                                                    contentDescription = stringResource(R.string.budget_edit_category),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = { showDeleteDialog = category },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = stringResource(R.string.common_delete),
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(Spacing.xs))

                                    // Progress bar
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier.fillMaxWidth(),
                                        color = if (isOverBudget) MaterialTheme.colorScheme.error else PrimaryBlue
                                    )

                                    // Expand/collapse expenses
                                    if (category.expenses.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(Spacing.xs))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(MaterialTheme.shapes.small)
                                                .clickable {
                                                    expandedCategoryId = if (isExpanded) null else category.id
                                                }
                                                .padding(vertical = Spacing.xs),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${stringResource(R.string.budget_expense_history)} (${category.expenses.size})",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = PrimaryBlue,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Icon(
                                                if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                contentDescription = null,
                                                tint = PrimaryBlue,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        if (isExpanded) {
                                            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.xs))
                                            category.expenses.forEach { expense ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        if (expense.note.isNotBlank()) {
                                                            Text(
                                                                text = expense.note,
                                                                style = MaterialTheme.typography.bodySmall
                                                            )
                                                        }
                                                        Text(
                                                            text = buildString {
                                                                if (expense.addedBy.isNotBlank()) append("${expense.addedBy} · ")
                                                                if (expense.createdAt > 0) append(dateFormat.format(Date(expense.createdAt)))
                                                            },
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    Text(
                                                        text = "${String.format("%,.0f", expense.amount)} Kč",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
