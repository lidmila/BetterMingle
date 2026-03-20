package com.bettermingle.app.utils

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

suspend fun removeModuleFromEvent(eventId: String, moduleKey: String) {
    val doc = FirebaseFirestore.getInstance().collection("events").document(eventId)
    val snapshot = doc.get().await()
    val current = (snapshot.get("enabledModules") as? List<*>)?.filterIsInstance<String>() ?: return
    doc.update("enabledModules", current.filter { it != moduleKey }).await()
}
