package com.bettermingle.app.utils

import kotlin.math.abs
import kotlin.math.min

data class Debt(
    val fromUserId: String,
    val toUserId: String,
    val amount: Double
)

object DebtCalculator {

    fun calculateMinimumTransactions(
        expenses: Map<String, Double>,
        shares: Map<String, Double>
    ): List<Debt> {
        val allUsers = (expenses.keys + shares.keys).toSet()
        val balances = mutableMapOf<String, Double>()

        for (user in allUsers) {
            val paid = expenses[user] ?: 0.0
            val owed = shares[user] ?: 0.0
            balances[user] = paid - owed
        }

        val creditors = balances.entries
            .filter { it.value > 0.01 }
            .sortedByDescending { it.value }
            .map { it.key to it.value }
            .toMutableList()

        val debtors = balances.entries
            .filter { it.value < -0.01 }
            .sortedByDescending { abs(it.value) }
            .map { it.key to abs(it.value) }
            .toMutableList()

        val transactions = mutableListOf<Debt>()
        val creditorAmounts = creditors.map { it.second }.toDoubleArray()
        val debtorAmounts = debtors.map { it.second }.toDoubleArray()

        var i = 0
        var j = 0

        while (i < creditors.size && j < debtors.size) {
            val transferAmount = min(creditorAmounts[i], debtorAmounts[j])

            if (transferAmount > 0.01) {
                transactions.add(
                    Debt(
                        fromUserId = debtors[j].first,
                        toUserId = creditors[i].first,
                        amount = Math.round(transferAmount * 100.0) / 100.0
                    )
                )
            }

            creditorAmounts[i] -= transferAmount
            debtorAmounts[j] -= transferAmount

            if (creditorAmounts[i] < 0.01) i++
            if (debtorAmounts[j] < 0.01) j++
        }

        return transactions
    }

    fun equalSplit(totalAmount: Double, participantCount: Int): Double {
        if (participantCount <= 0) return 0.0
        return Math.round(totalAmount / participantCount * 100.0) / 100.0
    }
}
