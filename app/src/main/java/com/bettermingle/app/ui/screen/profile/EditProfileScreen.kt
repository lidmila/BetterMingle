package com.bettermingle.app.ui.screen.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bettermingle.app.ui.component.BetterMingleTextField
import com.bettermingle.app.ui.component.AVATAR_COUNT
import com.bettermingle.app.ui.component.UserAvatar
import com.bettermingle.app.ui.component.builtInAvatarUrl
import com.bettermingle.app.ui.component.getAvatarResourceId
import com.bettermingle.app.ui.component.isBuiltInAvatar
import com.bettermingle.app.ui.component.parseAvatarIndex
import androidx.compose.material.icons.filled.Lock
import androidx.compose.ui.draw.alpha
import com.bettermingle.app.R
import com.bettermingle.app.ui.theme.AccentGold
import com.bettermingle.app.ui.theme.AccentPink
import com.bettermingle.app.ui.theme.PrimaryBlue
import com.bettermingle.app.ui.theme.Spacing
import com.bettermingle.app.ui.theme.TextOnColor

import com.bettermingle.app.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onNavigateBack: () -> Unit,
    profileViewModel: ProfileViewModel = viewModel()
) {
    val state by profileViewModel.uiState.collectAsState()

    var isInitialized by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }

    // Initialize fields once when profile data first loads
    if (!isInitialized && state.userName.isNotEmpty()) {
        name = state.userName
        contactEmail = state.contactEmail
        phone = state.phone
        department = state.department
        bio = state.bio
        isInitialized = true
    }
    var showAvatarPicker by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { profileViewModel.uploadAvatar(it) }
    }

    if (showAvatarPicker) {
        AvatarPickerDialog(
            currentAvatarUrl = state.userAvatarUrl,
            isPremium = state.isPremium,
            onSelectAvatar = { index ->
                profileViewModel.selectAvatar(builtInAvatarUrl(index))
                showAvatarPicker = false
            },
            onUploadPhoto = {
                showAvatarPicker = false
                photoPickerLauncher.launch("image/*")
            },
            onDismiss = { showAvatarPicker = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_profile_title), style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.screenPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clickable { showAvatarPicker = true },
                contentAlignment = Alignment.Center
            ) {
                if (state.isUploadingAvatar) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(PrimaryBlue.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = PrimaryBlue
                        )
                    }
                } else {
                    UserAvatar(
                        avatarUrl = state.userAvatarUrl,
                        displayName = state.userName,
                        size = 96.dp
                    )
                }
                // Camera overlay
                if (!state.isUploadingAvatar) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 4.dp, bottom = 4.dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(AccentPink),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = stringResource(R.string.edit_profile_change_avatar),
                            tint = TextOnColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Text(
                text = stringResource(R.string.edit_profile_change_avatar_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.xs)
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            BetterMingleTextField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(R.string.edit_profile_name)
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            BetterMingleTextField(
                value = contactEmail,
                onValueChange = { contactEmail = it },
                label = stringResource(R.string.edit_profile_contact_email),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            BetterMingleTextField(
                value = phone,
                onValueChange = { phone = it },
                label = stringResource(R.string.edit_profile_phone),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            BetterMingleTextField(
                value = department,
                onValueChange = { department = it },
                label = stringResource(R.string.edit_profile_department)
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            BetterMingleTextField(
                value = bio,
                onValueChange = { bio = it },
                label = stringResource(R.string.edit_profile_bio),
                singleLine = false,
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            Button(
                onClick = {
                    profileViewModel.updateProfile(
                        name = name,
                        contactEmail = contactEmail,
                        phone = phone,
                        department = department,
                        bio = bio
                    )
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving && name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = TextOnColor,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(stringResource(R.string.edit_profile_save), color = TextOnColor)
                }
            }
        }
    }
}

private const val FREE_AVATAR_LIMIT = 15

@Composable
private fun AvatarPickerDialog(
    currentAvatarUrl: String,
    isPremium: Boolean,
    onSelectAvatar: (Int) -> Unit,
    onUploadPhoto: () -> Unit,
    onDismiss: () -> Unit
) {
    val currentIndex = remember(currentAvatarUrl) { parseAvatarIndex(currentAvatarUrl) }
    val avatarIndices = remember { (1..AVATAR_COUNT).toList() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_profile_avatar_picker_title)) },
        text = {
            Column {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(Spacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    modifier = Modifier.height(320.dp)
                ) {
                    items(avatarIndices, key = { it }) { index ->
                        val resId = getAvatarResourceId(index)
                        val isSelected = currentIndex == index
                        val isLocked = index > FREE_AVATAR_LIMIT && !isPremium
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .then(
                                    if (isSelected) Modifier.border(3.dp, PrimaryBlue, CircleShape)
                                    else Modifier
                                )
                                .clickable {
                                    if (!isLocked) onSelectAvatar(index)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (resId != null) {
                                Image(
                                    painter = painterResource(id = resId),
                                    contentDescription = stringResource(R.string.edit_profile_avatar_description, index),
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .then(
                                            if (isLocked) Modifier.alpha(0.4f) else Modifier
                                        ),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            if (isLocked) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = stringResource(R.string.edit_profile_avatar_premium),
                                        tint = AccentGold,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.md))

                TextButton(
                    onClick = onUploadPhoto,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text(stringResource(R.string.edit_profile_upload_photo))
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.edit_profile_avatar_picker_close)) }
        }
    )
}
