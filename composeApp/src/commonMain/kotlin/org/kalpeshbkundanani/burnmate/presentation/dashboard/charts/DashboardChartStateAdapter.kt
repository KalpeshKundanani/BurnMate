package org.kalpeshbkundanani.burnmate.presentation.dashboard.charts

import org.kalpeshbkundanani.burnmate.dashboard.model.DebtChartPoint
import org.kalpeshbkundanani.burnmate.dashboard.model.WeightSummary
import org.kalpeshbkundanani.burnmate.weight.model.WeightEntry
import kotlin.math.abs
import kotlin.math.roundToInt

class DashboardChartStateAdapter {
    fun map(
        range: ChartRangeOption,
        debtPoints: List<DebtChartPoint>,
        weightSummary: WeightSummary?,
        weightEntries: List<WeightEntry>
    ): DashboardChartState {
        val debtTrend = mapDebtTrend(range, debtPoints)
        val weeklyDeficit = mapWeeklyDeficit(debtPoints)
        val weightTrend = mapWeightTrend(range, weightEntries)
        val progressRing = mapProgressRing(weightSummary)

        return DashboardChartState(
            debtTrend = debtTrend,
            weightTrend = weightTrend,
            weeklyDeficit = weeklyDeficit,
            progressRing = progressRing
        )
    }

    private fun mapDebtTrend(range: ChartRangeOption, debtPoints: List<DebtChartPoint>): DebtTrendChartState? {
        val sortedPoints = debtPoints.sortedBy { it.date }
        val trimmedPoints = sortedPoints.takeLast(range.days)
        if (trimmedPoints.isEmpty()) return null

        val mappedPoints = trimmedPoints.map {
            DebtTrendPoint(
                date = it.date,
                cumulativeDebtCalories = it.cumulativeDebtCalories,
                label = "${it.date.month.name.take(3)} ${it.date.dayOfMonth}"
            )
        }

        val minDebt = mappedPoints.minOf { it.cumulativeDebtCalories }
        val maxDebt = mappedPoints.maxOf { it.cumulativeDebtCalories }
        val latestValue = mappedPoints.last().cumulativeDebtCalories
        
        val latestValueLabel = if (latestValue > 0) "+$latestValue kcal" else "$latestValue kcal"

        return DebtTrendChartState(
            points = mappedPoints,
            minDebtCalories = minDebt,
            maxDebtCalories = maxDebt,
            latestValueLabel = latestValueLabel
        )
    }

    private fun mapWeeklyDeficit(debtPoints: List<DebtChartPoint>): WeeklyDeficitChartState? {
        val sortedPoints = debtPoints.sortedBy { it.date }.takeLast(8)
        if (sortedPoints.size < 2) return null

        val bars = mutableListOf<WeeklyDeficitBar>()
        for (i in 1 until sortedPoints.size) {
            val previous = sortedPoints[i - 1]
            val current = sortedPoints[i]
            val delta = current.cumulativeDebtCalories - previous.cumulativeDebtCalories
            
            val direction = when {
                delta < 0 -> DailyBalanceDirection.Deficit
                delta > 0 -> DailyBalanceDirection.Surplus
                else -> DailyBalanceDirection.Neutral
            }
            
            bars.add(
                WeeklyDeficitBar(
                    date = current.date,
                    deltaCalories = delta,
                    direction = direction,
                    label = "${current.date.month.name.take(3)} ${current.date.dayOfMonth}"
                )
            )
        }

        val maxMagnitude = bars.maxOfOrNull { abs(it.deltaCalories) } ?: 0

        return WeeklyDeficitChartState(
            bars = bars.takeLast(7),
            maxMagnitudeCalories = maxMagnitude
        )
    }

    private fun mapWeightTrend(range: ChartRangeOption, weightEntries: List<WeightEntry>): WeightTrendChartState? {
        if (weightEntries.isEmpty()) return null
        
        val deduplicated = weightEntries
            .sortedWith(compareBy({ it.date }, { it.createdAt }))
            .groupBy { it.date }
            .map { it.value.last() }
            .sortedBy { it.date }
            .takeLast(range.days)

        if (deduplicated.isEmpty()) return null

        val mappedPoints = deduplicated.map {
            WeightTrendPoint(
                date = it.date,
                weightKg = it.weight.kg,
                label = "${it.date.month.name.take(3)} ${it.date.dayOfMonth}"
            )
        }

        val minWeight = mappedPoints.minOf { it.weightKg }
        val maxWeight = mappedPoints.maxOf { it.weightKg }
        val latestValue = mappedPoints.last().weightKg
        val latestValueLabel = "${((latestValue * 10.0).roundToInt() / 10.0)} kg"

        return WeightTrendChartState(
            points = mappedPoints,
            minWeightKg = minWeight,
            maxWeightKg = maxWeight,
            latestValueLabel = latestValueLabel
        )
    }

    private fun mapProgressRing(weightSummary: WeightSummary?): GoalProgressRingState? {
        if (weightSummary == null) return null

        val progressFraction = (weightSummary.progressPercentage / 100.0).coerceIn(0.0, 1.0).toFloat()
        val formattedProgress = "${weightSummary.progressPercentage.roundToInt()}%"
        val remainingKg = weightSummary.remainingKg
        val isGoalReached = remainingKg <= 0.0

        val formattedRemaining = ((remainingKg * 10.0).roundToInt() / 10.0)
        
        val supportingLabel = if (isGoalReached) {
            "Goal reached"
        } else {
            "$formattedRemaining kg to goal"
        }

        return GoalProgressRingState(
            progressFraction = progressFraction,
            progressLabel = formattedProgress,
            supportingLabel = supportingLabel,
            isGoalReached = isGoalReached
        )
    }
}
