package org.kalpeshbkundanani.burnmate.presentation.dashboard

import kotlin.test.Test
import kotlin.test.assertEquals
import org.kalpeshbkundanani.burnmate.caloriedebt.model.CalorieDebtSeverity
import org.kalpeshbkundanani.burnmate.caloriedebt.model.CalorieDebtTrend
import org.kalpeshbkundanani.burnmate.dashboard.domain.DashboardReadModelService
import org.kalpeshbkundanani.burnmate.dashboard.model.DashboardSnapshot
import org.kalpeshbkundanani.burnmate.dashboard.model.DebtSummary
import org.kalpeshbkundanani.burnmate.dashboard.model.TodaySummary
import org.kalpeshbkundanani.burnmate.dashboard.model.WeightSummary
import org.kalpeshbkundanani.burnmate.presentation.shared.LoadableUiState
import kotlinx.datetime.LocalDate

class DashboardViewModelTest {

    @Test
    fun `t03 maps dashboard snapshot into content cards`() {
        val date = LocalDate(2026, 3, 16)
        val viewModel = DashboardViewModel(
            dashboardService = FakeDashboardReadModelService(
                snapshotsByDate = mapOf(date to dashboardSnapshot(date))
            ),
            initialDate = date
        )

        val state = viewModel.uiState.value

        assertEquals(LoadableUiState.Content, state.status)
        assertEquals("+600 kcal", state.todaySummary?.formattedCurrentDeficit)
        assertEquals("1200 kcal left", state.todaySummary?.formattedProgressVsYesterday)
        assertEquals("+150 kcal", state.debtSummary?.formattedWeeklyNet)
        assertEquals("Trend: INCREASED", state.debtSummary?.formattedComparisonStat)
        assertEquals("80.0 kg", state.weightSummary?.formattedCurrentWeight)
        assertEquals("70.0 kg", state.weightSummary?.formattedGoalWeight)
        assertEquals("50.0%", state.weightSummary?.formattedProgress)
    }

    @Test
    fun `t08 date navigation reloads the selected date`() {
        val initialDate = LocalDate(2026, 3, 16)
        val previousDate = LocalDate(2026, 3, 15)
        val nextDate = LocalDate(2026, 3, 17)
        val service = FakeDashboardReadModelService(
            snapshotsByDate = mapOf(
                initialDate to dashboardSnapshot(initialDate),
                previousDate to dashboardSnapshot(previousDate),
                nextDate to dashboardSnapshot(nextDate)
            )
        )
        val viewModel = DashboardViewModel(dashboardService = service, initialDate = initialDate)

        viewModel.onEvent(DashboardEvent.PreviousDayTapped)
        assertEquals(previousDate, viewModel.uiState.value.selectedDate)

        viewModel.onEvent(DashboardEvent.NextDayTapped)
        assertEquals(initialDate, viewModel.uiState.value.selectedDate)

        assertEquals(listOf(initialDate, previousDate, initialDate), service.requestedDates)
    }

    @Test
    fun `t10 identical dependency outputs yield identical state`() {
        val date = LocalDate(2026, 3, 16)
        val first = DashboardViewModel(
            dashboardService = FakeDashboardReadModelService(
                snapshotsByDate = mapOf(date to dashboardSnapshot(date))
            ),
            initialDate = date
        )
        val second = DashboardViewModel(
            dashboardService = FakeDashboardReadModelService(
                snapshotsByDate = mapOf(date to dashboardSnapshot(date))
            ),
            initialDate = date
        )

        assertEquals(first.uiState.value, second.uiState.value)
    }
}

private class FakeDashboardReadModelService(
    private val snapshotsByDate: Map<LocalDate, DashboardSnapshot>
) : DashboardReadModelService {
    val requestedDates = mutableListOf<LocalDate>()

    override fun getDashboardSnapshot(today: LocalDate): Result<DashboardSnapshot> {
        requestedDates += today
        return Result.success(snapshotsByDate.getValue(today))
    }
}

private fun dashboardSnapshot(date: LocalDate): DashboardSnapshot {
    return DashboardSnapshot(
        snapshotDate = date,
        todaySummary = TodaySummary(
            totalIntakeCalories = 800,
            totalBurnCalories = 200,
            netCalories = 600,
            remainingCalories = 1200,
            dailyTargetCalories = 2000
        ),
        debtSummary = DebtSummary(
            currentDebtCalories = 150,
            severity = CalorieDebtSeverity.LOW,
            trend = CalorieDebtTrend.INCREASED
        ),
        weightSummary = WeightSummary(
            currentWeightKg = 80.0,
            goalWeightKg = 70.0,
            remainingKg = 10.0,
            progressPercentage = 50.0
        ),
        debtChartPoints = emptyList()
    )
}
