package org.kalpeshbkundanani.burnmate.weight.domain

import kotlinx.datetime.LocalDate
import org.kalpeshbkundanani.burnmate.weight.model.WeightEntry
import org.kalpeshbkundanani.burnmate.weight.model.WeightValue

interface WeightHistoryService {
    fun recordWeight(date: LocalDate, weight: WeightValue): Result<WeightEntry>
    fun editWeight(date: LocalDate, newWeight: WeightValue): Result<WeightEntry>
    fun deleteWeight(date: LocalDate): Result<Boolean>
    fun getWeightHistory(): Result<List<WeightEntry>>
    fun getWeightByDate(date: LocalDate): Result<WeightEntry?>
    fun getWeightByDateRange(startDate: LocalDate, endDate: LocalDate): Result<List<WeightEntry>>
}
