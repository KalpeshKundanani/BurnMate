package org.kalpeshbkundanani.burnmate.weight.domain

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.kalpeshbkundanani.burnmate.weight.model.WeightEntry
import org.kalpeshbkundanani.burnmate.weight.model.WeightHistoryError
import org.kalpeshbkundanani.burnmate.weight.model.WeightValue
import org.kalpeshbkundanani.burnmate.weight.repository.WeightHistoryRepository

class DefaultWeightHistoryService(
    private val validator: WeightEntryValidator = DefaultWeightEntryValidator(),
    private val repository: WeightHistoryRepository,
    private val clock: () -> Instant = { Clock.System.now() }
) : WeightHistoryService {

    override fun recordWeight(date: LocalDate, weight: WeightValue): Result<WeightEntry> {
        val validationResult = validator.validate(weight)
        if (validationResult.isFailure) {
            return Result.failure(validationResult.exceptionOrNull()!!)
        }

        val existingEntryResult = repository.getByDate(date)
        if (existingEntryResult.isFailure) {
            return Result.failure(existingEntryResult.exceptionOrNull()!!)
        }
        if (existingEntryResult.getOrThrow() != null) {
            return Result.failure(
                WeightHistoryError.DuplicateWeightDate(
                    date = date,
                    detail = "weight entry for $date already exists"
                )
            )
        }

        return repository.save(
            WeightEntry(
                date = date,
                weight = weight,
                createdAt = clock()
            )
        )
    }

    override fun editWeight(date: LocalDate, newWeight: WeightValue): Result<WeightEntry> {
        val validationResult = validator.validate(newWeight)
        if (validationResult.isFailure) {
            return Result.failure(validationResult.exceptionOrNull()!!)
        }

        val existingEntryResult = repository.getByDate(date)
        if (existingEntryResult.isFailure) {
            return Result.failure(existingEntryResult.exceptionOrNull()!!)
        }

        val existingEntry = existingEntryResult.getOrThrow()
            ?: return Result.failure(
                WeightHistoryError.WeightEntryNotFound(
                    date = date,
                    detail = "weight entry for $date does not exist"
                )
            )

        return repository.update(
            existingEntry.copy(
                weight = newWeight,
                createdAt = clock()
            )
        )
    }

    override fun deleteWeight(date: LocalDate): Result<Boolean> = repository.deleteByDate(date)

    override fun getWeightHistory(): Result<List<WeightEntry>> = repository.getAll()

    override fun getWeightByDate(date: LocalDate): Result<WeightEntry?> = repository.getByDate(date)

    override fun getWeightByDateRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<List<WeightEntry>> = repository.getByDateRange(startDate, endDate)
}
