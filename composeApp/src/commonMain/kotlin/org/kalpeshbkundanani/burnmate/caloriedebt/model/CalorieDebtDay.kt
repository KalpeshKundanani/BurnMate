package org.kalpeshbkundanani.burnmate.caloriedebt.model

import kotlinx.datetime.LocalDate

data class CalorieDebtDay(
    val date: LocalDate,
    val consumedCalories: Int,
    val targetCalories: Int,
    val dailyDeltaCalories: Int,
    val startingDebtCalories: Int,
    val endingDebtCalories: Int
)
