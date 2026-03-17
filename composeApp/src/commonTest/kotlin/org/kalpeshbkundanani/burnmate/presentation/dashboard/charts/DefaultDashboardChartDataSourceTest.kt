package org.kalpeshbkundanani.burnmate.presentation.dashboard.charts

import kotlinx.datetime.LocalDate
import org.kalpeshbkundanani.burnmate.dashboard.domain.DashboardReadModelService
import org.kalpeshbkundanani.burnmate.dashboard.model.DashboardSnapshot
import org.kalpeshbkundanani.burnmate.dashboard.model.TodaySummary
import org.kalpeshbkundanani.burnmate.weight.domain.WeightHistoryService
import org.kalpeshbkundanani.burnmate.weight.model.WeightEntry
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultDashboardChartDataSourceTest {
    
    private val fakeDashboardService = object : DashboardReadModelService {
        var lastRequestedDate: LocalDate? = null
        override fun getDashboardSnapshot(date: LocalDate): Result<DashboardSnapshot> {
            lastRequestedDate = date
            return Result.success(DashboardSnapshot(date, TodaySummary(0, 0, 0, 0, 0), null, null, emptyList()))
        }
    }
    
    private val fakeWeightHistoryService = object : WeightHistoryService {
        var lastStartDate: LocalDate? = null
        var lastEndDate: LocalDate? = null
        override fun getWeightByDateRange(startDate: LocalDate, endDate: LocalDate): Result<List<WeightEntry>> {
            lastStartDate = startDate
            lastEndDate = endDate
            return Result.success(emptyList())
        }
        override fun recordWeight(date: LocalDate, weight: org.kalpeshbkundanani.burnmate.weight.model.WeightValue): Result<WeightEntry> = Result.success(WeightEntry(date, weight, kotlinx.datetime.Clock.System.now()))
        override fun editWeight(date: LocalDate, newWeight: org.kalpeshbkundanani.burnmate.weight.model.WeightValue): Result<WeightEntry> = Result.success(WeightEntry(date, newWeight, kotlinx.datetime.Clock.System.now()))
        override fun deleteWeight(date: LocalDate): Result<Boolean> = Result.success(true)
        override fun getWeightHistory(): Result<List<WeightEntry>> = Result.success(emptyList())
        override fun getWeightByDate(date: LocalDate): Result<WeightEntry?> = Result.success(null)
    }
    
    @Test
    fun `loadDebtChartSnapshot uses max 8 days for small ranges`() {
        var factoryRequestedDays = 0
        val factory = { days: Int -> 
            factoryRequestedDays = days
            fakeDashboardService
        }
        val source = DefaultDashboardChartDataSource(factory, fakeWeightHistoryService)
        
        source.loadDebtChartSnapshot(LocalDate(2026, 3, 17), ChartRangeOption.Last7Days)
        
        assertEquals(8, factoryRequestedDays)
        assertEquals(LocalDate(2026, 3, 17), fakeDashboardService.lastRequestedDate)
    }
    
    @Test
    fun `loadDebtChartSnapshot uses range days when greater than 8`() {
        var factoryRequestedDays = 0
        val factory = { days: Int -> 
            factoryRequestedDays = days
            fakeDashboardService
        }
        val source = DefaultDashboardChartDataSource(factory, fakeWeightHistoryService)
        
        source.loadDebtChartSnapshot(LocalDate(2026, 3, 17), ChartRangeOption.Last30Days)
        
        assertEquals(30, factoryRequestedDays)
    }
    
    @Test
    fun `loadWeightEntries calculates correct start date for range`() {
        val factory = { _: Int -> fakeDashboardService }
        val source = DefaultDashboardChartDataSource(factory, fakeWeightHistoryService)
        val selectedDate = LocalDate(2026, 3, 17)
        
        source.loadWeightEntries(selectedDate, ChartRangeOption.Last7Days)
        
        assertEquals(LocalDate(2026, 3, 11), fakeWeightHistoryService.lastStartDate)
        assertEquals(LocalDate(2026, 3, 17), fakeWeightHistoryService.lastEndDate)
    }
}
