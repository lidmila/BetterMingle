package com.bettermingle.app.utils

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot

/**
 * Safely access documents from QuerySnapshot.
 * On certain devices (e.g. 16KB page size), Firestore's getDocuments() may return null
 * despite its @NonNull annotation, causing NPE when iterating.
 * This extension property handles that case gracefully.
 */
@Suppress("USELESS_ELVIS")
val QuerySnapshot.safeDocuments: List<DocumentSnapshot>
    get() = documents ?: emptyList()
