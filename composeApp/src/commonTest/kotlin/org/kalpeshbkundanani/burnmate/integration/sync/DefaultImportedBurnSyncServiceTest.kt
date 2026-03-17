package org.kalpeshbkundanani.burnmate.integration.sync

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.kalpeshbkundanani.burnmate.integration.model.ImportedBurnSample
import org.kalpeshbkundanani.burnmate.logging.model.CalorieAmount
import org.kalpeshbkundanani.burnmate.logging.model.CalorieEntry
import org.kalpeshbkundanani.burnmate.logging.model.EntryDate
import org.kalpeshbkundanani.burnmate.logging.model.EntryId
import org.kalpeshbkundanani.burnmate.logging.repository.LocalEntryRepository

class DefaultImportedBurnSyncServiceTest {

    @Test
    fun `t07 sync replaces only google owned entries`() {
        val repository = LocalEntryRepository()
        repository.create(
            CalorieEntry(
                id = EntryId("manual-1"),
                date = EntryDate(LocalDate(2026, 3, 10)),
                amount = CalorieAmount(450),
                createdAt = Instant.parse("2026-03-10T08:00:00Z")
            )
        )
        repository.create(
            CalorieEntry(
                id = EntryId("googlefit:2026-03-10:burn"),
                date = EntryDate(LocalDate(2026, 3, 10)),
                amount = CalorieAmount(-90),
                createdAt = Instant.parse("2026-03-10T12:00:00Z")
            )
        )
        val service = DefaultImportedBurnSyncService(repository)

        val summary = service.sync(
            startDate = LocalDate(2026, 3, 10),
            endDate = LocalDate(2026, 3, 12),
            samples = listOf(
                ImportedBurnSample(
                    entryId = "googlefit:2026-03-10:burn",
                    date = LocalDate(2026, 3, 10),
                    burnCalories = 120,
                    createdAt = Instant.parse("2026-03-10T12:00:00Z")
                ),
                ImportedBurnSample(
                    entryId = "googlefit:2026-03-11:burn",
                    date = LocalDate(2026, 3, 11),
                    burnCalories = 210,
                    createdAt = Instant.parse("2026-03-11T12:00:00Z")
                )
            )
        ).getOrThrow()

        val entries = repository.fetchByDateRange(
            startDate = EntryDate(LocalDate(2026, 3, 10)),
            endDate = EntryDate(LocalDate(2026, 3, 12))
        ).getOrThrow()

        assertEquals(2, summary.importedEntries)
        assertEquals(2, summary.importedDays)
        assertEquals(listOf("manual-1", "googlefit:2026-03-10:burn", "googlefit:2026-03-11:burn"), entries.map { it.id.value })
        assertEquals(listOf(450, -120, -210), entries.map { it.amount.value })
    }
}
