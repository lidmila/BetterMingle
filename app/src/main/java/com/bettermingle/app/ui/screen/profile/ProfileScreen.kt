package com.bettermingle.app.ui.screen.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.AutoAwesome
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bettermingle.app.R
import com.bettermingle.app.ui.component.BetterMingleCard
import com.bettermingle.app.ui.component.UserAvatar
import com.bettermingle.app.viewmodel.ProfileViewModel
import com.bettermingle.app.ui.theme.AccentGold
import com.bettermingle.app.ui.theme.AccentOrange
import com.bettermingle.app.ui.theme.AccentPink
import com.bettermingle.app.ui.theme.BetterMingleMotion
import com.bettermingle.app.ui.theme.PrimaryBlue
import com.bettermingle.app.ui.theme.Spacing
import com.bettermingle.app.ui.theme.TextOnColor


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onUpgrade: () -> Unit = {},
    onSettings: () -> Unit = {},
    onHelp: () -> Unit = {},
    onEditProfile: () -> Unit = {},
    onYearInReview: () -> Unit = {},
    profileViewModel: ProfileViewModel = viewModel()
) {
    val profileState by profileViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                profileViewModel.refreshProfile()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.profile_title), style = MaterialTheme.typography.headlineSmall)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        var visible by remember { mutableStateOf(false) }
        val avatarScale = remember { Animatable(0.5f) }

        LaunchedEffect(Unit) {
            visible = true
            avatarScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.screenPadding)
        ) {
            // Gradient header with avatar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                    .background(PrimaryBlue)
                    .padding(vertical = Spacing.lg, horizontal = Spacing.md),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .scale(avatarScale.value)
                            .border(3.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                            .clickable { onEditProfile() }
                    ) {
                        UserAvatar(
                            avatarUrl = profileState.userAvatarUrl,
                            displayName = profileState.userName,
                            size = 80.dp
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.sm))

                    Text(
                        text = profileState.userName.ifBlank { stringResource(R.string.profile_default_user) },
                        style = MaterialTheme.typography.titleMedium,
                        color = TextOnColor
                    )
                    Text(
                        text = profileState.userEmail,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextOnColor.copy(alpha = 0.8f)
                    )

                    if (profileState.bio.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(
                            text = profileState.bio,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextOnColor.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // Edit profile button
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(BetterMingleMotion.STANDARD, delayMillis = 50)) +
                        slideInVertically(tween(BetterMingleMotion.STANDARD, delayMillis = 50)) { it / 3 }
            ) {
                BetterMingleCard(onClick = onEditProfile) {
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
                        Text(
                            text = stringResource(R.string.profile_edit),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Contact info cards (only show if filled)
            val hasContactInfo = profileState.contactEmail.isNotEmpty()
                    || profileState.phone.isNotEmpty()
                    || profileState.department.isNotEmpty()

            if (hasContactInfo) {
                Spacer(modifier = Modifier.height(Spacing.md))

                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(BetterMingleMotion.STANDARD, delayMillis = 100)) +
                            slideInVertically(tween(BetterMingleMotion.STANDARD, delayMillis = 100)) { it / 3 }
                ) {
                    BetterMingleCard {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            if (profileState.contactEmail.isNotEmpty()) {
                                ContactInfoRow(
                                    icon = Icons.Default.Email,
                                    label = stringResource(R.string.profile_contact_email),
                                    value = profileState.contactEmail
                                )
                            }
                            if (profileState.phone.isNotEmpty()) {
                                ContactInfoRow(
                                    icon = Icons.Default.Phone,
                                    label = stringResource(R.string.profile_phone),
                                    value = profileState.phone
                                )
                            }
                            if (profileState.department.isNotEmpty()) {
                                ContactInfoRow(
                                    icon = Icons.Default.Business,
                                    label = stringResource(R.string.profile_department),
                                    value = profileState.department
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            // Premium card
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(BetterMingleMotion.STANDARD, delayMillis = 150)) +
                        slideInVertically(tween(BetterMingleMotion.STANDARD, delayMillis = 150)) { it / 3 }
            ) {
            BetterMingleCard(
                onClick = onUpgrade,
                modifier = Modifier.border(
                    2.dp,
                    AccentGold,
                    MaterialTheme.shapes.medium
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = AccentGold,
                        modifier = Modifier.size(Spacing.iconLG)
                    )
                    Spacer(modifier = Modifier.width(Spacing.md))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.profile_premium),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = stringResource(R.string.profile_premium_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            // Settings section
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(BetterMingleMotion.STANDARD, delayMillis = 200)) +
                        slideInVertically(tween(BetterMingleMotion.STANDARD, delayMillis = 200)) { it / 3 }
            ) {
            Column {
            ProfileMenuItem(
                icon = Icons.Default.Settings,
                title = stringResource(R.string.profile_settings),
                iconTint = PrimaryBlue,
                onClick = onSettings
            )
            ProfileMenuItem(
                icon = Icons.Default.Notifications,
                title = stringResource(R.string.profile_notifications),
                iconTint = AccentGold,
                onClick = {
                    try {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Log.e("ProfileScreen", "Cannot open notification settings", e)
                    }
                }
            )
            ProfileMenuItem(
                icon = Icons.AutoMirrored.Filled.HelpOutline,
                title = stringResource(R.string.profile_help),
                iconTint = AccentPink,
                onClick = onHelp
            )
            ProfileMenuItem(
                icon = Icons.Default.AutoAwesome,
                title = stringResource(R.string.profile_year_in_events),
                iconTint = PrimaryBlue,
                onClick = onYearInReview
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            ProfileMenuItem(
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                title = stringResource(R.string.profile_logout),
                iconTint = AccentOrange,
                onClick = onLogout
            )
            }
            }
        }
    }
}

@Composable
private fun ContactInfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PrimaryBlue,
            modifier = Modifier.size(Spacing.iconSM)
        )
        Spacer(modifier = Modifier.width(Spacing.sm))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    iconTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit
) {
    BetterMingleCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(Spacing.iconMD)
            )
            Spacer(modifier = Modifier.width(Spacing.md))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    Spacer(modifier = Modifier.height(Spacing.sm))
}
