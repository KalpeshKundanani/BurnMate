package org.kalpeshbkundanani.burnmate.presentation.dashboard.charts

import kotlinx.datetime.LocalDate
import org.kalpeshbkundanani.burnmate.dashboard.model.DashboardSnapshot
import org.kalpeshbkundanani.burnmate.weight.model.WeightEntry

interface DashboardChartDataSource {
    fun loadDebtChartSnapshot(
        selectedDate: LocalDate,
        range: ChartRangeOption
    ): Result<DashboardSnapshot>

    fun loadWeightEntries(
        selectedDate: LocalDate,
        range: ChartRangeOption
    ): Result<List<WeightEntry>>
}
