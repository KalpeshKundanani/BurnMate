package org.kalpeshbkundanani.burnmate.logging

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.kalpeshbkundanani.burnmate.logging.domain.DefaultCalorieEntryFactory
import org.kalpeshbkundanani.burnmate.logging.domain.DefaultCalorieEntryValidator
import org.kalpeshbkundanani.burnmate.logging.model.CalorieAmount
import org.kalpeshbkundanani.burnmate.logging.model.EntryDate
import org.kalpeshbkundanani.burnmate.logging.model.EntryValidationError

class DefaultCalorieEntryFactoryTest {

    @Test
    fun validEntryCreationSucceeds() {
        val factory = DefaultCalorieEntryFactory(
            validator = DefaultCalorieEntryValidator(),
            idGenerator = { "entry-001" },
            clock = { Instant.parse("2026-03-16T10:15:30Z") }
        )

        val result = factory.create(date(2026, 3, 15), CalorieAmount(1_500))

        assertTrue(result.isSuccess)
        val entry = result.getOrThrow()
        assertEquals("entry-001", entry.id.value)
        assertEquals(date(2026, 3, 15), entry.date)
        assertEquals(1_500, entry.amount.value)
        assertEquals(Instant.parse("2026-03-16T10:15:30Z"), entry.createdAt)
    }

    @Test
    fun deterministicInputsProduceDeterministicEntryOutput() {
        val expectedInstant = Instant.parse("2026-03-16T10:15:30Z")
        val factory = DefaultCalorieEntryFactory(
            validator = DefaultCalorieEntryValidator(),
            idGenerator = { "entry-fixed" },
            clock = { expectedInstant }
        )

        val firstResult = factory.create(date(2026, 3, 15), CalorieAmount(2_100))
        val secondResult = factory.create(date(2026, 3, 15), CalorieAmount(2_100))

        assertTrue(firstResult.isSuccess)
        assertTrue(secondResult.isSuccess)
        assertEquals(firstResult.getOrThrow(), secondResult.getOrThrow())
    }

    @Test
    fun unrealisticCalorieAmountIsRejectedDuringCreation() {
        val factory = DefaultCalorieEntryFactory(
            validator = DefaultCalorieEntryValidator(),
            idGenerator = { "entry-002" },
            clock = { Instant.parse("2026-03-16T10:15:30Z") }
        )

        val result = factory.create(date(2026, 3, 15), CalorieAmount(15_001))

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull() as EntryValidationError.UnrealisticCalorieAmount
        assertEquals("UNREALISTIC_CALORIE_AMOUNT", error.message?.substringBefore(':'))
    }

    private fun date(year: Int, month: Int, day: Int): EntryDate = EntryDate(LocalDate(year, month, day))
}
