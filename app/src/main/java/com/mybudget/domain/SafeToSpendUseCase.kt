package com.mybudget.domain

import com.mybudget.data.local.entity.TransactionEntity
import java.util.Calendar

class SafeToSpendUseCase {
    
    /**
     * Subtracts expenses in the current period from the user-defined base safe spend amount.
     * The period is defined by the 'cycle' parameter: 'Daily', 'Weekly', or 'Monthly'.
     */
    fun calculateSafeToSpend(
        transactions: List<TransactionEntity>,
        baseSafeToSpend: Double,
        cycle: String
    ): Double {
        val calendar = Calendar.getInstance()
        var periodStart = 0L
        var periodEnd = 0L

        when (cycle) {
            "Daily" -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                periodStart = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                periodEnd = calendar.timeInMillis
            }
            "Weekly" -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                periodStart = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_WEEK, 7)
                periodEnd = calendar.timeInMillis
            }
            else -> { // Default to Monthly
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                periodStart = calendar.timeInMillis
                calendar.add(Calendar.MONTH, 1)
                periodEnd = calendar.timeInMillis
            }
        }

        var periodExpenses = 0.0
        for (tx in transactions) {
            if (!tx.isIncome && tx.timestamp in periodStart until periodEnd) {
                periodExpenses += tx.amount
            }
        }

        val safeRemaining = baseSafeToSpend - periodExpenses
        return if (safeRemaining > 0) safeRemaining else 0.0
    }
}
