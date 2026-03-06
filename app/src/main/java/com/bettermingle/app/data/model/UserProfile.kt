package com.bettermingle.app.data.model

data class UserProfile(
    val id: String = "",
    val displayName: String = "",
    val email: String = "",
    val avatarUrl: String = "",
    val phone: String = "",
    val contactEmail: String = "",
    val department: String = "",
    val bio: String = "",
    val dietaryPreferences: List<String> = emptyList(),
    val isPremium: Boolean = false,
    val premiumUntil: Long? = null
)
