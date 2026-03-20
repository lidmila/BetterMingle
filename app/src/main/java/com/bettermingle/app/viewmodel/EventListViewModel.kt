package com.bettermingle.app.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bettermingle.app.data.model.Event
import com.bettermingle.app.data.model.EventStatus
import com.bettermingle.app.data.preferences.SettingsManager
import com.bettermingle.app.data.repository.EventRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar

data class EventListUiState(
    val events: List<Event> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val searchQuery: String = "",
    val statusFilter: EventStatus? = null,
    val participantCounts: Map<String, Int> = emptyMap(),
    val yearInReviewDismissed: Boolean = false
) {
    val filteredEvents: List<Event>
        get() {
            var result = events
            if (searchQuery.isNotBlank()) {
                result = result.filter {
                    it.name.contains(searchQuery, ignoreCase = true)
                }
            }
            if (statusFilter != null) {
                result = result.filter { it.status == statusFilter }
            }
            return result
        }

    val groupedEvents: Map<String, List<Event>>
        get() = filteredEvents.groupBy { it.status.name }
}

class EventListViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = EventRepository(application)
    private val settingsManager = SettingsManager(application)

    private val _uiState = MutableStateFlow(EventListUiState())
    val uiState: StateFlow<EventListUiState> = _uiState.asStateFlow()

    init {
        loadEvents()
        syncFromCloud()
        syncPremiumFromCloud()
        checkYearInReviewDismiss()
    }

    private fun checkYearInReviewDismiss() {
        viewModelScope.launch {
            val dismissedYear = settingsManager.getYearInReviewDismissedYear().first()
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            if (dismissedYear >= currentYear) {
                _uiState.value = _uiState.value.copy(yearInReviewDismissed = true)
            }
        }
    }

    private fun loadEvents() {
        viewModelScope.launch {
            repository.getAllEvents().collect { events ->
                _uiState.value = _uiState.value.copy(
                    events = events,
                    isLoading = false
                )
                loadParticipantCounts(events.map { it.id })
            }
        }
    }

    private fun loadParticipantCounts(eventIds: List<String>) {
        eventIds.forEach { eventId ->
            viewModelScope.launch {
                try {
                    repository.getParticipantCount(eventId).collect { count ->
                        _uiState.value = _uiState.value.copy(
                            participantCounts = _uiState.value.participantCounts + (eventId to count)
                        )
                    }
                } catch (e: Exception) {
                    Log.w("EventListViewModel", "Failed to load participant count for $eventId", e)
                }
            }
        }
    }

    private fun syncFromCloud() {
        viewModelScope.launch {
            try {
                repository.syncFromCloud()
            } catch (e: Exception) {
                Log.e("EventListViewModel", "Cloud sync failed", e)
            }
        }
    }

    private fun syncPremiumFromCloud() {
        // In debug builds, skip cloud sync to preserve debug tier set via setDebugTier()
        if (com.bettermingle.app.BuildConfig.DEBUG) return

        viewModelScope.launch {
            try {
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                val doc = FirebaseFirestore.getInstance()
                    .collection("users").document(uid).get().await()
                val isPremium = doc.getBoolean("isPremium") ?: false
                val premiumUntil = doc.getTimestamp("premiumUntil")?.toDate()?.time
                    ?: doc.getLong("premiumUntil")
                val tier = try {
                    doc.getString("premiumTier")?.let { com.bettermingle.app.data.preferences.PremiumTier.valueOf(it) }
                } catch (_: Exception) { null }
                settingsManager.updatePremiumStatus(isPremium, premiumUntil, tier)
                Log.d("EventListViewModel", "syncPremiumFromCloud: isPremium=$isPremium, tier=$tier")
            } catch (e: Exception) {
                Log.w("EventListViewModel", "Failed to sync premium status", e)
            }
        }
    }

    fun updateSearch(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun updateStatusFilter(status: EventStatus?) {
        _uiState.value = _uiState.value.copy(statusFilter = status)
    }

    fun dismissYearInReview() {
        _uiState.value = _uiState.value.copy(yearInReviewDismissed = true)
        viewModelScope.launch {
            settingsManager.dismissYearInReview(Calendar.getInstance().get(Calendar.YEAR))
        }
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        syncFromCloud()
    }

    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            try {
                repository.deleteEvent(eventId)
            } catch (e: Exception) {
                Log.e("EventListViewModel", "Failed to delete event $eventId", e)
            }
        }
    }
}
