package org.kalpeshbkundanani.burnmate.caloriedebt.domain

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.plus
import org.kalpeshbkundanani.burnmate.caloriedebt.model.CalculationWindow
import org.kalpeshbkundanani.burnmate.caloriedebt.model.CalorieDebtDay
import org.kalpeshbkundanani.burnmate.caloriedebt.model.CalorieDebtError
import org.kalpeshbkundanani.burnmate.caloriedebt.model.CalorieDebtResult
import org.kalpeshbkundanani.burnmate.caloriedebt.model.DailyCalorieEntry

class DefaultCalorieDebtCalculator(
    private val validator: CalorieDebtValidator = DefaultCalorieDebtValidator(),
    private val trendClassifier: DebtTrendClassifier = DefaultDebtTrendClassifier()
) : CalorieDebtCalculator {

    override fun calculate(
        window: CalculationWindow,
        entries: List<DailyCalorieEntry>
    ): Result<CalorieDebtResult> {
        val validation = validator.validate(window, entries)
        if (validation.isFailure) {
            val error = validation.exceptionOrNull() as? CalorieDebtError
            return Result.failure(error ?: validation.exceptionOrNull()!!)
        }

        val inRangeEntries = entries.filter { it.date >= window.startDate && it.date <= window.endDate }
        val entryLookup = inRangeEntries.associate { it.date to it.consumedCalories }

        val days = mutableListOf<CalorieDebtDay>()
        var currentDate = window.startDate
        var previousEndingDebt = 0

        while (currentDate <= window.endDate) {
            val consumed = entryLookup[currentDate] ?: 0
            val delta = consumed - window.targetCalories
            val startingDebt = previousEndingDebt
            val endingDebt = maxOf(0, startingDebt + delta)

            days += CalorieDebtDay(
                date = currentDate,
                consumedCalories = consumed,
                targetCalories = window.targetCalories,
                dailyDeltaCalories = delta,
                startingDebtCalories = startingDebt,
                endingDebtCalories = endingDebt
            )

            previousEndingDebt = endingDebt
            currentDate = currentDate.plus(DatePeriod(days = 1))
        }

        val finalDebt = days.lastOrNull()?.endingDebtCalories ?: 0
        val trend = trendClassifier.classifyLatestTrend(days)
        val severity = trendClassifier.classifySeverity(finalDebt)
        val streak = trendClassifier.calculateDebtStreak(days)

        return Result.success(
            CalorieDebtResult(
                finalDebtCalories = finalDebt,
                days = days,
                latestTrend = trend,
                debtStreakDays = streak,
                severity = severity
            )
        )
    }
}
