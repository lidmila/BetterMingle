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
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class EventListUiState(
    val events: List<Event> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val searchQuery: String = "",
    val statusFilter: EventStatus? = null
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
    }

    private fun loadEvents() {
        viewModelScope.launch {
            repository.getAllEvents().collect { events ->
                _uiState.value = _uiState.value.copy(
                    events = events,
                    isLoading = false
                )
            }
        }
    }

    private fun syncFromCloud() {
        viewModelScope.launch {
            try {
                repository.syncFromCloud()
            } catch (_: Exception) { }
        }
    }

    private fun syncPremiumFromCloud() {
        viewModelScope.launch {
            try {
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                val doc = FirebaseFirestore.getInstance()
                    .collection("users").document(uid).get().await()
                val isPremium = doc.getBoolean("isPremium") ?: false
                val premiumUntil = doc.getTimestamp("premiumUntil")?.toDate()?.time
                    ?: doc.getLong("premiumUntil")
                settingsManager.updatePremiumStatus(isPremium, premiumUntil)
                Log.d("EventListViewModel", "syncPremiumFromCloud: isPremium=$isPremium")
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

    fun refresh() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        syncFromCloud()
    }

    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            try {
                repository.deleteEvent(eventId)
            } catch (_: Exception) { }
        }
    }
}
