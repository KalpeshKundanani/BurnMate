package org.kalpeshbkundanani.burnmate.logging.repository

import org.kalpeshbkundanani.burnmate.logging.model.CalorieEntry
import org.kalpeshbkundanani.burnmate.logging.model.EntryDate
import org.kalpeshbkundanani.burnmate.logging.model.EntryId
import org.kalpeshbkundanani.burnmate.logging.model.EntryRepositoryError

class LocalEntryRepository : EntryRepository {

    private val entriesById = linkedMapOf<EntryId, CalorieEntry>()
    private val entryIdsByDate = linkedMapOf<EntryDate, MutableList<EntryId>>()

    override fun create(entry: CalorieEntry): Result<CalorieEntry> {
        if (entriesById.containsKey(entry.id)) {
            return Result.failure(
                EntryRepositoryError.DuplicateEntry(
                    id = entry.id,
                    detail = "entry with id ${entry.id.value} already exists"
                )
            )
        }

        entriesById[entry.id] = entry
        entryIdsByDate.getOrPut(entry.date) { mutableListOf() }.add(entry.id)
        return Result.success(entry)
    }

    override fun deleteById(id: EntryId): Result<Boolean> {
        val removed = entriesById.remove(id) ?: return Result.success(false)
        val idsForDate = entryIdsByDate[removed.date]

        if (idsForDate != null) {
            idsForDate.remove(id)
            if (idsForDate.isEmpty()) {
                entryIdsByDate.remove(removed.date)
            }
        }

        return Result.success(true)
    }

    override fun fetchByDateRange(startDate: EntryDate, endDate: EntryDate): Result<List<CalorieEntry>> {
        if (startDate.value > endDate.value) {
            return Result.failure(
                EntryRepositoryError.InvalidDateRange(
                    startDate = startDate,
                    endDate = endDate,
                    detail = "startDate must be on or before endDate"
                )
            )
        }

        val sortedEntries = entryIdsByDate
            .filterKeys { date -> date.value >= startDate.value && date.value <= endDate.value }
            .values
            .flatten()
            .mapNotNull(entriesById::get)
            .sortedWith(
                compareBy<CalorieEntry> { it.date.value }
                    .thenBy { it.createdAt }
                    .thenBy { it.id.value }
            )

        return Result.success(sortedEntries)
    }

    override fun fetchByDate(date: EntryDate): Result<List<CalorieEntry>> = fetchByDateRange(date, date)
}
