package com.bettermingle.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PollType {
    DATE, LOCATION, ACTIVITY, PRICE, CUSTOM
}

@Entity(tableName = "polls")
data class Poll(
    @PrimaryKey val id: String = "",
    val eventId: String = "",
    val createdBy: String = "",
    val title: String = "",
    val pollType: PollType = PollType.CUSTOM,
    val allowMultiple: Boolean = false,
    val isAnonymous: Boolean = false,
    val deadline: Long? = null,
    val isClosed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "poll_options")
data class PollOption(
    @PrimaryKey val id: String = "",
    val pollId: String = "",
    val label: String = "",
    val description: String = "",
    val sortOrder: Int = 0
)

data class PollVote(
    val id: String = "",
    val optionId: String = "",
    val userId: String = "",
    val value: Int = 1 // 1=ano, 0=možná, -1=ne
)
