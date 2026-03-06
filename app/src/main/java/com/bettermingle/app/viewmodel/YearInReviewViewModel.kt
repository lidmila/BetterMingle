package com.bettermingle.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bettermingle.app.data.repository.EventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

data class YearInReviewStats(
    val year: Int = Calendar.getInstance().get(Calendar.YEAR),
    val totalEvents: Int = 0,
    val totalUniqueParticipants: Int = 0,
    val totalEventDays: Int = 0,
    val mostVisitedLocation: String = "",
    val mostActiveMonth: String = "",
    val longestEvent: String = "",
    val longestEventDays: Int = 0,
    val avgParticipantsPerEvent: Int = 0,
    val isLoading: Boolean = true
)

class YearInReviewViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = EventRepository(application)

    private val _stats = MutableStateFlow(YearInReviewStats())
    val stats: StateFlow<YearInReviewStats> = _stats.asStateFlow()

    private val czechMonths = listOf(
        "Leden", "Únor", "Březen", "Duben", "Květen", "Červen",
        "Červenec", "Srpen", "Září", "Říjen", "Listopad", "Prosinec"
    )

    init {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            try {
                val allEvents = repository.getAllEvents().first()
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                val cal = Calendar.getInstance()

                val yearEvents = allEvents.filter { event ->
                    event.startDate?.let { date ->
                        cal.timeInMillis = date
                        cal.get(Calendar.YEAR) == currentYear
                    } ?: false
                }

                val totalEvents = yearEvents.size

                // Collect unique participants across all events
                val allParticipantIds = mutableSetOf<String>()
                yearEvents.forEach { event ->
                    try {
                        val participants = repository.getParticipantsByEvent(event.id).first()
                        participants.forEach { allParticipantIds.add(it.userId) }
                    } catch (_: Exception) { }
                }

                // Total event days
                val totalDays = yearEvents.sumOf { event ->
                    val start = event.startDate ?: return@sumOf 0
                    val end = event.endDate ?: start
                    val diffMs = end - start
                    val days = (diffMs / (1000 * 60 * 60 * 24)).toInt()
                    maxOf(days, 1)
                }

                // Most visited location
                val mostLocation = yearEvents
                    .filter { it.locationName.isNotEmpty() }
                    .groupBy { it.locationName }
                    .maxByOrNull { it.value.size }
                    ?.key ?: ""

                // Most active month
                val mostMonth = yearEvents
                    .mapNotNull { it.startDate }
                    .map { date ->
                        cal.timeInMillis = date
                        cal.get(Calendar.MONTH)
                    }
                    .groupBy { it }
                    .maxByOrNull { it.value.size }
                    ?.key?.let { czechMonths.getOrNull(it) } ?: ""

                // Longest event
                val longestEntry = yearEvents
                    .filter { it.startDate != null && it.endDate != null }
                    .maxByOrNull { (it.endDate ?: 0) - (it.startDate ?: 0) }
                val longest = longestEntry?.name ?: ""
                val longestDays = longestEntry?.let {
                    val diff = (it.endDate ?: 0) - (it.startDate ?: 0)
                    maxOf((diff / (1000 * 60 * 60 * 24)).toInt(), 1)
                } ?: 0

                // Average participants per event
                val avgParticipants = if (totalEvents > 0) {
                    allParticipantIds.size / totalEvents
                } else 0

                _stats.value = YearInReviewStats(
                    year = currentYear,
                    totalEvents = totalEvents,
                    totalUniqueParticipants = allParticipantIds.size,
                    totalEventDays = totalDays,
                    mostVisitedLocation = mostLocation,
                    mostActiveMonth = mostMonth,
                    longestEvent = longest,
                    longestEventDays = longestDays,
                    avgParticipantsPerEvent = avgParticipants,
                    isLoading = false
                )
            } catch (_: Exception) {
                _stats.value = _stats.value.copy(isLoading = false)
            }
        }
    }
}
