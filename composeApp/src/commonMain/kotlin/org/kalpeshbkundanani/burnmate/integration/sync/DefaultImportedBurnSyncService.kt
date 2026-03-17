package org.kalpeshbkundanani.burnmate.integration.sync

import kotlinx.datetime.LocalDate
import org.kalpeshbkundanani.burnmate.integration.model.GoogleFitSyncSummary
import org.kalpeshbkundanani.burnmate.integration.model.GoogleIntegrationError
import org.kalpeshbkundanani.burnmate.integration.model.ImportedBurnSample
import org.kalpeshbkundanani.burnmate.logging.model.CalorieAmount
import org.kalpeshbkundanani.burnmate.logging.model.CalorieEntry
import org.kalpeshbkundanani.burnmate.logging.model.EntryDate
import org.kalpeshbkundanani.burnmate.logging.model.EntryId
import org.kalpeshbkundanani.burnmate.logging.repository.EntryRepository

class DefaultImportedBurnSyncService(
    private val entryRepository: EntryRepository
) : ImportedBurnSyncService {

    override fun sync(
        startDate: LocalDate,
        endDate: LocalDate,
        samples: List<ImportedBurnSample>
    ): Result<GoogleFitSyncSummary> {
        if (startDate > endDate) {
            return Result.failure(IllegalArgumentException("INVALID_IMPORT_WINDOW"))
        }

        samples.forEach { sample ->
            require(sample.burnCalories > 0) { "INVALID_BURN_SAMPLE" }
            require(sample.entryId.startsWith(GOOGLE_FIT_ENTRY_PREFIX)) { "INVALID_INTEGRATION_ID" }
        }

        val existingEntries = entryRepository
            .fetchByDateRange(EntryDate(startDate), EntryDate(endDate))
            .getOrElse { return Result.failure(it) }
        val googleOwnedEntries = existingEntries.filter { it.id.value.startsWith(GOOGLE_FIT_ENTRY_PREFIX) }
        val createdEntries = mutableListOf<CalorieEntry>()

        try {
            googleOwnedEntries.forEach { entry ->
                entryRepository.deleteById(entry.id).getOrElse { throw it }
            }

            samples.forEach { sample ->
                val entry = CalorieEntry(
                    id = EntryId(sample.entryId),
                    date = EntryDate(sample.date),
                    amount = CalorieAmount(-sample.burnCalories),
                    createdAt = sample.createdAt
                )
                entryRepository.create(entry).getOrElse { throw it }
                createdEntries += entry
            }
        } catch (error: Throwable) {
            rollback(createdEntries, googleOwnedEntries)
            return Result.failure(
                GoogleIntegrationError.SyncFailed(error.message ?: "sync failed")
            )
        }

        return Result.success(
            GoogleFitSyncSummary(
                startDate = startDate,
                endDate = endDate,
                importedEntries = samples.size,
                importedDays = samples.map { it.date }.distinct().size
            )
        )
    }

    private fun rollback(createdEntries: List<CalorieEntry>, removedEntries: List<CalorieEntry>) {
        createdEntries.forEach { entryRepository.deleteById(it.id) }
        removedEntries.forEach { entryRepository.create(it) }
    }

    companion object {
        const val GOOGLE_FIT_ENTRY_PREFIX: String = "googlefit:"
    }
}
