package com.bettermingle.app.ui.screen.event

import com.bettermingle.app.R
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Fastfood
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.SportsBar
import androidx.compose.material.icons.rounded.Bed
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import com.bettermingle.app.utils.performHapticClick
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bettermingle.app.data.model.Expense
import com.bettermingle.app.data.model.ExpenseSplit
import com.bettermingle.app.data.ads.AdManager
import com.bettermingle.app.ui.component.BetterMingleCard
import com.bettermingle.app.ui.component.BetterMingleTextField
import com.bettermingle.app.ui.component.EmptyState
import com.bettermingle.app.ui.component.NativeAdCard
import com.bettermingle.app.ui.component.UserAvatar
import com.bettermingle.app.ui.component.ModuleColorPickerDialog
import com.bettermingle.app.data.repository.EventRepository
import androidx.compose.material.icons.filled.Palette
import com.bettermingle.app.ui.theme.AccentOrange
import com.bettermingle.app.ui.theme.AccentPink
import com.bettermingle.app.ui.theme.AccentPurple
import com.bettermingle.app.ui.theme.BalancePositive
import com.bettermingle.app.ui.theme.BalanceNegative
import com.bettermingle.app.ui.theme.BackgroundSecondary
import com.bettermingle.app.ui.theme.PrimaryBlue
import com.bettermingle.app.ui.theme.Spacing
import com.bettermingle.app.ui.theme.Success
import com.bettermingle.app.ui.theme.TextOnColor

import com.bettermingle.app.data.preferences.SettingsManager
import com.bettermingle.app.data.preferences.AppSettings
import com.bettermingle.app.utils.CurrencyUtils
import com.bettermingle.app.utils.Debt
import com.bettermingle.app.utils.DebtCalculator
import com.bettermingle.app.utils.ActivityLogger
import com.bettermingle.app.utils.ParticipantUtils
import com.bettermingle.app.utils.removeModuleFromEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// --- Helper functions ---

private fun categoryIcon(category: String): ImageVector = when (category.lowercase()) {
    "jídlo", "jidlo", "food" -> Icons.Rounded.Fastfood
    "doprava", "transport" -> Icons.Rounded.DirectionsCar
    "ubytování", "ubytovani", "accommodation" -> Icons.Rounded.Bed
    "nápoje", "napoje", "drinks" -> Icons.Rounded.SportsBar
    "ostatní", "other", "" -> Icons.AutoMirrored.Rounded.ReceiptLong
    else -> Icons.AutoMirrored.Rounded.ReceiptLong
}

