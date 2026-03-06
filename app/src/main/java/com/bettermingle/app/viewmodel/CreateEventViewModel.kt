package com.bettermingle.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bettermingle.app.data.model.EventModule
import com.bettermingle.app.data.preferences.PremiumTier
import com.bettermingle.app.data.preferences.SettingsManager
import com.bettermingle.app.data.repository.EventRepository
import com.bettermingle.app.utils.ActivityLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class CreateEventUiState(
    val isCreating: Boolean = false,
    val createdEventId: String? = null,
    val error: String? = null
)

class CreateEventViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = EventRepository(application)
    private val settingsManager = SettingsManager(application)

    val premiumTier: Flow<PremiumTier> = settingsManager.settingsFlow.map { it.premiumTier }

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
        enabledModules: List<EventModule> = emptyList(),
        introText: String = "",
        securityEnabled: Boolean = false,
        eventPin: String = "",
        hideFinancials: Boolean = false,
        screenshotProtection: Boolean = false,
        autoDeleteDays: Int = 0,
        requireApproval: Boolean = false,
        invitedEmails: List<String> = emptyList()
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
                    enabledModules = enabledModules,
                    introText = introText,
                    securityEnabled = securityEnabled,
                    eventPin = eventPin,
                    hideFinancials = hideFinancials,
                    screenshotProtection = screenshotProtection,
                    autoDeleteDays = autoDeleteDays,
                    requireApproval = requireApproval,
                    invitedEmails = invitedEmails
                )
                ActivityLogger.log(eventId, "settings", "vytvořil/a novou akci: $name", eventName = name)
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
