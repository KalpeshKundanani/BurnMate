package org.kalpeshbkundanani.burnmate.weight.domain

import org.kalpeshbkundanani.burnmate.caloriedebt.domain.CalorieDebtCalculator
import org.kalpeshbkundanani.burnmate.caloriedebt.model.CalculationWindow
import org.kalpeshbkundanani.burnmate.caloriedebt.model.DailyCalorieEntry
import org.kalpeshbkundanani.burnmate.weight.model.DebtRecalculationResult
import org.kalpeshbkundanani.burnmate.weight.model.WeightValue

class DefaultDebtRecalculationService(
    private val calculator: CalorieDebtCalculator
) : DebtRecalculationService {

    override fun recomputeDebt(
        newWeight: WeightValue,
        window: CalculationWindow,
        entries: List<DailyCalorieEntry>
    ): Result<DebtRecalculationResult> {
        val adjustedTargetCalories = (newWeight.kg * ADJUSTED_TARGET_MULTIPLIER).toInt()
        val adjustedWindow = window.copy(targetCalories = adjustedTargetCalories)
        val debtResult = calculator.calculate(adjustedWindow, entries)

        if (debtResult.isFailure) {
            return Result.failure(debtResult.exceptionOrNull()!!)
        }

        return Result.success(
            DebtRecalculationResult(
                triggeringWeight = newWeight,
                adjustedTargetCalories = adjustedTargetCalories,
                debtResult = debtResult.getOrThrow()
            )
        )
    }

    companion object {
        private const val ADJUSTED_TARGET_MULTIPLIER: Double = 22.0
    }
}
