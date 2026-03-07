package com.bettermingle.app.ui.screen.event

import com.bettermingle.app.R
import androidx.compose.ui.res.stringResource
import com.bettermingle.app.utils.ActivityLogger
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bettermingle.app.data.repository.ChatMessageUi
import com.bettermingle.app.data.repository.ChatRepository
import com.bettermingle.app.ui.component.BetterMingleTextField
import com.bettermingle.app.ui.component.EmptyState
import com.bettermingle.app.data.ads.AdManager
import com.bettermingle.app.data.preferences.SettingsManager
import com.bettermingle.app.ui.component.NativeAdCard
import com.bettermingle.app.ui.theme.AccentPink
import com.bettermingle.app.ui.theme.PrimaryBlue
import com.bettermingle.app.ui.theme.Spacing
import com.bettermingle.app.ui.theme.TextOnColor

import androidx.compose.ui.platform.LocalView
import com.bettermingle.app.utils.DateFormatUtils
import com.bettermingle.app.utils.performHapticClick
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

private val REACTION_EMOJIS = listOf("\uD83D\uDC4D", "❤\uFE0F", "\uD83D\uDE02", "\uD83D\uDE2E", "\uD83D\uDE22", "\uD83D\uDD25")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    eventId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val settings by settingsManager.settingsFlow.collectAsState(initial = null)
    val showAds = settings?.let { AdManager.hasAds(it.premiumTier) } ?: false
    val chatRepository = remember { ChatRepository(context) }
    val messages by chatRepository.getMessagesFlow(eventId).collectAsState(initial = emptyList())
    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var showEmojiPickerForMessageId by remember { mutableStateOf<String?>(null) }
    val hapticView = LocalView.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dashboard_chat), style = MaterialTheme.typography.titleMedium) },
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
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                    .padding(horizontal = Spacing.screenPadding, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BetterMingleTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    label = stringResource(R.string.chat_input_placeholder),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(Spacing.sm))

                IconButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            val text = messageText
                            messageText = ""
                            scope.launch {
                                chatRepository.sendMessage(eventId, text)
                                val preview = if (text.length > 40) text.take(40) + "…" else text
                                ActivityLogger.log(eventId, "chat", context.getString(R.string.activity_sent_message, preview))
                            }
                        }
                    }
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.chat_send),
                        tint = if (messageText.isNotBlank()) PrimaryBlue else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    ) { innerPadding ->
        if (messages.isEmpty()) {
            EmptyState(
                icon = Icons.AutoMirrored.Filled.Chat,
                illustration = R.drawable.il_empty_chat,
                title = stringResource(R.string.chat_empty_title),
                description = stringResource(R.string.chat_empty_description),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                state = listState,
                contentPadding = PaddingValues(Spacing.screenPadding),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                reverseLayout = true
            ) {
                items(messages, key = { it.message.id }) { chatMessage ->
                    ChatMessageItem(
                        chatMessage = chatMessage,
                        isOwnMessage = chatMessage.message.userId == currentUserId,
                        currentUserId = currentUserId,
                        showEmojiPicker = showEmojiPickerForMessageId == chatMessage.message.id,
                        onLongPress = {
                            showEmojiPickerForMessageId = if (showEmojiPickerForMessageId == chatMessage.message.id) null else chatMessage.message.id
                        },
                        onEmojiSelected = { emoji ->
                            showEmojiPickerForMessageId = null
                            hapticView.performHapticClick()
                            scope.launch {
                                chatRepository.toggleReaction(eventId, chatMessage.message.id, emoji)
                            }
                        },
                        onReactionTap = { emoji ->
                            hapticView.performHapticClick()
                            scope.launch {
                                chatRepository.toggleReaction(eventId, chatMessage.message.id, emoji)
                            }
                        }
                    )
                }

                if (showAds && messages.isNotEmpty()) {
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChatMessageItem(
    chatMessage: ChatMessageUi,
    isOwnMessage: Boolean,
    currentUserId: String,
    showEmojiPicker: Boolean,
    onLongPress: () -> Unit,
    onEmojiSelected: (String) -> Unit,
    onReactionTap: (String) -> Unit
) {
    val message = chatMessage.message

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start
    ) {
        // Emoji picker popup
        AnimatedVisibility(
            visible = showEmojiPicker,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 4.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    REACTION_EMOJIS.forEach { emoji ->
                        Text(
                            text = emoji,
                            fontSize = 22.sp,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onEmojiSelected(emoji) }
                                .padding(6.dp)
                        )
                    }
                }
            }
        }

        if (showEmojiPicker) {
            Spacer(modifier = Modifier.height(4.dp))
        }

        if (!isOwnMessage) {
            Text(
                text = message.userName.ifEmpty { message.userId.take(8) },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryBlue
            )
            Spacer(modifier = Modifier.height(2.dp))
        }

        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(
                    if (isOwnMessage) PrimaryBlue.copy(alpha = 0.1f)
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { onLongPress() }
                    )
                }
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // Reactions row
        if (chatMessage.reactions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                chatMessage.reactions.forEach { (emoji, userIds) ->
                    if (userIds.isNotEmpty()) {
                        val isReactedByMe = currentUserId in userIds
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isReactedByMe) PrimaryBlue.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onReactionTap(emoji) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(text = emoji, fontSize = 14.sp)
                                Text(
                                    text = "${userIds.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isReactedByMe) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isReactedByMe) PrimaryBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = DateFormatUtils.formatTime(message.createdAt),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
