package org.kalpeshbkundanani.burnmate.weight

import org.kalpeshbkundanani.burnmate.caloriedebt.model.CalculationWindow
import org.kalpeshbkundanani.burnmate.caloriedebt.model.DailyCalorieEntry
import org.kalpeshbkundanani.burnmate.caloriedebt.model.CalorieDebtResult
import org.kalpeshbkundanani.burnmate.caloriedebt.model.CalorieDebtSeverity
import org.kalpeshbkundanani.burnmate.caloriedebt.model.CalorieDebtTrend
import org.kalpeshbkundanani.burnmate.weight.domain.DebtRecalculationService
import org.kalpeshbkundanani.burnmate.weight.model.DebtRecalculationResult
import org.kalpeshbkundanani.burnmate.weight.model.WeightValue

class FakeDebtRecalculationService : DebtRecalculationService {
    var invocationCount: Int = 0
        private set

    val weights = mutableListOf<WeightValue>()

    fun reset() {
        invocationCount = 0
        weights.clear()
    }

    override fun recomputeDebt(
        newWeight: WeightValue,
        window: CalculationWindow,
        entries: List<DailyCalorieEntry>
    ): Result<DebtRecalculationResult> {
        invocationCount += 1
        weights += newWeight

        return Result.success(
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
}
