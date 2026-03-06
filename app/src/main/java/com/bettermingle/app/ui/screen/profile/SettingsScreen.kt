package com.bettermingle.app.ui.screen.profile

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bettermingle.app.R
import com.bettermingle.app.data.preferences.PremiumTier
import com.bettermingle.app.ui.component.BetterMingleCard
import com.bettermingle.app.ui.component.BetterMingleTextField
import com.bettermingle.app.ui.theme.AccentGold
import com.bettermingle.app.ui.theme.AccentOrange
import com.bettermingle.app.ui.theme.BackgroundPrimary
import com.bettermingle.app.ui.theme.PrimaryBlue
import com.bettermingle.app.ui.theme.Spacing
import com.bettermingle.app.ui.theme.TextSecondary
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import com.bettermingle.app.viewmodel.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onAccountDeleted: () -> Unit,
    profileViewModel: ProfileViewModel = viewModel()
) {
    val profileState by profileViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showEditNameDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    if (showEditNameDialog) {
        var newName by remember { mutableStateOf(profileState.userName) }
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text(stringResource(R.string.settings_edit_name)) },
            text = {
                BetterMingleTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = stringResource(R.string.settings_edit_name_label)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                val user = FirebaseAuth.getInstance().currentUser
                                val profileUpdates = userProfileChangeRequest {
                                    displayName = newName
                                }
                                user?.updateProfile(profileUpdates)?.await()

                                val settingsManager = com.bettermingle.app.data.preferences.SettingsManager(context)
                                settingsManager.updateUserInfo(
                                    name = newName,
                                    email = user?.email ?: "",
                                    avatarUrl = user?.photoUrl?.toString() ?: ""
                                )

                                val uid = user?.uid
                                if (uid != null) {
                                    FirebaseFirestore.getInstance()
                                        .collection("users").document(uid)
                                        .update("displayName", newName).await()
                                }

                                Toast.makeText(context, context.getString(R.string.settings_name_updated), Toast.LENGTH_SHORT).show()
                                showEditNameDialog = false
                            } catch (_: Exception) {
                                Toast.makeText(context, context.getString(R.string.settings_name_update_error), Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = newName.isNotBlank()
                ) { Text(stringResource(R.string.common_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text(stringResource(R.string.settings_delete_account)) },
            text = {
                Text(stringResource(R.string.settings_delete_account_confirm))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                val user = FirebaseAuth.getInstance().currentUser
                                val uid = user?.uid

                                if (uid != null) {
                                    FirebaseFirestore.getInstance()
                                        .collection("users").document(uid)
                                        .delete().await()
                                }

                                val settingsManager = com.bettermingle.app.data.preferences.SettingsManager(context)
                                settingsManager.clearAll()

                                user?.delete()?.await()

                                onAccountDeleted()
                            } catch (_: Exception) {
                                Toast.makeText(context, context.getString(R.string.settings_delete_account_error), Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.common_delete), color = AccentOrange)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundPrimary
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
            // Section: Profil
            item {
                Text(
                    text = stringResource(R.string.settings_section_profile),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryBlue
                )
            }

            // Edit name
            item {
                BetterMingleCard(onClick = { showEditNameDialog = true }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(Spacing.iconMD)
                        )
                        Spacer(modifier = Modifier.width(Spacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_edit_name),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = profileState.userName.ifBlank { stringResource(R.string.settings_name_not_set) },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // Email (read-only)
            item {
                BetterMingleCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(Spacing.iconMD)
                        )
                        Spacer(modifier = Modifier.width(Spacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_email),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = profileState.userEmail.ifBlank { stringResource(R.string.settings_email_not_found) },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // Change password
            item {
                BetterMingleCard(onClick = {
                    val email = profileState.userEmail
                    if (email.isNotBlank()) {
                        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                            .addOnSuccessListener {
                                Toast.makeText(context, context.getString(R.string.settings_password_reset_sent), Toast.LENGTH_LONG).show()
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, context.getString(R.string.settings_password_reset_error), Toast.LENGTH_SHORT).show()
                            }
                    }
                }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(Spacing.iconMD)
                        )
                        Spacer(modifier = Modifier.width(Spacing.md))
                        Text(
                            text = stringResource(R.string.settings_change_password),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Section: Notifikace
            item {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = stringResource(R.string.settings_section_notifications),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryBlue
                )
            }

            item {
                BetterMingleCard {
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
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(Spacing.iconMD)
                            )
                            Spacer(modifier = Modifier.width(Spacing.md))
                            Text(
                                text = stringResource(R.string.settings_notifications_enable),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Switch(
                            checked = profileState.settings.notificationsEnabled,
                            onCheckedChange = { enabled ->
                                scope.launch {
                                    val settingsManager = com.bettermingle.app.data.preferences.SettingsManager(context)
                                    settingsManager.setNotificationsEnabled(enabled)
                                }
                            },
                            colors = SwitchDefaults.colors(checkedTrackColor = PrimaryBlue)
                        )
                    }
                }
            }

            // Section: Vzhled
            item {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = stringResource(R.string.settings_section_appearance),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryBlue
                )
            }

            item {
                BetterMingleCard {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DarkMode,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(Spacing.iconMD)
                            )
                            Spacer(modifier = Modifier.width(Spacing.md))
                            Text(
                                text = stringResource(R.string.settings_theme),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            val currentTheme = profileState.settings.themeMode
                            listOf(
                                "system" to R.string.settings_theme_system,
                                "light" to R.string.settings_theme_light,
                                "dark" to R.string.settings_theme_dark
                            ).forEach { (mode, labelRes) ->
                                val label = stringResource(labelRes)
                                androidx.compose.material3.FilterChip(
                                    selected = currentTheme == mode,
                                    onClick = {
                                        scope.launch {
                                            val settingsManager = com.bettermingle.app.data.preferences.SettingsManager(context)
                                            settingsManager.setThemeMode(mode)
                                        }
                                    },
                                    label = { Text(label) },
                                    colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryBlue.copy(alpha = 0.15f),
                                        selectedLabelColor = PrimaryBlue
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Section: Language
            item {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = stringResource(R.string.settings_section_language),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryBlue
                )
            }

            item {
                BetterMingleCard {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(Spacing.iconMD)
                            )
                            Spacer(modifier = Modifier.width(Spacing.md))
                            Text(
                                text = stringResource(R.string.settings_language),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            val currentLanguage = profileState.settings.appLanguage
                            val systemLabel = stringResource(R.string.settings_language_system)
                            listOf(
                                "system" to systemLabel,
                                "cs" to "Čeština",
                                "en" to "English"
                            ).forEach { (langCode, label) ->
                                androidx.compose.material3.FilterChip(
                                    selected = currentLanguage == langCode,
                                    onClick = {
                                        scope.launch {
                                            val settingsManager = com.bettermingle.app.data.preferences.SettingsManager(context)
                                            settingsManager.setAppLanguage(langCode)
                                        }
                                    },
                                    label = { Text(label) },
                                    colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryBlue.copy(alpha = 0.15f),
                                        selectedLabelColor = PrimaryBlue
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Section: O aplikaci
            item {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = stringResource(R.string.settings_section_about),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryBlue
                )
            }

            item {
                BetterMingleCard {
                    Column {
                        Text(
                            text = stringResource(R.string.settings_version, com.bettermingle.app.BuildConfig.VERSION_NAME),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(
                            text = stringResource(R.string.settings_copyright),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Section: Debug (only in debug builds)
            if (com.bettermingle.app.BuildConfig.DEBUG) {
                item {
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Text(
                        text = "Debug",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentGold
                    )
                }

                item {
                    BetterMingleCard {
                        Column {
                            Text(
                                text = stringResource(R.string.settings_debug_tier_switch),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(Spacing.xs))
                            Text(
                                text = stringResource(R.string.settings_debug_tier_current, profileState.settings.premiumTier.name),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                            ) {
                                PremiumTier.entries.forEach { tier ->
                                    val isSelected = profileState.settings.premiumTier == tier
                                    androidx.compose.material3.FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            scope.launch {
                                                val settingsManager = com.bettermingle.app.data.preferences.SettingsManager(context)
                                                settingsManager.setDebugTier(tier)
                                            }
                                        },
                                        label = { Text(tier.name) },
                                        colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = when (tier) {
                                                PremiumTier.FREE -> TextSecondary.copy(alpha = 0.15f)
                                                PremiumTier.PRO -> PrimaryBlue.copy(alpha = 0.15f)
                                                PremiumTier.BUSINESS -> AccentGold.copy(alpha = 0.15f)
                                            },
                                            selectedLabelColor = when (tier) {
                                                PremiumTier.FREE -> TextSecondary
                                                PremiumTier.PRO -> PrimaryBlue
                                                PremiumTier.BUSINESS -> AccentGold
                                            }
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section: Nebezpečná zóna
            item {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = stringResource(R.string.settings_section_danger),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AccentOrange
                )
            }

            item {
                BetterMingleCard(onClick = { showDeleteAccountDialog = true }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = AccentOrange,
                            modifier = Modifier.size(Spacing.iconMD)
                        )
                        Spacer(modifier = Modifier.width(Spacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_delete_account),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = AccentOrange
                            )
                            Text(
                                text = stringResource(R.string.settings_delete_account_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
