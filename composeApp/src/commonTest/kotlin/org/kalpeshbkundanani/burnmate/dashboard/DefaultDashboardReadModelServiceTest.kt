package org.kalpeshbkundanani.burnmate.dashboard

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.kalpeshbkundanani.burnmate.caloriedebt.domain.CalorieDebtCalculator
import org.kalpeshbkundanani.burnmate.caloriedebt.domain.DefaultCalorieDebtCalculator
import org.kalpeshbkundanani.burnmate.caloriedebt.model.CalculationWindow
import org.kalpeshbkundanani.burnmate.caloriedebt.model.CalorieDebtDay
import org.kalpeshbkundanani.burnmate.caloriedebt.model.CalorieDebtResult
import org.kalpeshbkundanani.burnmate.caloriedebt.model.CalorieDebtSeverity
import org.kalpeshbkundanani.burnmate.caloriedebt.model.CalorieDebtTrend
import org.kalpeshbkundanani.burnmate.caloriedebt.model.DailyCalorieEntry
import org.kalpeshbkundanani.burnmate.dashboard.domain.DefaultDashboardReadModelService
import org.kalpeshbkundanani.burnmate.dashboard.model.DebtChartPoint
import org.kalpeshbkundanani.burnmate.dashboard.model.DebtSummary
import org.kalpeshbkundanani.burnmate.dashboard.model.TodaySummary
import org.kalpeshbkundanani.burnmate.dashboard.model.WeightSummary
import org.kalpeshbkundanani.burnmate.logging.model.CalorieAmount
import org.kalpeshbkundanani.burnmate.logging.model.CalorieEntry
import org.kalpeshbkundanani.burnmate.logging.model.EntryDate
import org.kalpeshbkundanani.burnmate.logging.model.EntryId
import org.kalpeshbkundanani.burnmate.logging.repository.EntryRepository
import org.kalpeshbkundanani.burnmate.profile.model.BodyMetrics
import org.kalpeshbkundanani.burnmate.weight.domain.WeightHistoryService
import org.kalpeshbkundanani.burnmate.weight.model.WeightEntry
import org.kalpeshbkundanani.burnmate.weight.model.WeightValue

class DefaultDashboardReadModelServiceTest {

    @Test
    fun `T-01 snapshot generation`() {
        val debtResult = debtResult(
            finalDebtCalories = 150,
            severity = CalorieDebtSeverity.LOW,
            trend = CalorieDebtTrend.INCREASED,
            days = listOf(
                debtDay(2026, 3, 15, endingDebtCalories = 100),
                debtDay(2026, 3, 16, endingDebtCalories = 150)
            )
        )
        val service = createService(
            entries = listOf(
                entry("entry-001", 2026, 3, 16, 500, "2026-03-16T08:00:00Z"),
                entry("entry-002", 2026, 3, 16, 300, "2026-03-16T09:00:00Z"),
                entry("entry-003", 2026, 3, 16, -200, "2026-03-16T10:00:00Z")
            ),
            weightEntries = listOf(weightEntry(2026, 3, 16, 80.0)),
            debtCalculator = StubDebtCalculator(Result.success(debtResult))
        )

        val result = service.getDashboardSnapshot(date(2026, 3, 16))

        assertTrue(result.isSuccess)
        val snapshot = result.getOrThrow()
        assertEquals(
            TodaySummary(
                totalIntakeCalories = 800,
                totalBurnCalories = 200,
                netCalories = 600,
                remainingCalories = 1200,
                dailyTargetCalories = 2000
            ),
            snapshot.todaySummary
        )
        assertEquals(
            DebtSummary(
                currentDebtCalories = 150,
                severity = CalorieDebtSeverity.LOW,
                trend = CalorieDebtTrend.INCREASED
            ),
            snapshot.debtSummary
        )
        assertEquals(
            WeightSummary(
                currentWeightKg = 80.0,
                goalWeightKg = 70.0,
                remainingKg = 10.0,
                progressPercentage = 50.0
            ),
            snapshot.weightSummary
        )
        assertEquals(
            listOf(
                DebtChartPoint(date(2026, 3, 15), 100),
                DebtChartPoint(date(2026, 3, 16), 150)
            ),
            snapshot.debtChartPoints
        )
    }