@Composable
private fun categoryColor(category: String): Color = when (category.lowercase()) {
    "jídlo", "jidlo", "food" -> AccentOrange
    "doprava", "transport" -> PrimaryBlue
    "ubytování", "ubytovani", "accommodation" -> AccentPink
    "nápoje", "napoje", "drinks" -> AccentPurple
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun formatRelativeDate(timestamp: Long, todayStr: String, yesterdayStr: String): String {
    if (timestamp == 0L) return ""
    val now = Calendar.getInstance()
    val date = Calendar.getInstance().apply { timeInMillis = timestamp }

    return when {
        now.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR) -> todayStr
        now.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) - date.get(Calendar.DAY_OF_YEAR) == 1 -> yesterdayStr
        else -> SimpleDateFormat("d. M.", Locale.forLanguageTag("cs-CZ")).format(Date(timestamp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    eventId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val settings by settingsManager.settingsFlow.collectAsState(initial = AppSettings())
    val expenses = remember { mutableStateListOf<Expense>() }
    val debts = remember { mutableStateListOf<Debt>() }
    val payerNames = remember { mutableMapOf<String, String>() }
    val payerAvatars = remember { mutableMapOf<String, String>() }
    val participants = remember { mutableStateListOf<Pair<String, String>>() }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.expenses_tab_expenses),
        stringResource(R.string.expenses_tab_settlement)
    )
    var showCreateDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val firestore = FirebaseFirestore.getInstance()
    val snackbarHostState = remember { SnackbarHostState() }
    var isRefreshing by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var isOrganizer by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }

    LaunchedEffect(eventId) {
        try {
            val eventDoc = firestore.collection("events").document(eventId).get().await()
            isOrganizer = eventDoc.getString("createdBy") == currentUserId
        } catch (_: Exception) { }
    }

    val categoryOptions = listOf(
        stringResource(R.string.expenses_category_food),
        stringResource(R.string.expenses_category_transport),
        stringResource(R.string.expenses_category_accommodation),
        stringResource(R.string.expenses_category_drinks),
        stringResource(R.string.expenses_category_other)
    )

    fun loadExpenses() {
        scope.launch {
        try {
            val expensesSnapshot = firestore.collection("events").document(eventId)
                .collection("expenses").get().await()

            // Load user names and avatars for paidBy
            val userIds = expensesSnapshot.documents.mapNotNull { it.getString("paidBy") }.distinct()
            for (uid in userIds) {
                if (ParticipantUtils.isManualId(uid)) {
                    try {
                        val partDoc = firestore.collection("events").document(eventId)
                            .collection("participants").document(uid).get().await()
                        payerNames[uid] = partDoc.getString("displayName") ?: uid.take(8)
                        payerAvatars[uid] = ""
                    } catch (_: Exception) { payerNames[uid] = uid.take(8) }
                } else {
                    try {
                        val userDoc = firestore.collection("users").document(uid).get().await()
                        payerNames[uid] = userDoc.getString("displayName") ?: uid.take(8)
                        payerAvatars[uid] = userDoc.getString("avatarUrl") ?: ""
                    } catch (_: Exception) { payerNames[uid] = uid.take(8) }
                }
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
            val totalPaid = mutableMapOf<String, Double>()
            val totalOwed = mutableMapOf<String, Double>()

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
                        if (userId !in payerNames) {
                            if (ParticipantUtils.isManualId(userId)) {
                                try {
                                    val partDoc = firestore.collection("events").document(eventId)
                                        .collection("participants").document(userId).get().await()
                                    payerNames[userId] = partDoc.getString("displayName") ?: userId.take(8)
                                    payerAvatars[userId] = ""
                                } catch (_: Exception) { payerNames[userId] = userId.take(8) }
                            } else {
                                try {
                                    val userDoc = firestore.collection("users").document(userId).get().await()
                                    payerNames[userId] = userDoc.getString("displayName") ?: userId.take(8)
                                    payerAvatars[userId] = userDoc.getString("avatarUrl") ?: ""
                                } catch (_: Exception) { payerNames[userId] = userId.take(8) }
                            }
                        }
                    }
                }
            }

            val calculatedDebts = DebtCalculator.calculateMinimumTransactions(totalPaid, totalOwed)
            debts.clear()
            debts.addAll(calculatedDebts)
            isLoading = false
        } catch (_: Exception) { isLoading = false }
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
                ActivityLogger.log(eventId, "expense", context.getString(R.string.activity_deleted_expense, expense.description, expense.amount.toString()))
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
                val creditorName = payerNames[debt.toUserId] ?: debt.toUserId.take(8)
                ActivityLogger.log(eventId, "expense", context.getString(R.string.activity_settled_debt, creditorName))
                loadExpenses()
            } catch (_: Exception) { }
        }
    }

    LaunchedEffect(eventId) {
        loadExpenses()

        // Load participants for payer selection
        try {
            val participantsSnapshot = firestore.collection("events").document(eventId)
                .collection("participants").get().await()
            val loaded = mutableListOf<Pair<String, String>>()
            for (doc in participantsSnapshot.documents) {
                val uid = doc.getString("userId") ?: continue
                val isManual = doc.getBoolean("isManual") ?: ParticipantUtils.isManualId(uid)
                val name = if (isManual) {
                    doc.getString("displayName") ?: uid.take(8)
                } else {
                    try {
                        val userDoc = firestore.collection("users").document(uid).get().await()
                        userDoc.getString("displayName") ?: uid.take(8)
                    } catch (_: Exception) { uid.take(8) }
                }
                loaded.add(uid to name)
            }
            participants.clear()
            participants.addAll(loaded)
        } catch (_: Exception) { }
    }

    val expenseAddedMsg = stringResource(R.string.success_expense_added)
    val expenseDeletedMsg = stringResource(R.string.expenses_deleted)

    if (showCreateDialog) {
        AddExpenseDialog(
            eventId = eventId,
            participants = participants,
            categoryOptions = categoryOptions,
            onDismiss = { showCreateDialog = false },
            onCreated = {
                showCreateDialog = false
                loadExpenses()
                scope.launch { snackbarHostState.showSnackbar(expenseAddedMsg) }
            }
        )
    }

    if (showColorPicker) {
        ModuleColorPickerDialog(
            currentColor = AccentOrange,
            onColorSelected = { option ->
                showColorPicker = false
                scope.launch {
                    EventRepository(context).updateModuleColor(eventId, "EXPENSES", option.hex)
                }
            },
            onDismiss = { showColorPicker = false }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.expenses_title), style = MaterialTheme.typography.titleMedium) },
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
                                        removeModuleFromEvent(eventId, "EXPENSES")
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
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = Color.Transparent,
                    contentColor = TextOnColor,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                    modifier = Modifier
                        .shadow(8.dp, CircleShape, ambientColor = AccentOrange.copy(alpha = 0.3f), spotColor = AccentOrange.copy(alpha = 0.3f))
                        .background(AccentOrange, CircleShape)
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.expenses_new))
                }
            }
        }
    ) { innerPadding ->
        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                loadExpenses()
                scope.launch {
                    kotlinx.coroutines.delay(500)
                    isRefreshing = false
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // --- A. Summary card ---
            SummaryCard(
                expenses = expenses,
                debts = debts,
                currentUserId = currentUserId,
                payerNames = payerNames
            )

            // --- Tabs ---
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = BackgroundSecondary.copy(alpha = 0.5f),
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

            when (selectedTab) {
                0 -> ExpensesList(
                    expenses = expenses,
                    payerNames = payerNames,
                    currentUserId = currentUserId,
                    onDelete = { expense ->
                        deleteExpense(expense)
                        scope.launch { snackbarHostState.showSnackbar(expenseDeletedMsg) }
                    },
                    showAds = AdManager.hasAds(settings.premiumTier),
                    isLoading = isLoading
                )
                1 -> DebtsList(
                    debts = debts,
                    userNames = payerNames,
                    userAvatars = payerAvatars,
                    onSettle = { debt -> settleDebt(debt) },
                    isLoading = isLoading
                )
            }
        }
        }
    }
}

