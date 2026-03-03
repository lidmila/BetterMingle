package com.bettermingle.app.ui.screen.event

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ScreenshotMonitor
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bettermingle.app.ui.component.BetterMingleButton
import com.bettermingle.app.ui.component.BetterMingleCard
import com.bettermingle.app.ui.component.BetterMingleOutlinedButton
import com.bettermingle.app.ui.component.BetterMingleTextField
import com.bettermingle.app.ui.theme.AccentOrange
import com.bettermingle.app.ui.theme.PrimaryBlue
import com.bettermingle.app.ui.theme.Spacing
import com.bettermingle.app.ui.theme.Success
import com.bettermingle.app.ui.theme.TextSecondary
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventSettingsScreen(
    eventId: String,
    onNavigateBack: () -> Unit,
    onEventDeleted: () -> Unit = onNavigateBack
) {
    var notificationsEnabled by remember { mutableStateOf(true) }
    var inviteCode by remember { mutableStateOf("") }
    var eventName by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var eventDescription by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var budgetLimitText by remember { mutableStateOf("") }

    // Security state (would be loaded from event data)
    var securityEnabled by remember { mutableStateOf(false) }
    var hideFinancials by remember { mutableStateOf(false) }
    var screenshotProtection by remember { mutableStateOf(false) }
    var requireApproval by remember { mutableStateOf(false) }

    LaunchedEffect(eventId) {
        try {
            val doc = FirebaseFirestore.getInstance()
                .collection("events").document(eventId).get().await()
            inviteCode = doc.getString("inviteCode") ?: ""
            eventName = doc.getString("name") ?: ""
            eventDescription = doc.getString("description") ?: ""
            securityEnabled = doc.getBoolean("securityEnabled") ?: false
            hideFinancials = doc.getBoolean("hideFinancials") ?: false
            screenshotProtection = doc.getBoolean("screenshotProtection") ?: false
            requireApproval = doc.getBoolean("requireApproval") ?: false
            val budget = (doc.get("budgetLimit") as? Number)?.toDouble() ?: 0.0
            budgetLimitText = if (budget > 0) budget.toLong().toString() else ""
        } catch (_: Exception) { }
    }

    val inviteLink = "https://bettermingle.app/invite/$inviteCode"
    val shareText = "Ahoj! Pojď se připojit k akci \"$eventName\" na BetterMingle: $inviteLink"

    if (showPinDialog) {
        var newPin by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Změnit PIN") },
            text = {
                Column {
                    Text(
                        text = "Zadej nový 4místný PIN pro akci",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    BetterMingleTextField(
                        value = newPin,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) newPin = it },
                        label = "Nový PIN"
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
                                    .update("pin", newPin).await()
                                Toast.makeText(context, "PIN změněn", Toast.LENGTH_SHORT).show()
                                showPinDialog = false
                            } catch (_: Exception) {
                                Toast.makeText(context, "Chyba při změně PINu", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = newPin.length == 4
                ) { Text("Uložit") }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) { Text("Zrušit") }
            }
        )
    }

    if (showEditDialog) {
        var editName by remember { mutableStateOf(eventName) }
        var editDescription by remember { mutableStateOf(eventDescription) }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Upravit akci") },
            text = {
                Column {
                    BetterMingleTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = "Název akce"
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    BetterMingleTextField(
                        value = editDescription,
                        onValueChange = { editDescription = it },
                        label = "Popis",
                        singleLine = false,
                        maxLines = 3
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
                                    .update(
                                        mapOf(
                                            "name" to editName,
                                            "description" to editDescription
                                        )
                                    ).await()
                                eventName = editName
                                eventDescription = editDescription
                                Toast.makeText(context, "Akce upravena", Toast.LENGTH_SHORT).show()
                                showEditDialog = false
                            } catch (_: Exception) {
                                Toast.makeText(context, "Chyba při úpravě", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = editName.isNotBlank()
                ) { Text("Uložit") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Zrušit") }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Smazat akci") },
            text = { Text("Opravdu chceš trvale smazat akci \"$eventName\" a všechna její data? Tuto akci nelze vrátit zpět.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        try {
                            val firestore = FirebaseFirestore.getInstance()
                            val eventRef = firestore.collection("events").document(eventId)
                            // Delete subcollections
                            val subcollections = listOf("participants", "polls", "expenses", "carpoolRides", "rooms", "schedule", "messages")
                            for (sub in subcollections) {
                                val docs = eventRef.collection(sub).get().await()
                                for (doc in docs.documents) {
                                    doc.reference.delete().await()
                                }
                            }
                            eventRef.delete().await()
                            onEventDeleted()
                        } catch (_: Exception) { }
                    }
                }) {
                    Text("Smazat", color = AccentOrange)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Zrušit") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nastavení akce", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
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
                            text = "Pozvánka",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(Spacing.sm))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            BetterMingleOutlinedButton(
                                text = "Kopírovat odkaz",
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("invite_link", inviteLink))
                                    Toast.makeText(context, "Odkaz zkopírován", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f)
                            )

                            BetterMingleButton(
                                text = "Sdílet",
                                onClick = {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Sdílet pozvánku"))
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
                                text = "Notifikace",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Upozornění na novinky v akci",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }

                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { notificationsEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = PrimaryBlue
                            )
                        )
                    }
                }
            }

            // Security section
            item {
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
                                        text = "Zvýšená ochrana",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (securityEnabled) "Aktivní" else "Vypnuto",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (securityEnabled) Success else TextSecondary
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
                                title = "Skrýt finance",
                                description = "Finanční detaily vidí jen organizátor",
                                checked = hideFinancials,
                                onCheckedChange = {
                                    hideFinancials = it
                                    scope.launch { try { FirebaseFirestore.getInstance().collection("events").document(eventId).update("hideFinancials", it).await() } catch (_: Exception) { } }
                                }
                            )

                            Spacer(modifier = Modifier.height(Spacing.sm))

                            SecuritySettingRow(
                                icon = Icons.Default.ScreenshotMonitor,
                                title = "Ochrana obrazovky",
                                description = "Blokovat snímky obrazovky",
                                checked = screenshotProtection,
                                onCheckedChange = {
                                    screenshotProtection = it
                                    scope.launch { try { FirebaseFirestore.getInstance().collection("events").document(eventId).update("screenshotProtection", it).await() } catch (_: Exception) { } }
                                }
                            )

                            Spacer(modifier = Modifier.height(Spacing.sm))

                            SecuritySettingRow(
                                icon = Icons.Default.VerifiedUser,
                                title = "Schvalování účastníků",
                                description = "Noví účastníci musí být schváleni",
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
                                    text = "Změnit PIN",
                                    onClick = { showPinDialog = true },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Budget
            item {
                BetterMingleCard {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Savings,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Text(
                                text = "Rozpočet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(Spacing.sm))

                        Text(
                            text = "Nastav limit rozpočtu pro akci",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(Spacing.sm))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            BetterMingleTextField(
                                value = budgetLimitText,
                                onValueChange = { budgetLimitText = it.filter { c -> c.isDigit() || c == '.' } },
                                label = "Limit (Kč)",
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )

                            BetterMingleButton(
                                text = "Uložit",
                                onClick = {
                                    scope.launch {
                                        try {
                                            val amount = budgetLimitText.replace(",", ".").toDoubleOrNull() ?: 0.0
                                            FirebaseFirestore.getInstance()
                                                .collection("events").document(eventId)
                                                .update("budgetLimit", amount).await()
                                            Toast.makeText(context, "Rozpočet uložen", Toast.LENGTH_SHORT).show()
                                        } catch (_: Exception) {
                                            Toast.makeText(context, "Chyba při ukládání", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Edit event
            item {
                SettingsActionItem(
                    icon = Icons.Default.Edit,
                    title = "Upravit akci",
                    description = "Změnit název, datum a popis",
                    onClick = { showEditDialog = true }
                )
            }

            // Danger zone
            item {
                Spacer(modifier = Modifier.height(Spacing.md))
                Text(
                    text = "Nebezpečná zóna",
                    style = MaterialTheme.typography.titleSmall,
                    color = AccentOrange,
                    fontWeight = FontWeight.SemiBold
                )
            }

            item {
                SettingsActionItem(
                    icon = Icons.Default.Delete,
                    title = "Smazat akci",
                    description = "Trvale smazat akci a všechna data",
                    onClick = { showDeleteDialog = true },
                    isDangerous = true
                )
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
                    color = TextSecondary
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

            Spacer(modifier = Modifier.padding(Spacing.md))

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
                    color = TextSecondary
                )
            }
        }
    }
}
