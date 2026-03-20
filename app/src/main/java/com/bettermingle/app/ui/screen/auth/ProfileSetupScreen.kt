package com.bettermingle.app.ui.screen.auth

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bettermingle.app.R
import com.bettermingle.app.data.preferences.PremiumTier
import com.bettermingle.app.data.preferences.TierLimits
import com.bettermingle.app.ui.component.AVATAR_COUNT
import com.bettermingle.app.ui.component.BetterMingleButton
import com.bettermingle.app.ui.component.BetterMingleTextField
import com.bettermingle.app.ui.component.UserAvatar
import com.bettermingle.app.ui.component.builtInAvatarUrl
import com.bettermingle.app.ui.component.getAvatarResourceId
import com.bettermingle.app.ui.component.parseAvatarIndex
import com.bettermingle.app.ui.theme.AccentGold
import com.bettermingle.app.ui.theme.AccentPink
import com.bettermingle.app.ui.theme.BetterMingleMotion
import com.bettermingle.app.ui.theme.PrimaryBlue
import com.bettermingle.app.ui.theme.Spacing
import com.bettermingle.app.ui.theme.TextOnColor
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

@Composable
fun ProfileSetupScreen(
    onComplete: () -> Unit
) {
    val user = FirebaseAuth.getInstance().currentUser
    var isCheckingStatus by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var formVisible by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf(user?.displayName ?: "") }
    var phone by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var avatarUrl by remember { mutableStateOf("") }
    var isUploadingAvatar by remember { mutableStateOf(false) }
    var showAvatarPicker by remember { mutableStateOf(false) }
    var premiumTier by remember { mutableStateOf(PremiumTier.FREE) }

    // Check if profile setup is already completed + load premium tier
    LaunchedEffect(Unit) {
        val uid = user?.uid
        if (uid == null) {
            onComplete()
            return@LaunchedEffect
        }
        try {
            val doc = FirebaseFirestore.getInstance()
                .collection("users").document(uid).get().await()
            if (doc.getBoolean("profileSetupCompleted") == true) {
                onComplete()
                return@LaunchedEffect
            }
            // Load premium tier for avatar gating
            val tier = try {
                doc.getString("premiumTier")?.let { PremiumTier.valueOf(it) }
            } catch (_: Exception) { null }
            if (tier != null) premiumTier = tier
        } catch (_: Exception) {
            // Continue to show setup form
        }
        isCheckingStatus = false
        formVisible = true
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val uid = user?.uid ?: return@let
            isUploadingAvatar = true
            val storageRef = FirebaseStorage.getInstance()
                .reference.child("profile_photos/$uid.jpg")
            storageRef.putFile(it)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        avatarUrl = downloadUri.toString()
                        isUploadingAvatar = false
                    }
                }
                .addOnFailureListener { isUploadingAvatar = false }
        }
    }

    if (showAvatarPicker) {
        SetupAvatarPickerDialog(
            currentAvatarUrl = avatarUrl,
            premiumTier = premiumTier,
            onSelectAvatar = { index ->
                avatarUrl = builtInAvatarUrl(index)
                showAvatarPicker = false
            },
            onUploadPhoto = {
                showAvatarPicker = false
                photoPickerLauncher.launch("image/*")
            },
            onDismiss = { showAvatarPicker = false }
        )
    }

    if (isCheckingStatus) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = PrimaryBlue)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(Spacing.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(Spacing.xxl))

        AnimatedVisibility(
            visible = formVisible,
            enter = fadeIn(tween(BetterMingleMotion.STANDARD)) +
                    slideInVertically(tween(BetterMingleMotion.STANDARD)) { it / 3 }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.profile_setup_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(Spacing.sm))

                Text(
                    text = stringResource(R.string.profile_setup_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.xl))

        // Avatar
        AnimatedVisibility(
            visible = formVisible,
            enter = fadeIn(tween(BetterMingleMotion.STANDARD, delayMillis = 100)) +
                    slideInVertically(tween(BetterMingleMotion.STANDARD, delayMillis = 100)) { it / 3 }
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clickable { showAvatarPicker = true },
                contentAlignment = Alignment.Center
            ) {
                if (isUploadingAvatar) {
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
                } else if (avatarUrl.isNotEmpty()) {
                    UserAvatar(
                        avatarUrl = avatarUrl,
                        displayName = name,
                        size = 96.dp
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(PrimaryBlue.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                if (!isUploadingAvatar) {
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
                            contentDescription = stringResource(R.string.profile_setup_choose_avatar),
                            tint = TextOnColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.sm))

        Text(
            text = stringResource(R.string.profile_setup_choose_avatar),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        // Form fields
        AnimatedVisibility(
            visible = formVisible,
            enter = fadeIn(tween(BetterMingleMotion.STANDARD, delayMillis = 200)) +
                    slideInVertically(tween(BetterMingleMotion.STANDARD, delayMillis = 200)) { it / 3 }
        ) {
            Column {
                BetterMingleTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(R.string.profile_setup_name) + " *"
                )

                Spacer(modifier = Modifier.height(Spacing.md))

                BetterMingleTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = stringResource(R.string.profile_setup_phone),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                Spacer(modifier = Modifier.height(Spacing.md))

                BetterMingleTextField(
                    value = department,
                    onValueChange = { department = it },
                    label = stringResource(R.string.profile_setup_department)
                )

                Spacer(modifier = Modifier.height(Spacing.md))

                BetterMingleTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = stringResource(R.string.profile_setup_bio),
                    singleLine = false,
                    maxLines = 4
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.xl))

        // Buttons
        AnimatedVisibility(
            visible = formVisible,
            enter = fadeIn(tween(BetterMingleMotion.STANDARD, delayMillis = 300)) +
                    slideInVertically(tween(BetterMingleMotion.STANDARD, delayMillis = 300)) { it / 2 }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isSaving) {
                    CircularProgressIndicator(color = PrimaryBlue)
                } else {
                    BetterMingleButton(
                        text = stringResource(R.string.profile_setup_save),
                        onClick = {
                            val uid = user?.uid ?: return@BetterMingleButton
                            isSaving = true
                            val data = mutableMapOf<String, Any>(
                                "displayName" to name.trim(),
                                "phone" to phone.trim(),
                                "department" to department.trim(),
                                "bio" to bio.trim(),
                                "profileSetupCompleted" to true
                            )
                            if (avatarUrl.isNotEmpty()) {
                                data["avatarUrl"] = avatarUrl
                            }
                            FirebaseFirestore.getInstance()
                                .collection("users").document(uid)
                                .set(data, SetOptions.merge())
                                .addOnSuccessListener {
                                    // Update Firebase Auth displayName
                                    user.updateProfile(
                                        userProfileChangeRequest { displayName = name.trim() }
                                    ).addOnCompleteListener {
                                        if (avatarUrl.isNotEmpty()) {
                                            user.updateProfile(
                                                userProfileChangeRequest {
                                                    photoUri = Uri.parse(avatarUrl)
                                                }
                                            ).addOnCompleteListener { onComplete() }
                                        } else {
                                            onComplete()
                                        }
                                    }
                                }
                                .addOnFailureListener { isSaving = false }
                        },
                        isCta = true,
                        enabled = name.isNotBlank()
                    )
                }

            }
        }

        Spacer(modifier = Modifier.height(Spacing.lg))
    }
}

@Composable
private fun SetupAvatarPickerDialog(
    currentAvatarUrl: String,
    premiumTier: PremiumTier = PremiumTier.FREE,
    onSelectAvatar: (Int) -> Unit,
    onUploadPhoto: () -> Unit,
    onDismiss: () -> Unit
) {
    val currentIndex = remember(currentAvatarUrl) { parseAvatarIndex(currentAvatarUrl) }
    val avatarIndices = remember { (1..AVATAR_COUNT).toList() }
    val maxAvatars = remember(premiumTier) { TierLimits.maxAvatars(premiumTier) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_setup_avatar_picker_title)) },
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
                        val isLocked = index > maxAvatars
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
                                        .then(if (isLocked) Modifier.background(
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                                        ) else Modifier),
                                    contentScale = ContentScale.Crop,
                                    alpha = if (isLocked) 0.4f else 1f
                                )
                            }
                            if (isLocked) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = AccentGold,
                                    modifier = Modifier.size(20.dp)
                                )
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
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_close)) }
        }
    )
}
