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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bettermingle.app.ui.component.BetterMingleTextField
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import com.bettermingle.app.data.model.Expense
import com.bettermingle.app.data.model.ExpenseSplit
import com.bettermingle.app.ui.component.BetterMingleCard
import com.bettermingle.app.ui.component.EmptyState
import com.bettermingle.app.ui.theme.AccentOrange
import com.bettermingle.app.ui.theme.PrimaryBlue
import com.bettermingle.app.ui.theme.Spacing
import com.bettermingle.app.ui.theme.Success
import com.bettermingle.app.ui.theme.SurfacePeach
import com.bettermingle.app.ui.theme.TextOnColor
import com.bettermingle.app.ui.theme.TextSecondary
import com.bettermingle.app.utils.CurrencyUtils
import com.bettermingle.app.utils.Debt
import com.bettermingle.app.utils.DebtCalculator
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    eventId: String,
    onNavigateBack: () -> Unit
) {
    val expenses = remember { mutableStateListOf<Expense>() }
    val debts = remember { mutableStateListOf<Debt>() }
    val payerNames = remember { mutableMapOf<String, String>() }
    val participants = remember { mutableStateListOf<Pair<String, String>>() }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Výdaje", "Vyrovnání")
    var showCreateDialog by remember { mutableStateOf(false) }
    var budgetLimit by remember { mutableStateOf(0.0) }
    val scope = rememberCoroutineScope()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val firestore = FirebaseFirestore.getInstance()

    fun loadExpenses() {
        scope.launch {
        try {
            val expensesSnapshot = firestore.collection("events").document(eventId)
                .collection("expenses").get().await()

            // Load user names for paidBy
            val userIds = expensesSnapshot.documents.mapNotNull { it.getString("paidBy") }.distinct()
            for (uid in userIds) {
                try {
                    val userDoc = firestore.collection("users").document(uid).get().await()
                    payerNames[uid] = userDoc.getString("displayName") ?: uid.take(8)
                } catch (_: Exception) { payerNames[uid] = uid.take(8) }
            }

            val loadedExpenses = expensesSnapshot.documents.map { doc ->
                val data = doc.data ?: emptyMap()
                Expense(
                    id = doc.id,
                    eventId = eventId,
                    paidBy = data["paidBy"] as? String ?: "",
                    description = data["description"] as? String ?: "",
                    amount = (data["amount"] as? Number)?.toDouble() ?: 0.0,
                    currency = data["currency"] as? String ?: "CZK",
                    category = data["category"] as? String ?: "",
                    receiptUrl = data["receiptUrl"] as? String ?: "",
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0
                )
            }.sortedByDescending { it.createdAt }

            expenses.clear()
            expenses.addAll(loadedExpenses)

            // Calculate debts from splits
            val totalPaid = mutableMapOf<String, Double>() // userId -> total they paid
            val totalOwed = mutableMapOf<String, Double>() // userId -> total they owe

            for (expense in loadedExpenses) {
                totalPaid[expense.paidBy] = (totalPaid[expense.paidBy] ?: 0.0) + expense.amount

                val splitsSnapshot = firestore.collection("events").document(eventId)
                    .collection("expenses").document(expense.id)
                    .collection("splits").get().await()

                for (splitDoc in splitsSnapshot.documents) {
                    val splitData = splitDoc.data ?: continue
                    val userId = splitData["userId"] as? String ?: continue
                    val amount = (splitData["amount"] as? Number)?.toDouble() ?: 0.0
                    val isSettled = splitData["isSettled"] as? Boolean ?: false
                    if (!isSettled) {
                        totalOwed[userId] = (totalOwed[userId] ?: 0.0) + amount
                        // Also load payer names for split users
                        if (userId !in payerNames) {
                            try {
                                val userDoc = firestore.collection("users").document(userId).get().await()
                                payerNames[userId] = userDoc.getString("displayName") ?: userId.take(8)
                            } catch (_: Exception) { payerNames[userId] = userId.take(8) }
                        }
                    }
                }
            }

            val calculatedDebts = DebtCalculator.calculateMinimumTransactions(totalPaid, totalOwed)
            debts.clear()
            debts.addAll(calculatedDebts)
        } catch (_: Exception) { }
        }
    }

    fun deleteExpense(expense: Expense) {
        scope.launch {
            try {
                val ref = firestore.collection("events").document(eventId)
                    .collection("expenses").document(expense.id)
                val splits = ref.collection("splits").get().await()
                for (doc in splits.documents) { doc.reference.delete().await() }
                ref.delete().await()
                loadExpenses()
            } catch (_: Exception) { }
        }
    }

    fun settleDebt(debt: Debt) {
        scope.launch {
            try {
                val expensesSnapshot = firestore.collection("events").document(eventId)
                    .collection("expenses").get().await()
                for (expenseDoc in expensesSnapshot.documents) {
                    val paidBy = expenseDoc.getString("paidBy") ?: continue
                    if (paidBy != debt.toUserId) continue
                    val splitsSnapshot = expenseDoc.reference.collection("splits").get().await()
                    for (splitDoc in splitsSnapshot.documents) {
                        val userId = splitDoc.getString("userId") ?: continue
                        if (userId == debt.fromUserId) {
                            splitDoc.reference.update("isSettled", true).await()
                        }
                    }
                }
                loadExpenses()
            } catch (_: Exception) { }
        }
    }

    LaunchedEffect(eventId) {
        loadExpenses()
        try {
            val doc = firestore.collection("events").document(eventId).get().await()
            budgetLimit = (doc.get("budgetLimit") as? Number)?.toDouble() ?: 0.0
        } catch (_: Exception) { }

        // Load participants for payer selection
        try {
            val participantsSnapshot = firestore.collection("events").document(eventId)
                .collection("participants").get().await()
            val loaded = mutableListOf<Pair<String, String>>()
            for (doc in participantsSnapshot.documents) {
                val uid = doc.getString("userId") ?: continue
                val name = try {
                    val userDoc = firestore.collection("users").document(uid).get().await()
                    userDoc.getString("displayName") ?: uid.take(8)
                } catch (_: Exception) { uid.take(8) }
                loaded.add(uid to name)
            }
            participants.clear()
            participants.addAll(loaded)
        } catch (_: Exception) { }
    }

    if (showCreateDialog) {
        AddExpenseDialog(
            eventId = eventId,
            participants = participants,
            onDismiss = { showCreateDialog = false },
            onCreated = {
                showCreateDialog = false
                loadExpenses()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Výdaje", style = MaterialTheme.typography.titleMedium) },
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
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = AccentOrange,
                    contentColor = TextOnColor
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nový výdaj")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SurfacePeach.copy(alpha = 0.3f),
                contentColor = PrimaryBlue,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = PrimaryBlue
                        )
                    }
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            // Budget progress card
            if (budgetLimit > 0) {
                val totalSpent = expenses.sumOf { it.amount }
                val progress = (totalSpent / budgetLimit).coerceIn(0.0, 1.5).toFloat()
                val isOverBudget = totalSpent > budgetLimit

                BetterMingleCard(
                    modifier = Modifier.padding(
                        horizontal = Spacing.screenPadding,
                        vertical = Spacing.sm
                    )
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Rozpočet",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${CurrencyUtils.formatCzk(totalSpent)} / ${CurrencyUtils.formatCzk(budgetLimit)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = if (isOverBudget) AccentOrange else PrimaryBlue
                            )
                        }

                        Spacer(modifier = Modifier.height(Spacing.sm))

                        LinearProgressIndicator(
                            progress = { progress.coerceAtMost(1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (isOverBudget) AccentOrange else PrimaryBlue,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeCap = StrokeCap.Round
                        )

                        if (isOverBudget) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Překročeno o ${CurrencyUtils.formatCzk(totalSpent - budgetLimit)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentOrange,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            when (selectedTab) {
                0 -> ExpensesList(
                    expenses = expenses,
                    payerNames = payerNames,
                    currentUserId = currentUserId,
                    onDelete = { expense -> deleteExpense(expense) }
                )
                1 -> DebtsList(
                    debts = debts,
                    userNames = payerNames,
                    onSettle = { debt -> settleDebt(debt) }
                )
            }
        }
    }
}

@Composable
private fun ExpensesList(
    expenses: List<Expense>,
    payerNames: Map<String, String>,
    currentUserId: String,
    onDelete: (Expense) -> Unit
) {
    if (expenses.isEmpty()) {
        EmptyState(
            icon = Icons.Default.Payments,
            title = "Zatím žádné výdaje",
            description = "Přidej výdaj a rozděl ho mezi účastníky.",
            modifier = Modifier.fillMaxSize()
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(Spacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            items(expenses, key = { it.id }) { expense ->
                ExpenseItem(
                    expense = expense,
                    payerName = payerNames[expense.paidBy] ?: expense.paidBy.take(8),
                    currentUserId = currentUserId,
                    onDelete = onDelete
                )
            }
        }
    }
}

@Composable
private fun ExpenseItem(
    expense: Expense,
    payerName: String,
    currentUserId: String,
    onDelete: (Expense) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Smazat výdaj") },
            text = { Text("Opravdu chceš smazat tento výdaj?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete(expense)
                }) {
                    Text("Smazat", color = AccentOrange)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Zrušit") }
            }
        )
    }

    BetterMingleCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.description,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Zaplatil/a: $payerName",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                if (expense.category.isNotEmpty()) {
                    Text(
                        text = expense.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryBlue
                    )
                }
            }

            Text(
                text = CurrencyUtils.formatAmount(expense.amount, expense.currency),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = AccentOrange
            )

            if (expense.paidBy == currentUserId) {
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Smazat výdaj",
                        tint = AccentOrange
                    )
                }
            }
        }
    }
}

