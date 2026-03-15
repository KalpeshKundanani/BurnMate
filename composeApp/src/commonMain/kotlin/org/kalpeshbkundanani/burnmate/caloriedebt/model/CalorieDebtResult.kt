package org.kalpeshbkundanani.burnmate.caloriedebt.model

data class CalorieDebtResult(
    val finalDebtCalories: Int,
    val days: List<CalorieDebtDay>,
    val latestTrend: CalorieDebtTrend,
    val debtStreakDays: Int,
    val severity: CalorieDebtSeverity
)
