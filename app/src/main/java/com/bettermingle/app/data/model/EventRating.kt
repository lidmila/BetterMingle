package com.bettermingle.app.data.model

data class EventRating(
    val id: String = "",
    val eventId: String = "",
    val userId: String = "",
    val overallRating: Int = 0, // 1-5
    val comment: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
