package org.kalpeshbkundanani.burnmate.caloriedebt

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.kalpeshbkundanani.burnmate.caloriedebt.domain.DefaultCalorieDebtCalculator
import org.kalpeshbkundanani.burnmate.caloriedebt.model.CalculationWindow
import org.kalpeshbkundanani.burnmate.caloriedebt.model.CalorieDebtError
import org.kalpeshbkundanani.burnmate.caloriedebt.model.DailyCalorieEntry

class DefaultCalorieDebtValidatorTest {

    private val calculator = DefaultCalorieDebtCalculator()

    @Test
    fun duplicateDatesAreRejected() {
        val window = CalculationWindow(date(2026, 3, 1), date(2026, 3, 2), 2000)
        val entries = listOf(
            DailyCalorieEntry(date(2026, 3, 1), 2000),
            DailyCalorieEntry(date(2026, 3, 1), 2200)
        )

        val result = calculator.calculate(window, entries)
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull() as CalorieDebtError.Validation
        assertEquals("DUPLICATE_ENTRY_DATE", error.code)
    }

    @Test
    fun invertedRangeIsRejected() {
        val window = CalculationWindow(date(2026, 3, 3), date(2026, 3, 1), 2000)
        val result = calculator.calculate(window, emptyList())

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull() as CalorieDebtError.Validation
        assertEquals("INVALID_DATE_RANGE", error.code)
    }

    @Test
    fun negativeConsumedCaloriesAreRejected() {
        val window = CalculationWindow(date(2026, 3, 1), date(2026, 3, 1), 2000)
        val entries = listOf(DailyCalorieEntry(date(2026, 3, 1), -1))

        val result = calculator.calculate(window, entries)
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull() as CalorieDebtError.Validation
        assertEquals("INVALID_CONSUMED_CALORIES", error.code)
    }

    private fun date(year: Int, month: Int, day: Int): LocalDate = LocalDate(year, month, day)
}
