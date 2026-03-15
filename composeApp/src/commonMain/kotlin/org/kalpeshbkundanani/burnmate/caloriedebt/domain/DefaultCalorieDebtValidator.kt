package org.kalpeshbkundanani.burnmate.caloriedebt.domain

import kotlinx.datetime.LocalDate
import org.kalpeshbkundanani.burnmate.caloriedebt.model.CalculationWindow
import org.kalpeshbkundanani.burnmate.caloriedebt.model.CalorieDebtError
import org.kalpeshbkundanani.burnmate.caloriedebt.model.DailyCalorieEntry

class DefaultCalorieDebtValidator : CalorieDebtValidator {
    override fun validate(
        window: CalculationWindow,
        entries: List<DailyCalorieEntry>
    ): Result<Unit> {
        if (window.startDate > window.endDate) {
            return Result.failure(
                CalorieDebtError.Validation(
                    code = "INVALID_DATE_RANGE",
                    detail = "startDate must be on or before endDate"
                )
            )
        }

        if (window.targetCalories < 0) {
            return Result.failure(
                CalorieDebtError.Validation(
                    code = "INVALID_TARGET_CALORIES",
                    detail = "targetCalories must be non-negative"
                )
            )
        }

        val seenDates = mutableSetOf<LocalDate>()

        entries.forEach { entry ->
            if (entry.consumedCalories < 0) {
                return Result.failure(
                    CalorieDebtError.Validation(
                        code = "INVALID_CONSUMED_CALORIES",
                        detail = "consumedCalories cannot be negative for ${entry.date}"
                    )
                )
            }

            if (!seenDates.add(entry.date)) {
                return Result.failure(
                    CalorieDebtError.Validation(
                        code = "DUPLICATE_ENTRY_DATE",
                        detail = "duplicate entry for date ${entry.date}"
                    )
                )
            }
        }

        return Result.success(Unit)
    }
}
