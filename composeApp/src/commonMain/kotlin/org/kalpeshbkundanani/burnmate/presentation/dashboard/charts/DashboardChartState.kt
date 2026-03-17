package org.kalpeshbkundanani.burnmate.presentation.dashboard.charts

import kotlinx.datetime.LocalDate

data class DashboardChartState(
    val debtTrend: DebtTrendChartState?,
    val weightTrend: WeightTrendChartState?,
    val weeklyDeficit: WeeklyDeficitChartState?,
    val progressRing: GoalProgressRingState?
)

data class DebtTrendChartState(
    val points: List<DebtTrendPoint>,
    val minDebtCalories: Int,
    val maxDebtCalories: Int,
    val latestValueLabel: String
)

data class DebtTrendPoint(
    val date: LocalDate,
    val cumulativeDebtCalories: Int,
    val label: String
)

data class WeightTrendChartState(
    val points: List<WeightTrendPoint>,
    val minWeightKg: Double,
    val maxWeightKg: Double,
    val latestValueLabel: String
)

data class WeightTrendPoint(
    val date: LocalDate,
    val weightKg: Double,
    val label: String
)

enum class DailyBalanceDirection {
    Deficit,
    Surplus,
    Neutral
}

data class WeeklyDeficitChartState(
    val bars: List<WeeklyDeficitBar>,
    val maxMagnitudeCalories: Int
)

data class WeeklyDeficitBar(
    val date: LocalDate,
    val deltaCalories: Int,
    val direction: DailyBalanceDirection,
    val label: String
)

data class GoalProgressRingState(
    val progressFraction: Float,
    val progressLabel: String,
    val supportingLabel: String,
    val isGoalReached: Boolean
)
