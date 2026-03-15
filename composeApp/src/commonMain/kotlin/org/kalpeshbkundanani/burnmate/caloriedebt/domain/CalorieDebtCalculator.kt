package org.kalpeshbkundanani.burnmate.caloriedebt.domain

import org.kalpeshbkundanani.burnmate.caloriedebt.model.CalculationWindow
import org.kalpeshbkundanani.burnmate.caloriedebt.model.CalorieDebtResult
import org.kalpeshbkundanani.burnmate.caloriedebt.model.DailyCalorieEntry

interface CalorieDebtCalculator {
    fun calculate(
        window: CalculationWindow,
        entries: List<DailyCalorieEntry>
    ): Result<CalorieDebtResult>
}
