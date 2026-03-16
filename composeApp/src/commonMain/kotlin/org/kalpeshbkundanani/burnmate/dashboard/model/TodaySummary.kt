package org.kalpeshbkundanani.burnmate.dashboard.model

data class TodaySummary(
    val totalIntakeCalories: Int,
    val totalBurnCalories: Int,
    val netCalories: Int,
    val remainingCalories: Int,
    val dailyTargetCalories: Int
)
