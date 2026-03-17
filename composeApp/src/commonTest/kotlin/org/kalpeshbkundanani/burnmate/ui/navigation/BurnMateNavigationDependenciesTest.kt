package org.kalpeshbkundanani.burnmate.ui.navigation

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import org.kalpeshbkundanani.burnmate.logging.model.CalorieEntry
import org.kalpeshbkundanani.burnmate.logging.model.EntryDate
import org.kalpeshbkundanani.burnmate.logging.model.EntryId
import org.kalpeshbkundanani.burnmate.logging.repository.EntryRepository
import org.kalpeshbkundanani.burnmate.profile.domain.DefaultUserProfileFactory
import org.kalpeshbkundanani.burnmate.profile.model.BodyMetrics
import org.kalpeshbkundanani.burnmate.settings.preferences.InMemoryAppPreferencesStore
import org.kalpeshbkundanani.burnmate.settings.state.InMemoryAppSessionStore
import org.kalpeshbkundanani.burnmate.weight.domain.WeightHistoryService
import org.kalpeshbkundanani.burnmate.weight.model.WeightEntry
import org.kalpeshbkundanani.burnmate.weight.model.WeightValue
import org.kalpeshbkundanani.burnmate.weight.repository.LocalWeightRepository
import kotlin.test.Test
import kotlin.test.assertEquals

class BurnMateNavigationDependenciesTest {
    @Test
    fun `createChartDataSource respects 7d 14d and 30d windows`() {
        val entryRepository = WindowTrackingEntryRepository()
        val dependencies = BurnMateNavigationDependencies(
            profileFactory = DefaultUserProfileFactory(),
            entryRepository = entryRepository,
            entryFactory = org.kalpeshbkundanani.burnmate.logging.domain.DefaultCalorieEntryFactory(
                org.kalpeshbkundanani.burnmate.logging.domain.DefaultCalorieEntryValidator()
            ),
            weightHistoryRepository = LocalWeightRepository(),
            weightHistoryService = FakeWeightHistoryService(),
            appPreferencesStore = InMemoryAppPreferencesStore(),
            appSessionStore = InMemoryAppSessionStore()
        )
        val profileSummary = DefaultUserProfileFactory().create(
            BodyMetrics(
                heightCm = 170.0,
                currentWeightKg = 82.0,
                goalWeightKg = 72.0
            )
        ).getOrThrow()
        val chartDataSource = dependencies.createChartDataSource(profileSummary)
        val selectedDate = LocalDate(2026, 3, 17)

        chartDataSource.loadDebtChartSnapshot(selectedDate, org.kalpeshbkundanani.burnmate.presentation.dashboard.charts.ChartRangeOption.Last7Days)
        assertEquals(LocalDate(2026, 3, 10), entryRepository.lastRangeStart)

        chartDataSource.loadDebtChartSnapshot(selectedDate, org.kalpeshbkundanani.burnmate.presentation.dashboard.charts.ChartRangeOption.Last14Days)
        assertEquals(LocalDate(2026, 3, 4), entryRepository.lastRangeStart)

        chartDataSource.loadDebtChartSnapshot(selectedDate, org.kalpeshbkundanani.burnmate.presentation.dashboard.charts.ChartRangeOption.Last30Days)
        assertEquals(LocalDate(2026, 2, 16), entryRepository.lastRangeStart)
        assertEquals(selectedDate, entryRepository.lastRangeEnd)
    }
}

private class WindowTrackingEntryRepository : EntryRepository {
    var lastRangeStart: LocalDate? = null
    var lastRangeEnd: LocalDate? = null

    override fun create(entry: CalorieEntry): Result<CalorieEntry> = Result.success(entry)

    override fun deleteById(id: EntryId): Result<Boolean> = Result.success(true)

    override fun fetchByDateRange(startDate: EntryDate, endDate: EntryDate): Result<List<CalorieEntry>> {
        lastRangeStart = startDate.value
        lastRangeEnd = endDate.value
        return Result.success(emptyList())
    }

    override fun fetchByDate(date: EntryDate): Result<List<CalorieEntry>> {
        return Result.success(emptyList())
    }
}

private class FakeWeightHistoryService : WeightHistoryService {
    override fun recordWeight(date: LocalDate, weight: WeightValue): Result<WeightEntry> {
        return Result.success(WeightEntry(date, weight, Clock.System.now()))
    }

    override fun editWeight(date: LocalDate, newWeight: WeightValue): Result<WeightEntry> {
        return Result.success(WeightEntry(date, newWeight, Clock.System.now()))
    }

    override fun deleteWeight(date: LocalDate): Result<Boolean> = Result.success(true)

    override fun getWeightHistory(): Result<List<WeightEntry>> {
        return Result.success(
            listOf(
                WeightEntry(
                    date = LocalDate(2026, 3, 17),
                    weight = WeightValue(82.0),
                    createdAt = Clock.System.now()
                )
            )
        )
    }

    override fun getWeightByDate(date: LocalDate): Result<WeightEntry?> = Result.success(null)

    override fun getWeightByDateRange(startDate: LocalDate, endDate: LocalDate): Result<List<WeightEntry>> {
        return Result.success(emptyList())
    }
}
