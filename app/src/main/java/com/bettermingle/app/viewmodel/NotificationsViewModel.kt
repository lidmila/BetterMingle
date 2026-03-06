package com.bettermingle.app.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bettermingle.app.data.preferences.SettingsManager
import com.bettermingle.app.ui.screen.home.ActivityItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class NotificationsUiState(
    val activities: List<ActivityItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class NotificationsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsManager = SettingsManager(application)
    private val firestore = FirebaseFirestore.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    val unreadCount: StateFlow<Int> = settingsManager.getUnreadActivityCountFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        loadLocalActivities()
        loadRemoteActivities()
    }

    private fun loadLocalActivities() {
        viewModelScope.launch {
            settingsManager.getUserActivitiesFlow().collect { localEntries ->
                val localItems = localEntries.map { entry ->
                    ActivityItem(
                        id = entry.id,
                        eventId = entry.eventId,
                        eventName = entry.eventName,
                        actorName = entry.actorName,
                        actorId = entry.actorId,
                        type = entry.type,
                        description = entry.description,
                        timestamp = entry.timestamp,
                        isRead = true // own activities are always "read"
                    )
                }
                mergeActivities(local = localItems)
            }
        }
    }

    private var remoteActivities = emptyList<ActivityItem>()
    private var localActivities = emptyList<ActivityItem>()

    private fun mergeActivities(
        local: List<ActivityItem>? = null,
        remote: List<ActivityItem>? = null
    ) {
        if (local != null) localActivities = local
        if (remote != null) remoteActivities = remote

        // Merge: remote takes priority for items from other users,
        // local provides instant feedback for own activities
        val allById = mutableMapOf<String, ActivityItem>()

        // Add local first
        for (item in localActivities) {
            allById[item.id] = item
        }

        // Remote overwrites/adds (these include other users' activities)
        for (item in remoteActivities) {
            allById[item.id] = item
        }

        val merged = allById.values.sortedByDescending { it.timestamp }

        _uiState.value = _uiState.value.copy(
            activities = merged,
            isLoading = false,
            error = null
        )
    }

    fun loadRemoteActivities() {
        viewModelScope.launch {
            try {
                if (currentUserId.isEmpty()) return@launch

                val participantDocs = firestore.collectionGroup("participants")
                    .whereEqualTo("userId", currentUserId)
                    .get().await()

                val eventIds = participantDocs.documents.mapNotNull {
                    it.reference.parent.parent?.id
                }.distinct()

                // Load event names
                val eventNames = mutableMapOf<String, String>()
                for (eid in eventIds) {
                    try {
                        val doc = firestore.collection("events").document(eid).get().await()
                        eventNames[eid] = doc.getString("name") ?: ""
                    } catch (e: Exception) {
                        Log.e("NotificationsVM", "Failed to load event name ($eid): ${e.message}")
                    }
                }

                // Load activities — no orderBy to avoid index requirement
                val allRemote = mutableListOf<ActivityItem>()
                for (eid in eventIds) {
                    try {
                        val activityDocs = firestore.collection("events").document(eid)
                            .collection("activity")
                            .limit(50)
                            .get().await()

                        for (doc in activityDocs.documents) {
                            val data = doc.data ?: continue
                            allRemote.add(
                                ActivityItem(
                                    id = doc.id,
                                    eventId = eid,
                                    eventName = eventNames[eid] ?: "",
                                    actorName = data["actorName"] as? String ?: "",
                                    actorId = data["actorId"] as? String ?: "",
                                    type = data["type"] as? String ?: "",
                                    description = data["description"] as? String ?: "",
                                    timestamp = (data["timestamp"] as? Number)?.toLong() ?: 0,
                                    isRead = (data["readBy"] as? List<*>)?.contains(currentUserId) ?: false
                                )
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("NotificationsVM", "Failed to load activities ($eid): ${e.message}")
                    }
                }

                mergeActivities(remote = allRemote)

                // Mark all as read in Firestore
                for (activity in allRemote.filter { !it.isRead }) {
                    try {
                        firestore.collection("events").document(activity.eventId)
                            .collection("activity").document(activity.id)
                            .update("readBy", com.google.firebase.firestore.FieldValue.arrayUnion(currentUserId))
                    } catch (e: Exception) {
                        Log.e("NotificationsVM", "Failed to mark read: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("NotificationsVM", "Failed to load remote activities: ${e.message}")
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            settingsManager.clearUnreadActivityCount()
        }
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        loadRemoteActivities()
    }
}
