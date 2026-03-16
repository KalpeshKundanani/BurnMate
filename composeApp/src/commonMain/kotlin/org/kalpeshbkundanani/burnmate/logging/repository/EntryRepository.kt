package org.kalpeshbkundanani.burnmate.logging.repository

import org.kalpeshbkundanani.burnmate.logging.model.CalorieEntry
import org.kalpeshbkundanani.burnmate.logging.model.EntryDate
import org.kalpeshbkundanani.burnmate.logging.model.EntryId

interface EntryRepository {
    fun create(entry: CalorieEntry): Result<CalorieEntry>
    fun deleteById(id: EntryId): Result<Boolean>
    fun fetchByDateRange(startDate: EntryDate, endDate: EntryDate): Result<List<CalorieEntry>>
    fun fetchByDate(date: EntryDate): Result<List<CalorieEntry>>
}
