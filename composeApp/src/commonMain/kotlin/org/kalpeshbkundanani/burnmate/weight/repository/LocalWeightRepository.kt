package org.kalpeshbkundanani.burnmate.weight.repository

import kotlinx.datetime.LocalDate
import org.kalpeshbkundanani.burnmate.weight.model.WeightEntry
import org.kalpeshbkundanani.burnmate.weight.model.WeightHistoryError

class LocalWeightRepository : WeightHistoryRepository {

    private val entriesByDate = linkedMapOf<LocalDate, WeightEntry>()

    override fun save(entry: WeightEntry): Result<WeightEntry> {
        if (entriesByDate.containsKey(entry.date)) {
            return Result.failure(
                WeightHistoryError.DuplicateWeightDate(
                    date = entry.date,
                    detail = "weight entry for ${entry.date} already exists"
                )
            )
        }

        entriesByDate[entry.date] = entry
        return Result.success(entry)
    }

    override fun update(entry: WeightEntry): Result<WeightEntry> {
        if (!entriesByDate.containsKey(entry.date)) {
            return Result.failure(
                WeightHistoryError.WeightEntryNotFound(
                    date = entry.date,
                    detail = "weight entry for ${entry.date} does not exist"
                )
            )
        }

        entriesByDate[entry.date] = entry
        return Result.success(entry)
    }

    override fun deleteByDate(date: LocalDate): Result<Boolean> = Result.success(entriesByDate.remove(date) != null)

    override fun getByDate(date: LocalDate): Result<WeightEntry?> = Result.success(entriesByDate[date])

    override fun getByDateRange(startDate: LocalDate, endDate: LocalDate): Result<List<WeightEntry>> {
        if (startDate > endDate) {
            return Result.failure(
                WeightHistoryError.InvalidDateRange(
                    startDate = startDate,
                    endDate = endDate,
                    detail = "startDate must be on or before endDate"
                )
            )
        }

        return Result.success(
            entriesByDate.values
                .filter { it.date >= startDate && it.date <= endDate }
                .sortedBy { it.date }
        )
    }

    override fun getAll(): Result<List<WeightEntry>> = Result.success(entriesByDate.values.sortedBy { it.date })
}
