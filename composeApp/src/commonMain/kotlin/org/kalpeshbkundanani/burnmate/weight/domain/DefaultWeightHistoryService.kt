package org.kalpeshbkundanani.burnmate.weight.domain

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.kalpeshbkundanani.burnmate.caloriedebt.model.CalculationWindow
import org.kalpeshbkundanani.burnmate.caloriedebt.model.CalorieDebtResult
import org.kalpeshbkundanani.burnmate.caloriedebt.model.CalorieDebtSeverity
import org.kalpeshbkundanani.burnmate.caloriedebt.model.CalorieDebtTrend
import org.kalpeshbkundanani.burnmate.caloriedebt.model.DailyCalorieEntry
import org.kalpeshbkundanani.burnmate.weight.model.WeightEntry
import org.kalpeshbkundanani.burnmate.weight.model.DebtRecalculationResult
import org.kalpeshbkundanani.burnmate.weight.model.WeightHistoryError
import org.kalpeshbkundanani.burnmate.weight.model.WeightValue
import org.kalpeshbkundanani.burnmate.weight.repository.WeightHistoryRepository

class DefaultWeightHistoryService(
    private val validator: WeightEntryValidator = DefaultWeightEntryValidator(),
    private val repository: WeightHistoryRepository,
    private val debtService: DebtRecalculationService = NoOpDebtRecalculationService,
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

        val saveResult = repository.save(
            WeightEntry(
                date = date,
                weight = weight,
                createdAt = clock()
            )
        )

        if (saveResult.isSuccess) {
            triggerDebtRecalculation(date = date, weight = saveResult.getOrThrow().weight)
        }

        return saveResult
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

        val updateResult = repository.update(
            existingEntry.copy(
                weight = newWeight,
                createdAt = clock()
            )
        )

        if (updateResult.isSuccess) {
            triggerDebtRecalculation(date = date, weight = updateResult.getOrThrow().weight)
        }

        return updateResult
    }

    override fun deleteWeight(date: LocalDate): Result<Boolean> {
        val existingEntryResult = repository.getByDate(date)
        if (existingEntryResult.isFailure) {
            return Result.failure(existingEntryResult.exceptionOrNull()!!)
        }

        val existingEntry = existingEntryResult.getOrThrow()
        val deleteResult = repository.deleteByDate(date)

        if (deleteResult.isSuccess && deleteResult.getOrThrow() && existingEntry != null) {
            triggerDebtRecalculation(date = date, weight = existingEntry.weight)
        }

        return deleteResult
    }

    override fun getWeightHistory(): Result<List<WeightEntry>> = repository.getAll()

    override fun getWeightByDate(date: LocalDate): Result<WeightEntry?> = repository.getByDate(date)

    override fun getWeightByDateRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<List<WeightEntry>> = repository.getByDateRange(startDate, endDate)

    private fun triggerDebtRecalculation(date: LocalDate, weight: WeightValue) {
        debtService.recomputeDebt(
            newWeight = weight,
            window = CalculationWindow(
                startDate = date,
                endDate = date,
                targetCalories = 0
            ),
            entries = emptyList()
        )
    }
}

private object NoOpDebtRecalculationService : DebtRecalculationService {
    override fun recomputeDebt(
        newWeight: WeightValue,
        window: CalculationWindow,
        entries: List<DailyCalorieEntry>
    ) = Result.success(
        DebtRecalculationResult(
            triggeringWeight = newWeight,
            adjustedTargetCalories = window.targetCalories,
            debtResult = CalorieDebtResult(
                finalDebtCalories = 0,
                days = emptyList(),
                latestTrend = CalorieDebtTrend.UNCHANGED,
                debtStreakDays = 0,
                severity = CalorieDebtSeverity.NONE
            )
        )
    )
}
