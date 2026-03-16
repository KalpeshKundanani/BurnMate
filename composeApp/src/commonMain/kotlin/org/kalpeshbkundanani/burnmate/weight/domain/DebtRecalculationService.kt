package org.kalpeshbkundanani.burnmate.weight.domain

import org.kalpeshbkundanani.burnmate.caloriedebt.model.CalculationWindow
import org.kalpeshbkundanani.burnmate.caloriedebt.model.DailyCalorieEntry
import org.kalpeshbkundanani.burnmate.weight.model.DebtRecalculationResult
import org.kalpeshbkundanani.burnmate.weight.model.WeightValue

interface DebtRecalculationService {
    fun recomputeDebt(
        newWeight: WeightValue,
        window: CalculationWindow,
        entries: List<DailyCalorieEntry>
    ): Result<DebtRecalculationResult>
}
