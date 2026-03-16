package org.kalpeshbkundanani.burnmate.weight

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.kalpeshbkundanani.burnmate.weight.domain.DefaultWeightHistoryService
import org.kalpeshbkundanani.burnmate.weight.model.WeightHistoryError
import org.kalpeshbkundanani.burnmate.weight.model.WeightValue
import org.kalpeshbkundanani.burnmate.weight.repository.LocalWeightRepository

class DefaultWeightHistoryServiceTest {

    @Test
    fun `T-01 create weight entry`() {
        val debtService = FakeDebtRecalculationService()
        val service = createService("2026-03-16T09:00:00Z", debtService)

        val result = service.recordWeight(date(2026, 3, 16), WeightValue(82.4))

        assertTrue(result.isSuccess)
        val entry = result.getOrThrow()
        assertEquals(date(2026, 3, 16), entry.date)
        assertEquals(82.4, entry.weight.kg)
        assertEquals(Instant.parse("2026-03-16T09:00:00Z"), entry.createdAt)
        assertEquals(1, debtService.invocationCount)
    }

    @Test
    fun `T-02 reject duplicate date`() {
        val service = createService("2026-03-16T09:00:00Z")
        service.recordWeight(date(2026, 3, 16), WeightValue(82.4))

        val result = service.recordWeight(date(2026, 3, 16), WeightValue(82.0))

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull() as WeightHistoryError.DuplicateWeightDate
        assertEquals("DUPLICATE_WEIGHT_DATE", error.message?.substringBefore(':'))
    }

    @Test
    fun `T-03 retrieve history`() {
        val repository = LocalWeightRepository()
        val service = DefaultWeightHistoryService(
            repository = repository,
            clock = { Instant.parse("2026-03-16T09:00:00Z") }
        )
        service.recordWeight(date(2026, 3, 14), WeightValue(81.5))
        service.recordWeight(date(2026, 3, 15), WeightValue(81.1))

        val result = service.getWeightByDateRange(date(2026, 3, 14), date(2026, 3, 15))

        assertTrue(result.isSuccess)
        assertEquals(listOf(81.5, 81.1), result.getOrThrow().map { it.weight.kg })
    }

    @Test
    fun `T-04 update entry`() {
        val repository = LocalWeightRepository()
        val debtService = FakeDebtRecalculationService()
        val timestamps = listOf(
            Instant.parse("2026-03-15T07:00:00Z"),
            Instant.parse("2026-03-16T07:00:00Z")
        ).iterator()
        val service = DefaultWeightHistoryService(
            repository = repository,
            debtService = debtService,
            clock = { timestamps.next() }
        )
        service.recordWeight(date(2026, 3, 15), WeightValue(82.4))
        debtService.reset()

        val updateResult = service.editWeight(date(2026, 3, 15), WeightValue(81.9))

        assertTrue(updateResult.isSuccess)
        val updatedEntry = updateResult.getOrThrow()
        assertEquals(81.9, updatedEntry.weight.kg)
        assertEquals(Instant.parse("2026-03-16T07:00:00Z"), updatedEntry.createdAt)
        assertEquals(81.9, service.getWeightByDate(date(2026, 3, 15)).getOrThrow()?.weight?.kg)
        assertEquals(1, debtService.invocationCount)
        assertEquals(listOf(81.9), debtService.weights.map { it.kg })
    }

    @Test
    fun `T-05 delete entry`() {
        val debtService = FakeDebtRecalculationService()
        val service = createService("2026-03-16T09:00:00Z", debtService)
        service.recordWeight(date(2026, 3, 16), WeightValue(82.4))
        debtService.reset()

        val deleteResult = service.deleteWeight(date(2026, 3, 16))

        assertTrue(deleteResult.isSuccess)
        assertEquals(true, deleteResult.getOrThrow())
        assertNull(service.getWeightByDate(date(2026, 3, 16)).getOrThrow())
        assertEquals(1, debtService.invocationCount)
        assertEquals(listOf(82.4), debtService.weights.map { it.kg })
    }

    @Test
    fun `T-09 empty history retrieval`() {
        val service = createService("2026-03-16T09:00:00Z")

        val result = service.getWeightHistory()

        assertTrue(result.isSuccess)
        assertEquals(emptyList(), result.getOrThrow())
    }

    private fun createService(
        now: String,
        debtService: FakeDebtRecalculationService = FakeDebtRecalculationService()
    ): DefaultWeightHistoryService = DefaultWeightHistoryService(
        repository = LocalWeightRepository(),
        debtService = debtService,
        clock = { Instant.parse(now) }
    )

    private fun date(year: Int, month: Int, day: Int): LocalDate = LocalDate(year, month, day)
}
