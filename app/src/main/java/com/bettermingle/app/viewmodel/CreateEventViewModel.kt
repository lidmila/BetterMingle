package com.bettermingle.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bettermingle.app.data.model.EventModule
import com.bettermingle.app.data.repository.EventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CreateEventUiState(
    val isCreating: Boolean = false,
    val createdEventId: String? = null,
    val error: String? = null
)

class CreateEventViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = EventRepository(application)

    private val _uiState = MutableStateFlow(CreateEventUiState())
    val uiState: StateFlow<CreateEventUiState> = _uiState.asStateFlow()

    fun createEvent(
        name: String,
        description: String,
        theme: String = "",
        location: String,
        locationLat: Double? = null,
        locationLng: Double? = null,
        locationAddress: String = "",
        startDate: Long? = null,
        endDate: Long? = null,
        enabledModules: List<EventModule> = emptyList()
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreating = true, error = null)
            try {
                val eventId = repository.createEvent(
                    name = name,
                    description = description,
                    theme = theme,
                    locationName = location,
                    locationLat = locationLat,
                    locationLng = locationLng,
                    locationAddress = locationAddress,
                    startDate = startDate,
                    endDate = endDate,
                    enabledModules = enabledModules
                )
                _uiState.value = _uiState.value.copy(
                    isCreating = false,
                    createdEventId = eventId
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isCreating = false,
                    error = e.message ?: "Chyba při vytváření akce"
                )
            }
        }
    }
}
