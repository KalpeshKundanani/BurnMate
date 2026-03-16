package org.kalpeshbkundanani.burnmate.dashboard.domain

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import org.kalpeshbkundanani.burnmate.caloriedebt.domain.CalorieDebtCalculator
import org.kalpeshbkundanani.burnmate.caloriedebt.model.CalculationWindow
import org.kalpeshbkundanani.burnmate.caloriedebt.model.CalorieDebtDay
import org.kalpeshbkundanani.burnmate.caloriedebt.model.DailyCalorieEntry
import org.kalpeshbkundanani.burnmate.dashboard.model.DashboardSnapshot
import org.kalpeshbkundanani.burnmate.dashboard.model.DebtChartPoint
import org.kalpeshbkundanani.burnmate.dashboard.model.DebtSummary
import org.kalpeshbkundanani.burnmate.dashboard.model.TodaySummary
import org.kalpeshbkundanani.burnmate.dashboard.model.WeightSummary
import org.kalpeshbkundanani.burnmate.logging.model.CalorieEntry
import org.kalpeshbkundanani.burnmate.logging.model.EntryDate
import org.kalpeshbkundanani.burnmate.logging.repository.EntryRepository
import org.kalpeshbkundanani.burnmate.profile.model.BodyMetrics
import org.kalpeshbkundanani.burnmate.weight.domain.WeightHistoryService

class DefaultDashboardReadModelService(
    private val entryRepository: EntryRepository,
    private val debtCalculator: CalorieDebtCalculator,
    private val weightHistoryService: WeightHistoryService,
    private val bodyMetrics: BodyMetrics,
    private val dailyTargetCalories: Int,
    private val chartWindowDays: Int = DEFAULT_CHART_WINDOW_DAYS
) : DashboardReadModelService {

    override fun getDashboardSnapshot(today: LocalDate): Result<DashboardSnapshot> {
        if (chartWindowDays < 1) {
            return Result.failure(
                IllegalArgumentException("INVALID_CHART_WINDOW: chartWindowDays must be >= 1")
            )
        }

        val todayEntries = collectTodayLogs(today).getOrElse { error ->
            return Result.failure(error)
        }
        val todaySummary = calculateTodayTotals(todayEntries)
        val (debtSummary, debtChartPoints) = readCurrentDebt(today).getOrElse { error ->
            return Result.failure(error)
        }
        val weightSummary = readWeightProgress()

        return Result.success(
            DashboardSnapshot(
                snapshotDate = today,
                todaySummary = todaySummary,
                debtSummary = debtSummary,
                weightSummary = weightSummary,
                debtChartPoints = debtChartPoints
            )
        )
    }

    private fun collectTodayLogs(today: LocalDate): Result<List<CalorieEntry>> =
        entryRepository.fetchByDate(EntryDate(today))

    private fun calculateTodayTotals(entries: List<CalorieEntry>): TodaySummary {
        val totalIntakeCalories = entries
            .asSequence()
            .map { it.amount.value }
            .filter { it > 0 }
            .sum()
        val totalBurnCalories = entries
            .asSequence()
            .map { it.amount.value }
            .filter { it < 0 }
            .sumOf { -it }
        val netCalories = totalIntakeCalories - totalBurnCalories
        val remainingCalories = dailyTargetCalories - totalIntakeCalories

        return TodaySummary(
            totalIntakeCalories = totalIntakeCalories,
            totalBurnCalories = totalBurnCalories,
            netCalories = netCalories,
            remainingCalories = remainingCalories,
            dailyTargetCalories = dailyTargetCalories
        )
    }

    private fun readCurrentDebt(today: LocalDate): Result<Pair<DebtSummary?, List<DebtChartPoint>>> {
        val startDate = today.plus(DatePeriod(days = -(chartWindowDays - 1)))
        val entriesInWindow = entryRepository.fetchByDateRange(
            startDate = EntryDate(startDate),
            endDate = EntryDate(today)
        ).getOrElse { error ->
            return Result.failure(error)
        }

        val dailyEntries = entriesInWindow
            .groupBy { it.date.value }
            .toSortedMap()
            .map { (date, entries) ->
                DailyCalorieEntry(
                    date = date,
                    consumedCalories = entries.sumOf { it.amount.value }
                )
            }
        val debtResult = debtCalculator.calculate(
            window = CalculationWindow(
                startDate = startDate,
                endDate = today,
                targetCalories = dailyTargetCalories
            ),
            entries = dailyEntries
        ).getOrElse {
            return Result.success(null to emptyList())
        }

        val debtSummary = DebtSummary(
            currentDebtCalories = debtResult.finalDebtCalories,
            severity = debtResult.severity,
            trend = debtResult.latestTrend
        )

        return Result.success(debtSummary to prepareDebtChart(debtResult.days))
    }

    private fun readWeightProgress(): WeightSummary? {
        val history = weightHistoryService.getWeightHistory().getOrNull().orEmpty()
        if (history.isEmpty()) {
            return null
        }

        val currentWeightKg = history
            .sortedWith(compareBy({ it.date }, { it.createdAt }))
            .last()
            .weight
            .kg
        val goalWeightKg = bodyMetrics.goalWeightKg
        val remainingKg = maxOf(0.0, currentWeightKg - goalWeightKg)
        val totalToLose = bodyMetrics.currentWeightKg - goalWeightKg
        val progressPercentage = when {
            currentWeightKg <= goalWeightKg -> 100.0
            totalToLose <= 0.0 -> 100.0
            else -> (((totalToLose - remainingKg) / totalToLose) * 100.0).coerceIn(0.0, 100.0)
        }

        return WeightSummary(
            currentWeightKg = currentWeightKg,
            goalWeightKg = goalWeightKg,
            remainingKg = remainingKg,
            progressPercentage = progressPercentage
        )
    }

    private fun prepareDebtChart(days: List<CalorieDebtDay>): List<DebtChartPoint> = days
        .sortedBy { it.date }
        .map { day ->
            DebtChartPoint(
                date = day.date,
                cumulativeDebtCalories = day.endingDebtCalories
            )
        }

    companion object {
        const val DEFAULT_CHART_WINDOW_DAYS: Int = 7
    }
}
