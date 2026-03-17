package org.kalpeshbkundanani.burnmate.presentation.dashboard.charts

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import org.kalpeshbkundanani.burnmate.dashboard.domain.DashboardReadModelService
import org.kalpeshbkundanani.burnmate.dashboard.model.DashboardSnapshot
import org.kalpeshbkundanani.burnmate.weight.domain.WeightHistoryService
import org.kalpeshbkundanani.burnmate.weight.model.WeightEntry
import kotlin.math.max

class DefaultDashboardChartDataSource(
    private val dashboardServiceFactory: (Int) -> DashboardReadModelService,
    private val weightHistoryService: WeightHistoryService
) : DashboardChartDataSource {
    override fun loadDebtChartSnapshot(
        selectedDate: LocalDate,
        range: ChartRangeOption
    ): Result<DashboardSnapshot> {
        val requiredDays = max(range.days, 8)
        val service = dashboardServiceFactory(requiredDays)
        return service.getDashboardSnapshot(selectedDate)
    }

    override fun loadWeightEntries(
        selectedDate: LocalDate,
        range: ChartRangeOption
    ): Result<List<WeightEntry>> {
        val startDate = selectedDate.minus(range.days - 1, DateTimeUnit.DAY)
        return weightHistoryService.getWeightByDateRange(startDate, selectedDate)
    }
}
