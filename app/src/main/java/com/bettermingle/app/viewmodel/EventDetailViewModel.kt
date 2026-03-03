package com.bettermingle.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bettermingle.app.data.model.Event
import com.bettermingle.app.data.model.Participant
import com.bettermingle.app.data.repository.EventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EventDetailUiState(
    val event: Event? = null,
    val participants: List<Participant> = emptyList(),
    val participantCount: Int = 0,
    val isLoading: Boolean = true
)

class EventDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = EventRepository(application)

    private val _uiState = MutableStateFlow(EventDetailUiState())
    val uiState: StateFlow<EventDetailUiState> = _uiState.asStateFlow()

    fun loadEvent(eventId: String) {
        viewModelScope.launch {
            repository.getEventById(eventId).collect { event ->
                _uiState.value = _uiState.value.copy(
                    event = event,
                    isLoading = false
                )
            }
        }
        viewModelScope.launch {
            repository.getParticipantsByEvent(eventId).collect { participants ->
                _uiState.value = _uiState.value.copy(
                    participants = participants,
                    participantCount = participants.size
                )
            }
        }
    }
}
