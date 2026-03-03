package com.bettermingle.app.utils

import java.text.NumberFormat
import java.util.Locale

object CurrencyUtils {
    private val czechLocale = Locale.forLanguageTag("cs-CZ")
    private val currencyFormat = NumberFormat.getCurrencyInstance(czechLocale)

    fun formatCzk(amount: Double): String {
        return currencyFormat.format(amount)
    }

    fun formatCzkShort(amount: Double): String {
        return when {
            amount >= 1_000_000 -> String.format(czechLocale, "%.1f mil. Kč", amount / 1_000_000)
            amount >= 1_000 -> String.format(czechLocale, "%.0f tis. Kč", amount / 1_000)
            else -> String.format(czechLocale, "%.0f Kč", amount)
        }
    }

    fun formatAmount(amount: Double, currency: String = "CZK"): String {
        return when (currency) {
            "CZK" -> formatCzk(amount)
            "EUR" -> String.format(Locale.GERMANY, "%.2f €", amount)
            "USD" -> String.format(Locale.US, "$%.2f", amount)
            else -> String.format("%.2f %s", amount, currency)
        }
    }
}
