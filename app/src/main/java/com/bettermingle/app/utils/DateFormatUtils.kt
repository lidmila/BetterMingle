package com.bettermingle.app.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object DateFormatUtils {
    private val czechLocale = Locale.forLanguageTag("cs")

    private val dateFormat = SimpleDateFormat("d. M. yyyy", czechLocale)
    private val dateTimeFormat = SimpleDateFormat("d. M. yyyy HH:mm", czechLocale)
    private val timeFormat = SimpleDateFormat("HH:mm", czechLocale)
    private val dayMonthFormat = SimpleDateFormat("d. MMMM", czechLocale)
    private val fullFormat = SimpleDateFormat("EEEE d. MMMM yyyy", czechLocale)

    fun formatDate(timestamp: Long): String = dateFormat.format(Date(timestamp))

    fun formatDateTime(timestamp: Long): String = dateTimeFormat.format(Date(timestamp))

    fun formatTime(timestamp: Long): String = timeFormat.format(Date(timestamp))

    fun formatDayMonth(timestamp: Long): String = dayMonthFormat.format(Date(timestamp))

    fun formatFull(timestamp: Long): String = fullFormat.format(Date(timestamp))

    fun formatDateRange(start: Long, end: Long): String {
        val startCal = Calendar.getInstance().apply { timeInMillis = start }
        val endCal = Calendar.getInstance().apply { timeInMillis = end }

        return if (startCal.get(Calendar.YEAR) == endCal.get(Calendar.YEAR) &&
            startCal.get(Calendar.MONTH) == endCal.get(Calendar.MONTH)
        ) {
            "${startCal.get(Calendar.DAY_OF_MONTH)}.–${formatDate(end)}"
        } else {
            "${formatDate(start)} – ${formatDate(end)}"
        }
    }

    fun formatRelative(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = timestamp - now

        if (diff < 0) return "Proběhlo"

        val days = TimeUnit.MILLISECONDS.toDays(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff) % 24

        return when {
            days > 30 -> "za ${days / 30} měs."
            days > 0 -> "za $days dní"
            hours > 0 -> "za $hours hod."
            else -> "brzy"
        }
    }
}
