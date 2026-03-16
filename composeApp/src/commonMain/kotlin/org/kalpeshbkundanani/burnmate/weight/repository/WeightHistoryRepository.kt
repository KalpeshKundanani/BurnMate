package org.kalpeshbkundanani.burnmate.weight.repository

import kotlinx.datetime.LocalDate
import org.kalpeshbkundanani.burnmate.weight.model.WeightEntry

interface WeightHistoryRepository {
    fun save(entry: WeightEntry): Result<WeightEntry>
    fun update(entry: WeightEntry): Result<WeightEntry>
    fun deleteByDate(date: LocalDate): Result<Boolean>
    fun getByDate(date: LocalDate): Result<WeightEntry?>
    fun getByDateRange(startDate: LocalDate, endDate: LocalDate): Result<List<WeightEntry>>
    fun getAll(): Result<List<WeightEntry>>
}
