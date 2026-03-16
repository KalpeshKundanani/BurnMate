package org.kalpeshbkundanani.burnmate.caloriedebt.domain

import org.kalpeshbkundanani.burnmate.caloriedebt.model.CalorieDebtDay
import org.kalpeshbkundanani.burnmate.caloriedebt.model.CalorieDebtSeverity
import org.kalpeshbkundanani.burnmate.caloriedebt.model.CalorieDebtTrend

interface DebtTrendClassifier {
    fun classifyLatestTrend(days: List<CalorieDebtDay>): CalorieDebtTrend
    fun classifySeverity(finalDebtCalories: Int): CalorieDebtSeverity
    fun calculateDebtStreak(days: List<CalorieDebtDay>): Int
}