// --- A. Summary Card ---

@Composable
private fun SummaryCard(
    expenses: List<Expense>,
    debts: List<Debt>,
    currentUserId: String,
    payerNames: Map<String, String>
) {
    val totalSpent = expenses.sumOf { it.amount }
    val expenseCount = expenses.size

    // Calculate current user's balance: positive = others owe them, negative = they owe others
    val myDebtTo = debts.filter { it.fromUserId == currentUserId }.sumOf { it.amount }
    val myDebtFrom = debts.filter { it.toUserId == currentUserId }.sumOf { it.amount }
    val myBalance = myDebtFrom - myDebtTo

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = PrimaryBlue,
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
            )
            .padding(horizontal = Spacing.screenPadding, vertical = Spacing.md)
    ) {
        Column {
            // Total expenses
            Text(
                text = CurrencyUtils.formatCzk(totalSpent),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = TextOnColor
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = "$expenseCount ${when {
                    expenseCount == 1 -> stringResource(R.string.expenses_count_one)
                    expenseCount in 2..4 -> stringResource(R.string.expenses_count_few)
                    else -> stringResource(R.string.expenses_count_many)
                }}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextOnColor.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            // My balance
            if (debts.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color.White.copy(alpha = 0.15f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.expenses_my_balance),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextOnColor.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    when {
                        myBalance > 0.01 -> {
                            Text(
                                text = "+${CurrencyUtils.formatCzk(myBalance)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = BalancePositive // light green on dark bg
                            )
                            Spacer(modifier = Modifier.width(Spacing.xs))
                            Text(
                                text = stringResource(R.string.expenses_to_receive),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextOnColor.copy(alpha = 0.7f)
                            )
                        }
                        myBalance < -0.01 -> {
                            Text(
                                text = CurrencyUtils.formatCzk(-myBalance),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = BalanceNegative // light orange on dark bg
                            )
                            Spacer(modifier = Modifier.width(Spacing.xs))
                            Text(
                                text = stringResource(R.string.expenses_i_owe),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextOnColor.copy(alpha = 0.7f)
                            )
                        }
                        else -> {
                            Text(
                                text = stringResource(R.string.expenses_settled),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = BalancePositive
                            )
                        }
                    }
                }
            }

        }
    }
}