    @Test
    fun `T-02 intake aggregation`() {
        val service = createService(
            entries = listOf(
                entry("entry-001", 2026, 3, 16, 500, "2026-03-16T08:00:00Z"),
                entry("entry-002", 2026, 3, 16, 300, "2026-03-16T09:00:00Z"),
                entry("entry-003", 2026, 3, 16, 200, "2026-03-16T10:00:00Z")
            )
        )

        val snapshot = service.getDashboardSnapshot(date(2026, 3, 16)).getOrThrow()

        assertEquals(1000, snapshot.todaySummary.totalIntakeCalories)
    }

    @Test
    fun `T-03 burn aggregation`() {
        val service = createService(
            entries = listOf(
                entry("entry-001", 2026, 3, 16, -200, "2026-03-16T08:00:00Z"),
                entry("entry-002", 2026, 3, 16, -150, "2026-03-16T09:00:00Z")
            )
        )

        val snapshot = service.getDashboardSnapshot(date(2026, 3, 16)).getOrThrow()

        assertEquals(350, snapshot.todaySummary.totalBurnCalories)
    }

    @Test
    fun `T-04 net calories calculation`() {
        val service = createService(
            entries = listOf(
                entry("entry-001", 2026, 3, 16, 500, "2026-03-16T08:00:00Z"),
                entry("entry-002", 2026, 3, 16, 300, "2026-03-16T09:00:00Z"),
                entry("entry-003", 2026, 3, 16, -200, "2026-03-16T10:00:00Z"),
                entry("entry-004", 2026, 3, 16, -150, "2026-03-16T11:00:00Z")
            )
        )

        val snapshot = service.getDashboardSnapshot(date(2026, 3, 16)).getOrThrow()

        assertEquals(450, snapshot.todaySummary.netCalories)
    }

    @Test
    fun `T-05 remaining calories calculation`() {
        val service = createService(
            entries = listOf(
                entry("entry-001", 2026, 3, 16, 500, "2026-03-16T08:00:00Z"),
                entry("entry-002", 2026, 3, 16, 300, "2026-03-16T09:00:00Z")
            )
        )

        val snapshot = service.getDashboardSnapshot(date(2026, 3, 16)).getOrThrow()

        assertEquals(1200, snapshot.todaySummary.remainingCalories)
    }

    @Test
    fun `T-06 weight progress computation`() {
        val service = createService(
            entries = emptyList(),
            weightEntries = listOf(weightEntry(2026, 3, 16, 80.0))
        )

        val snapshot = service.getDashboardSnapshot(date(2026, 3, 16)).getOrThrow()

        assertEquals(
            WeightSummary(
                currentWeightKg = 80.0,
                goalWeightKg = 70.0,
                remainingKg = 10.0,
                progressPercentage = 50.0
            ),
            snapshot.weightSummary
        )
    }

    @Test
    fun `T-07 chart dataset generation`() {
        val debtCalculator = StubDebtCalculator(
            Result.success(
                debtResult(
                    finalDebtCalories = 150,
                    severity = CalorieDebtSeverity.LOW,
                    trend = CalorieDebtTrend.REDUCED,
                    days = listOf(
                        debtDay(2026, 3, 16, endingDebtCalories = 150),
                        debtDay(2026, 3, 14, endingDebtCalories = 100),
                        debtDay(2026, 3, 15, endingDebtCalories = 200)
                    )
                )
            )
        )
        val service = createService(
            entries = emptyList(),
            debtCalculator = debtCalculator
        )

        val snapshot = service.getDashboardSnapshot(date(2026, 3, 16)).getOrThrow()

        assertEquals(
            listOf(
                DebtChartPoint(date(2026, 3, 14), 100),
                DebtChartPoint(date(2026, 3, 15), 200),
                DebtChartPoint(date(2026, 3, 16), 150)
            ),
            snapshot.debtChartPoints
        )
    }

