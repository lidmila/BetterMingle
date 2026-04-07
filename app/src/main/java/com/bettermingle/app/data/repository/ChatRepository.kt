package com.bettermingle.app.data.repository

import android.content.Context
import com.bettermingle.app.data.database.AppDatabase
import com.bettermingle.app.data.model.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import com.bettermingle.app.utils.safeDocuments

data class ChatMessageUi(
    val message: Message,
    val reactions: Map<String, List<String>> = emptyMap()
)

class ChatRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val messageDao = db.messageDao()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun getMessagesFlow(eventId: String): Flow<List<ChatMessageUi>> = callbackFlow {
        val listener = firestore.collection("events").document(eventId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, _ ->
                val messages = snapshot?.safeDocuments?.map { doc ->
                    val data = doc.data ?: emptyMap()
                    val message = Message(
                        id = doc.id,
                        eventId = eventId,
                        userId = data["userId"] as? String ?: "",
                        userName = data["userName"] as? String ?: "",
                        content = data["content"] as? String ?: "",
                        replyTo = data["replyTo"] as? String,
                        createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    )

                    @Suppress("UNCHECKED_CAST")
                    val rawReactions = data["reactions"] as? Map<String, List<String>> ?: emptyMap()

                    ChatMessageUi(message = message, reactions = rawReactions)
                } ?: emptyList()
                trySend(messages)
            }
        awaitClose { listener.remove() }
    }

    fun getLocalMessages(eventId: String): Flow<List<Message>> =
        messageDao.getMessagesByEvent(eventId)

    suspend fun sendMessage(eventId: String, content: String, replyTo: String? = null): String {
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("Not logged in")
        val messageId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val message = Message(
            id = messageId,
            eventId = eventId,
            userId = userId,
            userName = auth.currentUser?.displayName ?: "",
            content = content,
            replyTo = replyTo,
            createdAt = now
        )

        // Save locally
        messageDao.insertMessage(message)

        // Sync to Firestore
        try {
            val messageData = mapOf(
                "userId" to userId,
                "userName" to message.userName,
                "content" to content,
                "replyTo" to replyTo,
                "createdAt" to now
            )
            firestore.collection("events").document(eventId)
                .collection("messages").document(messageId)
                .set(messageData).await()
        } catch (e: Exception) {
            Log.e("ChatRepository", "Failed to sync message to cloud", e)
        }

        return messageId
    }

    suspend fun toggleReaction(eventId: String, messageId: String, emoji: String) {
        val userId = auth.currentUser?.uid ?: return
        val messageRef = firestore.collection("events").document(eventId)
            .collection("messages").document(messageId)

        try {
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(messageRef)
                @Suppress("UNCHECKED_CAST")
                val reactions = (snapshot.get("reactions") as? Map<String, List<String>>)
                    ?.toMutableMap() ?: mutableMapOf()

                val users = reactions[emoji]?.toMutableList() ?: mutableListOf()
                if (userId in users) {
                    users.remove(userId)
                } else {
                    users.add(userId)
                }

                if (users.isEmpty()) {
                    reactions.remove(emoji)
                } else {
                    reactions[emoji] = users
                }

                transaction.update(messageRef, "reactions", reactions)
            }.await()
        } catch (e: Exception) {
            Log.w("ChatRepository", "Failed to toggle reaction", e)
        }
    }

    suspend fun deleteMessage(eventId: String, messageId: String) {
        messageDao.deleteMessageById(messageId)
        try {
            firestore.collection("events").document(eventId)
                .collection("messages").document(messageId)
                .delete().await()
        } catch (e: Exception) {
            Log.w("ChatRepository", "Failed to delete message $messageId from cloud", e)
        }
    }
}
