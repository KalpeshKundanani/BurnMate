package org.kalpeshbkundanani.burnmate.dashboard.model

import org.kalpeshbkundanani.burnmate.caloriedebt.model.CalorieDebtSeverity
import org.kalpeshbkundanani.burnmate.caloriedebt.model.CalorieDebtTrend

data class DebtSummary(
    val currentDebtCalories: Int,
    val severity: CalorieDebtSeverity,
    val trend: CalorieDebtTrend
)
