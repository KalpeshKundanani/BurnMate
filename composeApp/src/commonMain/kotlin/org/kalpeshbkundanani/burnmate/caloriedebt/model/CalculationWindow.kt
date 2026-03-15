package org.kalpeshbkundanani.burnmate.caloriedebt.model

import kotlinx.datetime.LocalDate

data class CalculationWindow(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val targetCalories: Int
)
