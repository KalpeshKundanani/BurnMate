package org.kalpeshbkundanani.burnmate.caloriedebt.domain

import org.kalpeshbkundanani.burnmate.caloriedebt.model.CalorieDebtDay
import org.kalpeshbkundanani.burnmate.caloriedebt.model.CalorieDebtSeverity
import org.kalpeshbkundanani.burnmate.caloriedebt.model.CalorieDebtTrend

class DefaultDebtTrendClassifier : DebtTrendClassifier {
    override fun classifyLatestTrend(days: List<CalorieDebtDay>): CalorieDebtTrend {
        if (days.isEmpty()) return CalorieDebtTrend.UNCHANGED

        val last = days.last()
        if (last.startingDebtCalories > 0 && last.endingDebtCalories == 0) {
            return CalorieDebtTrend.CLEARED
        }
        return when {
            last.endingDebtCalories > last.startingDebtCalories -> CalorieDebtTrend.INCREASED
            last.endingDebtCalories < last.startingDebtCalories -> CalorieDebtTrend.REDUCED
            else -> CalorieDebtTrend.UNCHANGED
        }
    }

    override fun classifySeverity(finalDebtCalories: Int): CalorieDebtSeverity = when {
        finalDebtCalories == 0 -> CalorieDebtSeverity.NONE
        finalDebtCalories in 1..299 -> CalorieDebtSeverity.LOW
        finalDebtCalories in 300..699 -> CalorieDebtSeverity.MEDIUM
        else -> CalorieDebtSeverity.HIGH
    }

    override fun calculateDebtStreak(days: List<CalorieDebtDay>): Int {
        var streak = 0
        for (day in days.asReversed()) {
            if (day.endingDebtCalories > 0 && day.dailyDeltaCalories >= 0) {
                streak += 1
            } else {
                break
            }
        }
        return streak
    }
}
