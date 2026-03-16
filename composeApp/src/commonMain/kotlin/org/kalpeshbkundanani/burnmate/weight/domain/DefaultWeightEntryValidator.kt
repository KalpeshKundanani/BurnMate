package org.kalpeshbkundanani.burnmate.weight.domain

import org.kalpeshbkundanani.burnmate.weight.model.WeightHistoryError
import org.kalpeshbkundanani.burnmate.weight.model.WeightValue

class DefaultWeightEntryValidator(
    private val minWeightKg: Double = MIN_WEIGHT_KG,
    private val maxWeightKg: Double = MAX_WEIGHT_KG
) : WeightEntryValidator {

    override fun validate(weight: WeightValue): Result<Unit> {
        return when {
            weight.kg < minWeightKg -> Result.failure(
                WeightHistoryError.Validation(
                    code = INVALID_WEIGHT_VALUE,
                    detail = "weight must be at least $minWeightKg kg"
                )
            )

            weight.kg > maxWeightKg -> Result.failure(
                WeightHistoryError.Validation(
                    code = INVALID_WEIGHT_VALUE,
                    detail = "weight must be at most $maxWeightKg kg"
                )
            )

            else -> Result.success(Unit)
        }
    }

    companion object {
        const val INVALID_WEIGHT_VALUE: String = "INVALID_WEIGHT_VALUE"
        const val MIN_WEIGHT_KG: Double = 0.5
        const val MAX_WEIGHT_KG: Double = 500.0
    }
}