@Composable
private fun DebtsList(
    debts: List<Debt>,
    userNames: Map<String, String>,
    onSettle: (Debt) -> Unit
) {
    if (debts.isEmpty()) {
        EmptyState(
            icon = Icons.Default.Receipt,
            title = "Vše vyrovnáno",
            description = "Momentálně nejsou žádné dluhy k vyrovnání.",
            modifier = Modifier.fillMaxSize()
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(Spacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            items(debts) { debt ->
                DebtItem(debt = debt, userNames = userNames, onSettle = onSettle)
            }
        }
    }
}

@Composable
private fun DebtItem(
    debt: Debt,
    userNames: Map<String, String>,
    onSettle: (Debt) -> Unit
) {
    BetterMingleCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = userNames[debt.fromUserId] ?: debt.fromUserId.take(8),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )

            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.padding(horizontal = Spacing.sm)
            )

            Text(
                text = userNames[debt.toUserId] ?: debt.toUserId.take(8),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = CurrencyUtils.formatCzk(debt.amount),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = AccentOrange
            )

            IconButton(onClick = { onSettle(debt) }) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Vyrovnat",
                    tint = Success
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExpenseDialog(
    eventId: String,
    participants: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onCreated: () -> Unit
) {
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    val currentUser = FirebaseAuth.getInstance().currentUser
    var selectedPayerId by remember { mutableStateOf(currentUser?.uid ?: "") }
    var payerDropdownExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val selectedPayerName = participants.firstOrNull { it.first == selectedPayerId }?.second
        ?: currentUser?.displayName ?: "Já"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nový výdaj") },
        text = {
            Column {
                BetterMingleTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = "Popis výdaje"
                )

                Spacer(modifier = Modifier.height(Spacing.formFieldSpacing))

                BetterMingleTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = "Částka (CZK)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                Spacer(modifier = Modifier.height(Spacing.formFieldSpacing))

                BetterMingleTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = "Kategorie (volitelné)"
                )

                if (participants.size > 1) {
                    Spacer(modifier = Modifier.height(Spacing.formFieldSpacing))

                    Text(
                        text = "Platil/a",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    ExposedDropdownMenuBox(
                        expanded = payerDropdownExpanded,
                        onExpandedChange = { payerDropdownExpanded = it }
                    ) {
                        BetterMingleTextField(
                            value = selectedPayerName,
                            onValueChange = {},
                            label = "Plátce",
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = payerDropdownExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )

                        ExposedDropdownMenu(
                            expanded = payerDropdownExpanded,
                            onDismissRequest = { payerDropdownExpanded = false }
                        ) {
                            participants.forEach { (uid, name) ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        selectedPayerId = uid
                                        payerDropdownExpanded = false
                                    }
                                )
                            }
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
                            val firestore = FirebaseFirestore.getInstance()
                            val parsedAmount = amount.replace(",", ".").toDoubleOrNull() ?: 0.0

                            val expenseData = hashMapOf(
                                "paidBy" to selectedPayerId,
                                "description" to description,
                                "amount" to parsedAmount,
                                "currency" to "CZK",
                                "category" to category,
                                "receiptUrl" to "",
                                "createdAt" to System.currentTimeMillis()
                            )
                            val expenseRef = firestore.collection("events").document(eventId)
                                .collection("expenses").add(expenseData).await()

                            // Create equal splits among all participants
                            val participantsSnapshot = firestore.collection("events").document(eventId)
                                .collection("participants").get().await()
                            val participantIds = participantsSnapshot.documents.mapNotNull {
                                it.getString("userId")
                            }
                            if (participantIds.isNotEmpty()) {
                                val splitAmount = parsedAmount / participantIds.size
                                for (uid in participantIds) {
                                    val splitData = hashMapOf(
                                        "userId" to uid,
                                        "amount" to splitAmount,
                                        "isSettled" to false
                                    )
                                    expenseRef.collection("splits").add(splitData).await()
                                }
                            }
                            onCreated()
                        } catch (_: Exception) { }
                    }
                },
                enabled = description.isNotBlank() && (amount.replace(",", ".").toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("Přidat")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Zrušit") }
        }
    )
}
