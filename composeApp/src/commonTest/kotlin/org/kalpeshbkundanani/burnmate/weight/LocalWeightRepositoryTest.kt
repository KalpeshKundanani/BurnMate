package org.kalpeshbkundanani.burnmate.weight

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.kalpeshbkundanani.burnmate.weight.model.WeightEntry
import org.kalpeshbkundanani.burnmate.weight.model.WeightHistoryError
import org.kalpeshbkundanani.burnmate.weight.model.WeightValue
import org.kalpeshbkundanani.burnmate.weight.repository.LocalWeightRepository

class LocalWeightRepositoryTest {

    @Test
    fun `T-08 chronological retrieval`() {
        val repository = LocalWeightRepository()
        repository.save(entry(2026, 3, 15, 82.0, "2026-03-15T08:00:00Z"))
        repository.save(entry(2026, 3, 13, 80.5, "2026-03-13T08:00:00Z"))
        repository.save(entry(2026, 3, 14, 81.2, "2026-03-14T08:00:00Z"))

        val result = repository.getAll()

        assertTrue(result.isSuccess)
        assertEquals(
            listOf(date(2026, 3, 13), date(2026, 3, 14), date(2026, 3, 15)),
            result.getOrThrow().map { it.date }
        )
    }

    @Test
    fun invertedDateRangeIsRejected() {
        val repository = LocalWeightRepository()

        val result = repository.getByDateRange(date(2026, 3, 15), date(2026, 3, 14))

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull() as WeightHistoryError.InvalidDateRange
        assertEquals("INVALID_DATE_RANGE", error.message?.substringBefore(':'))
    }

    private fun entry(year: Int, month: Int, day: Int, kg: Double, createdAt: String): WeightEntry = WeightEntry(
        date = date(year, month, day),
        weight = WeightValue(kg),
        createdAt = Instant.parse(createdAt)
    )

    private fun date(year: Int, month: Int, day: Int): LocalDate = LocalDate(year, month, day)
}