    @Test
    fun `T-08 deterministic snapshot`() {
        val debtResult = debtResult(
            finalDebtCalories = 150,
            severity = CalorieDebtSeverity.LOW,
            trend = CalorieDebtTrend.INCREASED,
            days = listOf(
                debtDay(2026, 3, 15, endingDebtCalories = 100),
                debtDay(2026, 3, 16, endingDebtCalories = 150)
            )
        )
        val service = createService(
            entries = listOf(
                entry("entry-001", 2026, 3, 16, 500, "2026-03-16T08:00:00Z"),
                entry("entry-002", 2026, 3, 16, 300, "2026-03-16T09:00:00Z"),
                entry("entry-003", 2026, 3, 16, -200, "2026-03-16T10:00:00Z")
            ),
            weightEntries = listOf(weightEntry(2026, 3, 16, 80.0)),
            debtCalculator = StubDebtCalculator(Result.success(debtResult))
        )

        val first = service.getDashboardSnapshot(date(2026, 3, 16)).getOrThrow()
        val second = service.getDashboardSnapshot(date(2026, 3, 16)).getOrThrow()

        assertEquals(first, second)
    }

    @Test
    fun `T-09 empty logging day`() {
        val service = createService(entries = emptyList())

        val snapshot = service.getDashboardSnapshot(date(2026, 3, 16)).getOrThrow()

        assertEquals(
            TodaySummary(
                totalIntakeCalories = 0,
                totalBurnCalories = 0,
                netCalories = 0,
                remainingCalories = 2000,
                dailyTargetCalories = 2000
            ),
            snapshot.todaySummary
        )
    }

    @Test
    fun `T-10 multi-day chart data`() {
        val calculator = DefaultCalorieDebtCalculator()
        val service = createService(
            entries = listOf(
                entry("entry-001", 2026, 3, 10, 2100, "2026-03-10T08:00:00Z"),
                entry("entry-002", 2026, 3, 12, 2300, "2026-03-12T08:00:00Z"),
                entry("entry-003", 2026, 3, 14, 1900, "2026-03-14T08:00:00Z"),
                entry("entry-004", 2026, 3, 16, 2200, "2026-03-16T08:00:00Z")
            ),
            debtCalculator = calculator,
            chartWindowDays = 7
        )
        val expectedDebtResult = calculator.calculate(
            window = CalculationWindow(
                startDate = date(2026, 3, 10),
                endDate = date(2026, 3, 16),
                targetCalories = 2000
            ),
            entries = listOf(
                DailyCalorieEntry(date(2026, 3, 10), 2100),
                DailyCalorieEntry(date(2026, 3, 12), 2300),
                DailyCalorieEntry(date(2026, 3, 14), 1900),
                DailyCalorieEntry(date(2026, 3, 16), 2200)
            )
        ).getOrThrow()

        val snapshot = service.getDashboardSnapshot(date(2026, 3, 16)).getOrThrow()

        assertEquals(
            expectedDebtResult.days.map { DebtChartPoint(it.date, it.endingDebtCalories) },
            snapshot.debtChartPoints
        )
        assertEquals(7, snapshot.debtChartPoints.size)
    }

    private fun createService(
        entries: List<CalorieEntry>,
        weightEntries: List<WeightEntry> = emptyList(),
        debtCalculator: CalorieDebtCalculator = StubDebtCalculator(
            Result.success(
                debtResult(
                    finalDebtCalories = 0,
                    severity = CalorieDebtSeverity.NONE,
                    trend = CalorieDebtTrend.UNCHANGED,
                    days = emptyList()
                )
            )
        ),
        bodyMetrics: BodyMetrics = BodyMetrics(
            heightCm = 175.0,
            currentWeightKg = 90.0,
            goalWeightKg = 70.0
        ),
        dailyTargetCalories: Int = 2000,
        chartWindowDays: Int = DefaultDashboardReadModelService.DEFAULT_CHART_WINDOW_DAYS
    ): DefaultDashboardReadModelService = DefaultDashboardReadModelService(
        entryRepository = FakeEntryRepository(entries),
        debtCalculator = debtCalculator,
        weightHistoryService = FakeWeightHistoryService(weightEntries),
        bodyMetrics = bodyMetrics,
        dailyTargetCalories = dailyTargetCalories,
        chartWindowDays = chartWindowDays
    )

