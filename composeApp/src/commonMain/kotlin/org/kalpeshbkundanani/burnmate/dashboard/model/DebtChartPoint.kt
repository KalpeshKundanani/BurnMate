package org.kalpeshbkundanani.burnmate.dashboard.model

import kotlinx.datetime.LocalDate

data class DebtChartPoint(
    val date: LocalDate,
    val cumulativeDebtCalories: Int
)
