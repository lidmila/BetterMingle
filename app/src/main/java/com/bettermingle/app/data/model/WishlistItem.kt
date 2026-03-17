package com.bettermingle.app.data.model

enum class WishlistItemStatus {
    FREE, RESERVED, BOUGHT
}

data class WishlistItem(
    val id: String = "",
    val eventId: String = "",
    val name: String = "",
    val price: String? = null,
    val productUrl: String? = null,
    val description: String? = null,
    val status: WishlistItemStatus = WishlistItemStatus.FREE,
    val claimedBy: String? = null,
    val claimedByName: String? = null,
    val addedBy: String = "",
    val createdAt: Long = 0L
)
