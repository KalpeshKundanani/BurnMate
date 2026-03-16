package org.kalpeshbkundanani.burnmate.profile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.kalpeshbkundanani.burnmate.profile.domain.DefaultBmiCalculator
import org.kalpeshbkundanani.burnmate.profile.domain.DefaultHealthyGoalValidator
import org.kalpeshbkundanani.burnmate.profile.domain.DefaultUserProfileFactory
import org.kalpeshbkundanani.burnmate.profile.model.BodyMetrics
import org.kalpeshbkundanani.burnmate.profile.model.GoalValidationReason
import org.kalpeshbkundanani.burnmate.profile.model.ProfileDomainError

class DefaultHealthyGoalValidatorTest {

    private val bmiCalculator = DefaultBmiCalculator()
    private val validator = DefaultHealthyGoalValidator()
    private val factory = DefaultUserProfileFactory()

    @Test
    fun goalBmiBelowHealthyRangeIsRejected() {
        val result = factory.create(BodyMetrics(heightCm = 175.0, currentWeightKg = 70.0, goalWeightKg = 54.0))

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull() as ProfileDomainError.Validation
        assertEquals("GOAL_BMI_BELOW_HEALTHY_RANGE", error.code)
    }

    @Test
    fun goalBmiInsideHealthyRangeSucceeds() {
        val result = factory.create(BodyMetrics(heightCm = 175.0, currentWeightKg = 84.0, goalWeightKg = 72.0))

        assertTrue(result.isSuccess)
        val summary = result.getOrThrow()
        assertEquals(GoalValidationReason.VALID, summary.goalValidation.reason)
        assertTrue(summary.goalValidation.isValid)
    }

    @Test
    fun validatorReturnsNullDerivedValuesForInvalidResult() {
        val metrics = BodyMetrics(heightCm = 175.0, currentWeightKg = 70.0, goalWeightKg = 54.0)
        val currentBmi = bmiCalculator.calculate(metrics.heightCm, metrics.currentWeightKg)
        val goalBmi = bmiCalculator.calculate(metrics.heightCm, metrics.goalWeightKg)

        val result = validator.validate(metrics, currentBmi, goalBmi)

        assertFalse(result.isValid)
        assertEquals(GoalValidationReason.GOAL_BMI_BELOW_HEALTHY_RANGE, result.reason)
        assertEquals(null, result.kilogramsToLose)
        assertEquals(null, result.bmiDelta)
    }
}
