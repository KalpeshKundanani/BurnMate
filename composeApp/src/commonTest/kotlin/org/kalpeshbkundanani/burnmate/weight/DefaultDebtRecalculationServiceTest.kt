package org.kalpeshbkundanani.burnmate.weight

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.kalpeshbkundanani.burnmate.caloriedebt.domain.DefaultCalorieDebtCalculator
import org.kalpeshbkundanani.burnmate.caloriedebt.model.CalculationWindow
import org.kalpeshbkundanani.burnmate.caloriedebt.model.DailyCalorieEntry
import org.kalpeshbkundanani.burnmate.weight.domain.DefaultDebtRecalculationService
import org.kalpeshbkundanani.burnmate.weight.model.DebtRecalculationResult
import org.kalpeshbkundanani.burnmate.weight.model.WeightValue

class DefaultDebtRecalculationServiceTest {

    private val service = DefaultDebtRecalculationService(DefaultCalorieDebtCalculator())

    @Test
    fun `T-07 recompute debt on change`() {
        val result = service.recomputeDebt(
            newWeight = WeightValue(80.0),
            window = window(2026, 3, 15, 2026, 3, 16, 2000),
            entries = listOf(
                DailyCalorieEntry(date(2026, 3, 15), 1900),
                DailyCalorieEntry(date(2026, 3, 16), 2100)
            )
        )

        assertTrue(result.isSuccess)
        val recalculation = result.getOrThrow()
        assertEquals(1760, recalculation.adjustedTargetCalories)
        assertEquals(480, recalculation.debtResult.finalDebtCalories)
        assertEquals(1760, recalculation.debtResult.days.first().targetCalories)
    }

    @Test
    fun `T-10 deterministic recomputation`() {
        val weight = WeightValue(78.3)
        val window = window(2026, 3, 10, 2026, 3, 12, 2100)
        val entries = listOf(
            DailyCalorieEntry(date(2026, 3, 10), 1800),
            DailyCalorieEntry(date(2026, 3, 11), 2200),
            DailyCalorieEntry(date(2026, 3, 12), 2300)
        )

        val first = service.recomputeDebt(weight, window, entries)
        val second = service.recomputeDebt(weight, window, entries)

        assertTrue(first.isSuccess)
        assertTrue(second.isSuccess)
        assertEquals(first.getOrThrow(), second.getOrThrow())
    }

    private fun window(
        startYear: Int,
        startMonth: Int,
        startDay: Int,
        endYear: Int,
        endMonth: Int,
        endDay: Int,
        targetCalories: Int
    ): CalculationWindow = CalculationWindow(
        startDate = date(startYear, startMonth, startDay),
        endDate = date(endYear, endMonth, endDay),
        targetCalories = targetCalories
    )

    private fun date(year: Int, month: Int, day: Int): LocalDate = LocalDate(year, month, day)
}
