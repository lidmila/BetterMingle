package com.bettermingle.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey val id: String = "",
    val eventId: String = "",
    val userId: String = "",
    val userName: String = "",
    val content: String = "",
    val replyTo: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
