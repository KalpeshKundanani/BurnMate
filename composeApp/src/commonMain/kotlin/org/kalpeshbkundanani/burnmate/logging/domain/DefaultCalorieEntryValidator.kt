package org.kalpeshbkundanani.burnmate.logging.domain

import org.kalpeshbkundanani.burnmate.logging.model.CalorieAmount
import org.kalpeshbkundanani.burnmate.logging.model.EntryDate
import org.kalpeshbkundanani.burnmate.logging.model.EntryValidationError

class DefaultCalorieEntryValidator(
    private val maxCalorieAmount: Int = MAX_CALORIE_AMOUNT
) : CalorieEntryValidator {

    override fun validate(date: EntryDate, amount: CalorieAmount): Result<Unit> {
        val value = amount.value
        val absoluteValue = kotlin.math.abs(value)

        if (absoluteValue > maxCalorieAmount) {
            return Result.failure(
                EntryValidationError.UnrealisticCalorieAmount(
                    amount = value,
                    maxAllowed = maxCalorieAmount,
                    detail = "absolute calorie amount must be <= $maxCalorieAmount for ${date.value}"
                )
            )
        }

        return Result.success(Unit)
    }

    companion object {
        const val MAX_CALORIE_AMOUNT: Int = 15_000
    }
}