    private fun debtResult(
        finalDebtCalories: Int,
        severity: CalorieDebtSeverity,
        trend: CalorieDebtTrend,
        days: List<CalorieDebtDay>
    ): CalorieDebtResult = CalorieDebtResult(
        finalDebtCalories = finalDebtCalories,
        days = days,
        latestTrend = trend,
        debtStreakDays = 0,
        severity = severity
    )

    private fun debtDay(
        year: Int,
        month: Int,
        day: Int,
        endingDebtCalories: Int
    ): CalorieDebtDay = CalorieDebtDay(
        date = date(year, month, day),
        consumedCalories = 0,
        targetCalories = 2000,
        dailyDeltaCalories = 0,
        startingDebtCalories = 0,
        endingDebtCalories = endingDebtCalories
    )

    private fun weightEntry(year: Int, month: Int, day: Int, kg: Double): WeightEntry = WeightEntry(
        date = date(year, month, day),
        weight = WeightValue(kg),
        createdAt = Instant.parse("${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}T08:00:00Z")
    )

    private fun entry(
        id: String,
        year: Int,
        month: Int,
        day: Int,
        amount: Int,
        createdAt: String
    ): CalorieEntry = CalorieEntry(
        id = EntryId(id),
        date = EntryDate(date(year, month, day)),
        amount = CalorieAmount(amount),
        createdAt = Instant.parse(createdAt)
    )

    private fun date(year: Int, month: Int, day: Int): LocalDate = LocalDate(year, month, day)
}

private class StubDebtCalculator(
    private val result: Result<CalorieDebtResult>
) : CalorieDebtCalculator {
    var lastWindow: CalculationWindow? = null
    var lastEntries: List<DailyCalorieEntry> = emptyList()

    override fun calculate(
        window: CalculationWindow,
        entries: List<DailyCalorieEntry>
    ): Result<CalorieDebtResult> {
        lastWindow = window
        lastEntries = entries
        return result
    }
}

private class FakeEntryRepository(
    entries: List<CalorieEntry>
) : EntryRepository {
    private val storedEntries = entries.sortedWith(compareBy({ it.date.value }, { it.createdAt }, { it.id.value }))

    override fun create(entry: CalorieEntry): Result<CalorieEntry> =
        Result.failure(UnsupportedOperationException("create is not supported in dashboard tests"))

    override fun deleteById(id: EntryId): Result<Boolean> =
        Result.failure(UnsupportedOperationException("deleteById is not supported in dashboard tests"))

    override fun fetchByDateRange(startDate: EntryDate, endDate: EntryDate): Result<List<CalorieEntry>> =
        Result.success(
            storedEntries.filter { it.date.value >= startDate.value && it.date.value <= endDate.value }
        )

    override fun fetchByDate(date: EntryDate): Result<List<CalorieEntry>> =
        Result.success(storedEntries.filter { it.date == date })
}

private class FakeWeightHistoryService(
    private val entries: List<WeightEntry>
) : WeightHistoryService {
    override fun recordWeight(date: LocalDate, weight: WeightValue): Result<WeightEntry> =
        Result.failure(UnsupportedOperationException("recordWeight is not supported in dashboard tests"))

    override fun editWeight(date: LocalDate, newWeight: WeightValue): Result<WeightEntry> =
        Result.failure(UnsupportedOperationException("editWeight is not supported in dashboard tests"))

    override fun deleteWeight(date: LocalDate): Result<Boolean> =
        Result.failure(UnsupportedOperationException("deleteWeight is not supported in dashboard tests"))

    override fun getWeightHistory(): Result<List<WeightEntry>> = Result.success(entries)

    override fun getWeightByDate(date: LocalDate): Result<WeightEntry?> =
        Result.failure(UnsupportedOperationException("getWeightByDate is not supported in dashboard tests"))

    override fun getWeightByDateRange(startDate: LocalDate, endDate: LocalDate): Result<List<WeightEntry>> =
        Result.failure(UnsupportedOperationException("getWeightByDateRange is not supported in dashboard tests"))
}
