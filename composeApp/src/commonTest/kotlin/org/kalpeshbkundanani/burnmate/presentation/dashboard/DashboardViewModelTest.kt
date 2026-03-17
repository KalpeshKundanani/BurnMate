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
import org.kalpeshbkundanani.burnmate.presentation.dashboard.charts.ChartRangeOption
import org.kalpeshbkundanani.burnmate.presentation.dashboard.charts.DashboardChartDataSource
import org.kalpeshbkundanani.burnmate.presentation.dashboard.charts.DashboardChartStateAdapter
import org.kalpeshbkundanani.burnmate.presentation.shared.LoadableUiState
import org.kalpeshbkundanani.burnmate.weight.model.WeightEntry
import kotlinx.datetime.LocalDate
import kotlin.test.assertNull
import kotlin.test.assertNotNull

class DashboardViewModelTest {

    @Test
    fun `t03 maps dashboard snapshot into content cards and visualization`() {
        val date = LocalDate(2026, 3, 16)
        val viewModel = DashboardViewModel(
            dashboardService = FakeDashboardReadModelService(
                snapshotsByDate = mapOf(date to dashboardSnapshot(date))
            ),
            chartDataSource = FakeDashboardChartDataSource(dashboardSnapshot(date)),
            chartAdapter = DashboardChartStateAdapter(),
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
        // Since fake data source returns empty lists and WeightSummary is present, only ring is non-null
        assertEquals(LoadableUiState.Content, state.visualization.status)
        assertEquals(ChartRangeOption.Last7Days, state.visualization.selectedRange)
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
        val viewModel = DashboardViewModel(
            dashboardService = service,
            chartDataSource = FakeDashboardChartDataSource(dashboardSnapshot(initialDate)),
            chartAdapter = DashboardChartStateAdapter(),
            initialDate = initialDate
        )

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
            chartDataSource = FakeDashboardChartDataSource(dashboardSnapshot(date)),
            chartAdapter = DashboardChartStateAdapter(),
            initialDate = date
        )
        val second = DashboardViewModel(
            dashboardService = FakeDashboardReadModelService(
                snapshotsByDate = mapOf(date to dashboardSnapshot(date))
            ),
            chartDataSource = FakeDashboardChartDataSource(dashboardSnapshot(date)),
            chartAdapter = DashboardChartStateAdapter(),
            initialDate = date
        )

        assertEquals(first.uiState.value, second.uiState.value)
    }

    @Test
    fun `t07 chart range change reloads visualization without changing selected date`() {
        val date = LocalDate(2026, 3, 16)
        val chartDataSource = FakeDashboardChartDataSource(dashboardSnapshot(date))
        val viewModel = DashboardViewModel(
            dashboardService = FakeDashboardReadModelService(
                snapshotsByDate = mapOf(date to dashboardSnapshot(date))
            ),
            chartDataSource = chartDataSource,
            chartAdapter = DashboardChartStateAdapter(),
            initialDate = date
        )

        viewModel.onEvent(DashboardEvent.ChartRangeSelected(ChartRangeOption.Last30Days))

        val state = viewModel.uiState.value
        assertEquals(date, state.selectedDate)
        assertEquals(ChartRangeOption.Last30Days, state.visualization.selectedRange)
        assertEquals(ChartRangeOption.Last30Days, chartDataSource.lastRequestedRange)
    }

    @Test
    fun `loading empty and error visualization states clear stale charts`() {
        val date = LocalDate(2026, 3, 17)
        val previousDate = LocalDate(2026, 3, 16)
        val chartDataSource = FakeDashboardChartDataSource(dashboardSnapshot(date))
        val viewModel = DashboardViewModel(
            dashboardService = FakeDashboardReadModelService(
                snapshotsByDate = mapOf(
                    date to dashboardSnapshot(date),
                    previousDate to emptyDashboardSnapshot(previousDate)
                )
            ),
            chartDataSource = chartDataSource,
            chartAdapter = DashboardChartStateAdapter(),
            initialDate = date
        )

        assertEquals(LoadableUiState.Content, viewModel.uiState.value.visualization.status)
        assertNotNull(viewModel.uiState.value.visualization.charts?.progressRing)

        chartDataSource.onLoadDebtChartSnapshot = {
            val stateWhileLoading = viewModel.uiState.value.visualization
            assertEquals(LoadableUiState.Loading, stateWhileLoading.status)
            assertNull(stateWhileLoading.charts)
        }
        chartDataSource.snapshot = emptyDashboardSnapshot(previousDate)
        chartDataSource.weightEntriesResult = Result.success(emptyList())
        viewModel.onEvent(DashboardEvent.PreviousDayTapped)

        val emptyState = viewModel.uiState.value.visualization
        assertEquals(LoadableUiState.Empty, emptyState.status)
        assertNull(emptyState.charts)
        assertEquals("Not enough data to display visualizations.", emptyState.emptyMessage?.message)

        chartDataSource.debtSnapshotResult = Result.failure(IllegalStateException("chart failure"))
        viewModel.onEvent(DashboardEvent.ChartRangeSelected(ChartRangeOption.Last30Days))

        val errorState = viewModel.uiState.value.visualization
        assertEquals(LoadableUiState.Error, errorState.status)
        assertNull(errorState.charts)
        assertEquals("chart failure", errorState.errorMessage?.message)
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

private class FakeDashboardChartDataSource(
    var snapshot: DashboardSnapshot
) : DashboardChartDataSource {
    var lastRequestedRange: ChartRangeOption? = null
    var debtSnapshotResult: Result<DashboardSnapshot>? = null
    var weightEntriesResult: Result<List<WeightEntry>> = Result.success(emptyList())
    var onLoadDebtChartSnapshot: (() -> Unit)? = null

    override fun loadDebtChartSnapshot(
        selectedDate: LocalDate,
        range: ChartRangeOption
    ): Result<DashboardSnapshot> {
        lastRequestedRange = range
        onLoadDebtChartSnapshot?.invoke()
        return debtSnapshotResult ?: Result.success(snapshot)
    }

    override fun loadWeightEntries(
        selectedDate: LocalDate,
        range: ChartRangeOption
    ): Result<List<WeightEntry>> {
        return weightEntriesResult
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

private fun emptyDashboardSnapshot(date: LocalDate): DashboardSnapshot {
    return DashboardSnapshot(
        snapshotDate = date,
        todaySummary = TodaySummary(
            totalIntakeCalories = 800,
            totalBurnCalories = 200,
            netCalories = 600,
            remainingCalories = 1200,
            dailyTargetCalories = 2000
        ),
        debtSummary = null,
        weightSummary = null,
        debtChartPoints = emptyList()
    )
}
