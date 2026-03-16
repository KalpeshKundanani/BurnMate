package org.kalpeshbkundanani.burnmate.caloriedebt.domain

import org.kalpeshbkundanani.burnmate.caloriedebt.model.CalculationWindow
import org.kalpeshbkundanani.burnmate.caloriedebt.model.DailyCalorieEntry

interface CalorieDebtValidator {
    fun validate(
        window: CalculationWindow,
        entries: List<DailyCalorieEntry>
    ): Result<Unit>
}
