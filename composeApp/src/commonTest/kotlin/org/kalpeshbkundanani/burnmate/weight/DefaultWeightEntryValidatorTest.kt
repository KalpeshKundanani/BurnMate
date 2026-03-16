package org.kalpeshbkundanani.burnmate.weight

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.kalpeshbkundanani.burnmate.weight.domain.DefaultWeightEntryValidator
import org.kalpeshbkundanani.burnmate.weight.model.WeightHistoryError
import org.kalpeshbkundanani.burnmate.weight.model.WeightValue

class DefaultWeightEntryValidatorTest {

    private val validator = DefaultWeightEntryValidator()

    @Test
    fun `T-06 boundary weight validation`() {
        assertTrue(validator.validate(WeightValue(0.5)).isSuccess)
        assertTrue(validator.validate(WeightValue(500.0)).isSuccess)

        val invalidWeights = listOf(0.0, -1.0, 0.49, 500.01)
        invalidWeights.forEach { kg ->
            val result = validator.validate(WeightValue(kg))
            assertTrue(result.isFailure)
            val error = result.exceptionOrNull() as WeightHistoryError.Validation
            assertEquals(DefaultWeightEntryValidator.INVALID_WEIGHT_VALUE, error.code)
        }
    }
}