// --- B. Expenses List ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpensesList(
    expenses: List<Expense>,
    payerNames: Map<String, String>,
    currentUserId: String,
    onDelete: (Expense) -> Unit,
    showAds: Boolean = false,
    isLoading: Boolean = false
) {
    when {
        isLoading && expenses.isEmpty() -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator()
            }
        }
        expenses.isEmpty() -> {
            EmptyState(
                icon = Icons.Default.Payments,
                illustration = R.drawable.il_empty_expenses,
                iconDescription = stringResource(R.string.expenses_empty_icon),
                title = stringResource(R.string.expenses_empty_title),
                description = stringResource(R.string.expenses_empty_description),
                modifier = Modifier.fillMaxSize()
            )
        }
        else -> {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(Spacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            items(expenses, key = { it.id }) { expense ->
                if (expense.paidBy == currentUserId) {
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                onDelete(expense)
                                true
                            } else false
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(AccentOrange.copy(alpha = 0.15f))
                                    .padding(horizontal = Spacing.md),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.common_delete),
                                    tint = AccentOrange
                                )
                            }
                        },
                        enableDismissFromStartToEnd = false
                    ) {
                        ExpenseItem(
                            expense = expense,
                            payerName = payerNames[expense.paidBy] ?: expense.paidBy.take(8),
                            currentUserId = currentUserId,
                            onDelete = onDelete
                        )
                    }
                } else {
                    ExpenseItem(
                        expense = expense,
                        payerName = payerNames[expense.paidBy] ?: expense.paidBy.take(8),
                        currentUserId = currentUserId,
                        onDelete = onDelete
                    )
                }
            }

            if (showAds && expenses.isNotEmpty()) {
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

@Composable
private fun ExpenseItem(
    expense: Expense,
    payerName: String,
    currentUserId: String,
    onDelete: (Expense) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val todayStr = stringResource(R.string.common_date_today)
    val yesterdayStr = stringResource(R.string.common_date_yesterday)

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.expenses_delete_title)) },
            text = { Text(stringResource(R.string.expenses_delete_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete(expense)
                }) {
                    Text(stringResource(R.string.common_delete), color = AccentOrange)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }

    BetterMingleCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category icon in colored circle
            val cat = expense.category
            val icon = categoryIcon(cat)
            val color = categoryColor(cat)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(color.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = cat.ifEmpty { stringResource(R.string.expenses_item_default) },
                    tint = color,
                    modifier = Modifier.size(Spacing.iconMD)
                )
            }

            Spacer(modifier = Modifier.width(Spacing.md))

            // Description, payer, date
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.description,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = buildString {
                        append(stringResource(R.string.expenses_paid_by, payerName))
                        val dateStr = formatRelativeDate(expense.createdAt, todayStr, yesterdayStr)
                        if (dateStr.isNotEmpty()) {
                            append(" · $dateStr")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(Spacing.sm))

            // Amount
            Text(
                text = CurrencyUtils.formatAmount(expense.amount, expense.currency),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = AccentOrange
            )

            // Delete button for own expenses
            if (expense.paidBy == currentUserId) {
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.a11y_delete_expense),
                        tint = AccentOrange.copy(alpha = 0.6f),
                        modifier = Modifier.size(Spacing.iconSM)
                    )
                }
            }
        }
    }
}

// --- C. Debts List ---

