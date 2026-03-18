package org.kalpeshbkundanani.burnmate.logging

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.kalpeshbkundanani.burnmate.logging.domain.DefaultCalorieEntryValidator
import org.kalpeshbkundanani.burnmate.logging.model.CalorieAmount
import org.kalpeshbkundanani.burnmate.logging.model.EntryDate
import org.kalpeshbkundanani.burnmate.logging.model.EntryValidationError

class DefaultCalorieEntryValidatorTest {

    private val validator = DefaultCalorieEntryValidator()

    @Test
    fun negativeCalorieAmountIsAcceptedForBurnEntries() {
        val result = validator.validate(date(2026, 3, 15), CalorieAmount(-1))

        assertTrue(result.isSuccess)
    }

    @Test
    fun unrealisticCalorieAmountIsRejected() {
        val result = validator.validate(date(2026, 3, 15), CalorieAmount(15_001))

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull() as EntryValidationError.UnrealisticCalorieAmount
        assertEquals("UNREALISTIC_CALORIE_AMOUNT", error.message?.substringBefore(':'))
        assertEquals(15_001, error.amount)
        assertEquals(15_000, error.maxAllowed)
    }

    @Test
    fun boundaryCalorieAmountsAreAccepted() {
        val zeroCaloriesResult = validator.validate(date(2026, 3, 15), CalorieAmount(0))
        val maxCaloriesResult = validator.validate(date(2026, 3, 15), CalorieAmount(15_000))

        assertTrue(zeroCaloriesResult.isSuccess)
        assertTrue(maxCaloriesResult.isSuccess)
    }

    private fun date(year: Int, month: Int, day: Int): EntryDate = EntryDate(LocalDate(year, month, day))
}
