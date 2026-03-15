package org.kalpeshbkundanani.burnmate.caloriedebt

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.kalpeshbkundanani.burnmate.caloriedebt.domain.DefaultCalorieDebtCalculator
import org.kalpeshbkundanani.burnmate.caloriedebt.domain.DefaultDebtTrendClassifier
import org.kalpeshbkundanani.burnmate.caloriedebt.model.CalculationWindow
import org.kalpeshbkundanani.burnmate.caloriedebt.model.CalorieDebtError
import org.kalpeshbkundanani.burnmate.caloriedebt.model.CalorieDebtSeverity
import org.kalpeshbkundanani.burnmate.caloriedebt.model.CalorieDebtTrend
import org.kalpeshbkundanani.burnmate.caloriedebt.model.DailyCalorieEntry

class DefaultCalorieDebtCalculatorTest {

    private val calculator = DefaultCalorieDebtCalculator()

    @Test
    fun overTargetDayCreatesDebt() {
        val window = CalculationWindow(date(2026, 3, 1), date(2026, 3, 1), 2000)
        val result = calculator.calculate(window, listOf(DailyCalorieEntry(date(2026, 3, 1), 2300)))

        assertTrue(result.isSuccess)
        val value = result.getOrThrow()
        assertEquals(300, value.finalDebtCalories)
        assertEquals(1, value.days.size)
        assertEquals(CalorieDebtTrend.INCREASED, value.latestTrend)
        assertEquals(CalorieDebtSeverity.MEDIUM, value.severity)
    }

    @Test
    fun underTargetWithNoDebtStaysZero() {
        val window = CalculationWindow(date(2026, 3, 1), date(2026, 3, 1), 2000)
        val result = calculator.calculate(window, listOf(DailyCalorieEntry(date(2026, 3, 1), 1700)))

        assertTrue(result.isSuccess)
        val value = result.getOrThrow()
        assertEquals(0, value.finalDebtCalories)
        assertEquals(CalorieDebtTrend.UNCHANGED, value.latestTrend)
        assertEquals(CalorieDebtSeverity.NONE, value.severity)
    }

    @Test
    fun underTargetReducesDebtWithoutGoingNegative() {
        val window = CalculationWindow(date(2026, 3, 1), date(2026, 3, 2), 2000)
        val entries = listOf(
            DailyCalorieEntry(date(2026, 3, 1), 2400),
            DailyCalorieEntry(date(2026, 3, 2), 1500)
        )

        val result = calculator.calculate(window, entries)
        val value = result.getOrThrow()

        assertEquals(0, value.finalDebtCalories)
        assertEquals(400, value.days[0].endingDebtCalories)
        assertEquals(0, value.days[1].endingDebtCalories)
        assertEquals(CalorieDebtTrend.CLEARED, value.latestTrend)
    }

    @Test
    fun missingDateProducesZeroConsumptionRow() {
        val window = CalculationWindow(date(2026, 3, 1), date(2026, 3, 3), 2000)
        val entries = listOf(
            DailyCalorieEntry(date(2026, 3, 1), 2100),
            DailyCalorieEntry(date(2026, 3, 3), 2200)
        )

        val result = calculator.calculate(window, entries)
        val value = result.getOrThrow()

        assertEquals(3, value.days.size)
        val middleDay = value.days[1]
        assertEquals(date(2026, 3, 2), middleDay.date)
        assertEquals(0, middleDay.consumedCalories)
    }

    @Test
    fun entriesOutsideRangeAreIgnored() {
        val window = CalculationWindow(date(2026, 3, 2), date(2026, 3, 2), 2000)
        val entries = listOf(
            DailyCalorieEntry(date(2026, 3, 1), 2500),
            DailyCalorieEntry(date(2026, 3, 2), 2100)
        )

        val result = calculator.calculate(window, entries)
        val value = result.getOrThrow()

        assertEquals(1, value.days.size)
        assertEquals(date(2026, 3, 2), value.days.single().date)
        assertEquals(100, value.finalDebtCalories)
    }

    @Test
    fun debtStreakCountsTrailingPositiveDebtWithPositiveDelta() {
        val window = CalculationWindow(date(2026, 3, 1), date(2026, 3, 4), 2000)
        val entries = listOf(
            DailyCalorieEntry(date(2026, 3, 1), 2100),
            DailyCalorieEntry(date(2026, 3, 2), 2200),
            DailyCalorieEntry(date(2026, 3, 3), 1800),
            DailyCalorieEntry(date(2026, 3, 4), 2300)
        )

        val result = calculator.calculate(window, entries)
        val value = result.getOrThrow()

        assertEquals(1, value.debtStreakDays)
        assertEquals(400, value.finalDebtCalories)
    }

    @Test
    fun severityThresholdsMapCorrectly() {
        val classifier = DefaultDebtTrendClassifier()
        assertEquals(CalorieDebtSeverity.NONE, classifier.classifySeverity(0))
        assertEquals(CalorieDebtSeverity.LOW, classifier.classifySeverity(1))
        assertEquals(CalorieDebtSeverity.LOW, classifier.classifySeverity(299))
        assertEquals(CalorieDebtSeverity.MEDIUM, classifier.classifySeverity(300))
        assertEquals(CalorieDebtSeverity.MEDIUM, classifier.classifySeverity(699))
        assertEquals(CalorieDebtSeverity.HIGH, classifier.classifySeverity(700))
    }

    private fun date(year: Int, month: Int, day: Int): LocalDate = LocalDate(year, month, day)
}