@Composable
private fun DebtsList(
    debts: List<Debt>,
    userNames: Map<String, String>,
    userAvatars: Map<String, String>,
    onSettle: (Debt) -> Unit,
    isLoading: Boolean = false
) {
    when {
        isLoading && debts.isEmpty() -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator()
            }
        }
        debts.isEmpty() -> {
            EmptyState(
                icon = Icons.Default.CheckCircle,
                illustration = R.drawable.il_empty_debts,
                title = stringResource(R.string.expenses_settled_title),
                description = stringResource(R.string.expenses_settled_description),
                modifier = Modifier.fillMaxSize()
            )
        }
        else -> {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(Spacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            items(debts) { debt ->
                DebtItem(
                    debt = debt,
                    userNames = userNames,
                    userAvatars = userAvatars,
                    onSettle = onSettle
                )
            }
        }
        }
    }
}

@Composable
private fun DebtItem(
    debt: Debt,
    userNames: Map<String, String>,
    userAvatars: Map<String, String>,
    onSettle: (Debt) -> Unit
) {
    val fromName = userNames[debt.fromUserId] ?: debt.fromUserId.take(8)
    val toName = userNames[debt.toUserId] ?: debt.toUserId.take(8)
    val fromAvatar = userAvatars[debt.fromUserId] ?: ""

    BetterMingleCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar of debtor
            UserAvatar(
                avatarUrl = fromAvatar,
                displayName = fromName,
                size = 40.dp
            )

            Spacer(modifier = Modifier.width(Spacing.md))

            // "Jan dluží Petře" text
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fromName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.expenses_debt_owes).trim() + " " + toName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(Spacing.sm))

            // Amount
            Text(
                text = CurrencyUtils.formatCzk(debt.amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AccentOrange
            )

            Spacer(modifier = Modifier.width(Spacing.sm))

            // Settle button
            Button(
                onClick = { onSettle(debt) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Success,
                    contentColor = TextOnColor
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.expenses_settle),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// --- D. Add Expense Dialog ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExpenseDialog(
    eventId: String,
    participants: List<Pair<String, String>>,
    categoryOptions: List<String>,
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
    val hapticView = LocalView.current
    val context = LocalContext.current

    val payerMeLabel = stringResource(R.string.expenses_payer_me)
    val selectedPayerName = participants.firstOrNull { it.first == selectedPayerId }?.second
        ?: currentUser?.displayName ?: payerMeLabel

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.expenses_new)) },
        text = {
            Column {
                BetterMingleTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = stringResource(R.string.expenses_description_label)
                )

                Spacer(modifier = Modifier.height(Spacing.formFieldSpacing))

                BetterMingleTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = stringResource(R.string.expenses_amount_label),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                Spacer(modifier = Modifier.height(Spacing.formFieldSpacing))

                // Category chips
                Text(
                    text = stringResource(R.string.expenses_category_label),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    categoryOptions.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = {
                                category = if (category == cat) "" else cat
                            },
                            label = {
                                Text(
                                    text = cat,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = categoryIcon(cat),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            shape = RoundedCornerShape(100.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = categoryColor(cat).copy(alpha = 0.15f),
                                selectedLabelColor = categoryColor(cat),
                                selectedLeadingIconColor = categoryColor(cat)
                            )
                        )
                    }
                }

                if (participants.size > 1) {
                    Spacer(modifier = Modifier.height(Spacing.formFieldSpacing))

                    Text(
                        text = stringResource(R.string.expenses_payer),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    ExposedDropdownMenuBox(
                        expanded = payerDropdownExpanded,
                        onExpandedChange = { payerDropdownExpanded = it }
                    ) {
                        BetterMingleTextField(
                            value = selectedPayerName,
                            onValueChange = {},
                            label = stringResource(R.string.expenses_payer_label),
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
                                "currency" to CurrencyUtils.getDefaultCurrency(),
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
                            ActivityLogger.log(eventId, "expense", context.getString(R.string.activity_added_expense, description, parsedAmount.toString()))
                            hapticView.performHapticClick()
                            onCreated()
                        } catch (_: Exception) { }
                    }
                },
                enabled = description.isNotBlank() && (amount.replace(",", ".").toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text(stringResource(R.string.expenses_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}
