package org.kalpeshbkundanani.burnmate.logging

import kotlinx.datetime.LocalDate
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.kalpeshbkundanani.burnmate.logging.model.CalorieAmount
import org.kalpeshbkundanani.burnmate.logging.model.CalorieEntry
import org.kalpeshbkundanani.burnmate.logging.model.EntryDate
import org.kalpeshbkundanani.burnmate.logging.model.EntryId
import org.kalpeshbkundanani.burnmate.logging.model.EntryRepositoryError
import org.kalpeshbkundanani.burnmate.logging.repository.EntryRepository
import org.kalpeshbkundanani.burnmate.logging.repository.LocalEntryRepository

class LocalEntryRepositoryTest {

    @Test
    fun deletingAnExistingEntryRemovesItFromRepository() {
        val repository = createRepository()
        val entry = entry(
            id = "entry-001",
            year = 2026,
            month = 3,
            day = 15,
            amount = 1_500,
            createdAt = "2026-03-15T09:00:00Z"
        )
        repository.create(entry)

        val deleteResult = repository.deleteById(entry.id)
        val fetchResult = repository.fetchByDate(date(2026, 3, 15))

        assertTrue(deleteResult.isSuccess)
        assertEquals(true, deleteResult.getOrThrow())
        assertTrue(fetchResult.isSuccess)
        assertEquals(emptyList(), fetchResult.getOrThrow())
    }

    @Test
    fun deletingANonExistentEntryReturnsFalse() {
        val repository = createRepository()

        val result = repository.deleteById(EntryId("missing-entry"))

        assertTrue(result.isSuccess)
        assertEquals(false, result.getOrThrow())
    }

    @Test
    fun fetchByDateRangeReturnsEntriesInChronologicalOrder() {
        val repository = createRepository()
        repository.create(entry("entry-002", 2026, 3, 15, 2_000, "2026-03-15T09:00:00Z"))
        repository.create(entry("entry-003", 2026, 3, 14, 1_900, "2026-03-14T09:00:00Z"))
        repository.create(entry("entry-001", 2026, 3, 13, 1_800, "2026-03-13T09:00:00Z"))

        val result = repository.fetchByDateRange(date(2026, 3, 13), date(2026, 3, 15))

        assertTrue(result.isSuccess)
        assertEquals(listOf("entry-001", "entry-003", "entry-002"), result.getOrThrow().map { it.id.value })
    }

    @Test
    fun fetchForEmptyDateRangeReturnsEmptyList() {
        val repository = createRepository()

        val result = repository.fetchByDateRange(date(2026, 3, 13), date(2026, 3, 15))

        assertTrue(result.isSuccess)
        assertEquals(emptyList(), result.getOrThrow())
    }

    @Test
    fun invertedDateRangeIsRejected() {
        val repository = createRepository()

        val result = repository.fetchByDateRange(date(2026, 3, 15), date(2026, 3, 10))

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull() as EntryRepositoryError.InvalidDateRange
        assertEquals("INVALID_DATE_RANGE", error.message?.substringBefore(':'))
    }

    @Test
    fun duplicateEntryCreationIsRejected() {
        val repository = createRepository()
        val entry = entry("entry-001", 2026, 3, 15, 1_500, "2026-03-15T09:00:00Z")

        val firstCreate = repository.create(entry)
        val secondCreate = repository.create(entry)

        assertTrue(firstCreate.isSuccess)
        assertTrue(secondCreate.isFailure)
        val error = secondCreate.exceptionOrNull() as EntryRepositoryError.DuplicateEntry
        assertEquals("DUPLICATE_ENTRY", error.message?.substringBefore(':'))
    }

    @Test
    fun repositoryContractMethodsBehaveConsistently() {
        val repository: EntryRepository = createRepository()
        val firstEntry = entry("entry-001", 2026, 3, 15, 1_250, "2026-03-15T07:00:00Z")
        val secondEntry = entry("entry-002", 2026, 3, 15, 1_450, "2026-03-15T08:00:00Z")

        assertTrue(repository.create(firstEntry).isSuccess)
        assertTrue(repository.create(secondEntry).isSuccess)
        assertEquals(listOf(firstEntry, secondEntry), repository.fetchByDate(date(2026, 3, 15)).getOrThrow())
        assertEquals(listOf(firstEntry, secondEntry), repository.fetchByDateRange(date(2026, 3, 15), date(2026, 3, 15)).getOrThrow())
        assertEquals(true, repository.deleteById(firstEntry.id).getOrThrow())
        assertEquals(listOf(secondEntry), repository.fetchByDate(date(2026, 3, 15)).getOrThrow())
    }

    private fun createRepository(): LocalEntryRepository = LocalEntryRepository()

    private fun date(year: Int, month: Int, day: Int): EntryDate = EntryDate(LocalDate(year, month, day))

    private fun entry(
        id: String,
        year: Int,
        month: Int,
        day: Int,
        amount: Int,
        createdAt: String
    ): CalorieEntry = CalorieEntry(
        id = EntryId(id),
        date = date(year, month, day),
        amount = CalorieAmount(amount),
        createdAt = Instant.parse(createdAt)
    )
}
