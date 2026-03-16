package org.kalpeshbkundanani.burnmate.logging.domain

import org.kalpeshbkundanani.burnmate.logging.model.CalorieAmount
import org.kalpeshbkundanani.burnmate.logging.model.EntryDate
import org.kalpeshbkundanani.burnmate.logging.model.EntryValidationError

class DefaultCalorieEntryValidator(
    private val maxCalorieAmount: Int = MAX_CALORIE_AMOUNT
) : CalorieEntryValidator {

    override fun validate(date: EntryDate, amount: CalorieAmount): Result<Unit> {
        val value = amount.value

        if (value < 0) {
            return Result.failure(
                EntryValidationError.InvalidCalorieAmount(
                    amount = value,
                    detail = "calorie amount must be non-negative for ${date.value}"
                )
            )
        }

        if (value > maxCalorieAmount) {
            return Result.failure(
                EntryValidationError.UnrealisticCalorieAmount(
                    amount = value,
                    maxAllowed = maxCalorieAmount,
                    detail = "calorie amount must be <= $maxCalorieAmount for ${date.value}"
                )
            )
        }

        return Result.success(Unit)
    }

    companion object {
        const val MAX_CALORIE_AMOUNT: Int = 15_000
    }
}
