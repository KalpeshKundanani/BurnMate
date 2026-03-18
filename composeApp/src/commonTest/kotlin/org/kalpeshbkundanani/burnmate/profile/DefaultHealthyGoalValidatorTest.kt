package org.kalpeshbkundanani.burnmate.profile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.kalpeshbkundanani.burnmate.profile.domain.DefaultBmiCalculator
import org.kalpeshbkundanani.burnmate.profile.domain.DefaultHealthyGoalValidator
import org.kalpeshbkundanani.burnmate.profile.domain.DefaultUserProfileFactory
import org.kalpeshbkundanani.burnmate.profile.model.BodyMetrics
import org.kalpeshbkundanani.burnmate.profile.model.GoalValidationReason

class DefaultHealthyGoalValidatorTest {

    private val bmiCalculator = DefaultBmiCalculator()
    private val validator = DefaultHealthyGoalValidator()
    private val factory = DefaultUserProfileFactory()

    @Test
    fun goalBmiBelowHealthyRangeStillSucceeds() {
        val result = factory.create(BodyMetrics(heightCm = 175.0, currentWeightKg = 70.0, goalWeightKg = 54.0))

        assertTrue(result.isSuccess)
        val summary = result.getOrThrow()
        assertEquals(GoalValidationReason.VALID, summary.goalValidation.reason)
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
    fun validatorStillRejectsGoalAtOrAboveCurrentWeight() {
        val result = factory.create(BodyMetrics(heightCm = 175.0, currentWeightKg = 70.0, goalWeightKg = 70.0))

        assertTrue(result.isFailure)
    }

    @Test
    fun validatorReturnsDerivedValuesForValidCustomGoal() {
        val metrics = BodyMetrics(heightCm = 175.0, currentWeightKg = 70.0, goalWeightKg = 54.0)
        val currentBmi = bmiCalculator.calculate(metrics.heightCm, metrics.currentWeightKg)
        val goalBmi = bmiCalculator.calculate(metrics.heightCm, metrics.goalWeightKg)

        val result = validator.validate(metrics, currentBmi, goalBmi)

        assertTrue(result.isValid)
        assertEquals(GoalValidationReason.VALID, result.reason)
        assertEquals(16.0, result.kilogramsToLose)
        assertEquals(5.3, result.bmiDelta)
    }
}
