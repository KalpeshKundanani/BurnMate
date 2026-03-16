package org.kalpeshbkundanani.burnmate.weight.model

import org.kalpeshbkundanani.burnmate.caloriedebt.model.CalorieDebtResult

data class DebtRecalculationResult(
    val triggeringWeight: WeightValue,
    val adjustedTargetCalories: Int,
    val debtResult: CalorieDebtResult
)
