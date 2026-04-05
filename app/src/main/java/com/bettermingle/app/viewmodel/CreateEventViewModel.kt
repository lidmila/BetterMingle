package com.bettermingle.app.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bettermingle.app.R
import com.bettermingle.app.data.model.EventModule
import com.bettermingle.app.data.preferences.PremiumTier
import com.bettermingle.app.data.preferences.SettingsManager
import com.bettermingle.app.data.repository.EventRepository
import com.bettermingle.app.utils.ActivityLogger
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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

    data class TemplateBudgetData(val name: String, val estimatedAmount: Int = 0)
    data class TemplateTaskData(val title: String)
    data class TemplateScheduleData(val title: String, val timeLabel: String)

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
        invitedEmails: List<String> = emptyList(),
        templateBudgetItems: List<TemplateBudgetData> = emptyList(),
        templateTasks: List<TemplateTaskData> = emptyList(),
        templateScheduleBlocks: List<TemplateScheduleData> = emptyList()
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
                ActivityLogger.log(eventId, "settings", getApplication<Application>().getString(R.string.activity_created_event, name), eventName = name)

                // Write template items to Firestore subcollections
                writeTemplateItems(eventId, templateBudgetItems, templateTasks, templateScheduleBlocks)

                _uiState.value = _uiState.value.copy(
                    isCreating = false,
                    createdEventId = eventId
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isCreating = false,
                    error = e.message ?: getApplication<Application>().getString(R.string.create_event_error)
                )
            }
        }
    }

    private suspend fun writeTemplateItems(
        eventId: String,
        budgetItems: List<TemplateBudgetData>,
        tasks: List<TemplateTaskData>,
        scheduleBlocks: List<TemplateScheduleData>
    ) {
        if (budgetItems.isEmpty() && tasks.isEmpty() && scheduleBlocks.isEmpty()) return

        val firestore = FirebaseFirestore.getInstance()
        val eventRef = firestore.collection("events").document(eventId)

        try {
            val batch = firestore.batch()

            budgetItems.forEach { item ->
                val doc = eventRef.collection("budgetCategories").document()
                batch.set(doc, mapOf(
                    "name" to item.name,
                    "planned" to item.estimatedAmount,
                    "fromTemplate" to true,
                    "createdAt" to System.currentTimeMillis()
                ))
            }

            tasks.forEach { task ->
                val doc = eventRef.collection("tasks").document()
                batch.set(doc, mapOf(
                    "name" to task.title,
                    "color" to "Modrá",
                    "assignedTo" to emptyList<String>(),
                    "isCompleted" to false,
                    "fromTemplate" to true,
                    "createdAt" to System.currentTimeMillis()
                ))
            }

            scheduleBlocks.forEach { block ->
                val doc = eventRef.collection("schedule").document()
                batch.set(doc, mapOf(
                    "title" to block.title,
                    "startTime" to null,
                    "endTime" to null,
                    "location" to "",
                    "description" to block.timeLabel,
                    "fromTemplate" to true,
                    "createdAt" to System.currentTimeMillis()
                ))
            }

            batch.commit().await()
        } catch (e: Exception) {
            Log.e("CreateEventViewModel", "Failed to write template items for $eventId", e)
        }
    }
}
