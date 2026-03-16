package org.kalpeshbkundanani.burnmate.caloriedebt.model

import kotlinx.datetime.LocalDate

data class DailyCalorieEntry(
    val date: LocalDate,
    val consumedCalories: Int
)
