package org.kalpeshbkundanani.burnmate.profile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.kalpeshbkundanani.burnmate.profile.domain.DefaultUserProfileFactory
import org.kalpeshbkundanani.burnmate.profile.model.BodyMetrics
import org.kalpeshbkundanani.burnmate.profile.model.GoalValidationReason
import org.kalpeshbkundanani.burnmate.profile.model.ProfileDomainError

class DefaultUserProfileFactoryTest {

    private val factory = DefaultUserProfileFactory()

    @Test
    fun validProfileCreatesSummary() {
        val result = factory.create(BodyMetrics(heightCm = 175.0, currentWeightKg = 82.0, goalWeightKg = 72.0))

        assertTrue(result.isSuccess)
        val summary = result.getOrThrow()
        assertEquals(175.0, summary.metrics.heightCm)
        assertEquals(82.0, summary.metrics.currentWeightKg)
        assertEquals(72.0, summary.metrics.goalWeightKg)
        assertEquals(10.0, summary.kilogramsToLose)
        assertEquals(GoalValidationReason.VALID, summary.goalValidation.reason)
        assertTrue(summary.goalValidation.isValid)
    }

    @Test
    fun zeroHeightIsRejected() {
        val result = factory.create(BodyMetrics(heightCm = 0.0, currentWeightKg = 82.0, goalWeightKg = 72.0))

        assertValidationCode(result, "INVALID_HEIGHT")
    }

    @Test
    fun negativeCurrentWeightIsRejected() {
        val result = factory.create(BodyMetrics(heightCm = 175.0, currentWeightKg = -1.0, goalWeightKg = 72.0))

        assertValidationCode(result, "INVALID_CURRENT_WEIGHT")
    }

    @Test
    fun goalWeightEqualToCurrentWeightIsRejected() {
        val result = factory.create(BodyMetrics(heightCm = 175.0, currentWeightKg = 82.0, goalWeightKg = 82.0))

        assertValidationCode(result, "GOAL_NOT_BELOW_CURRENT_WEIGHT")
    }

    @Test
    fun goalWeightAboveCurrentWeightIsRejected() {
        val result = factory.create(BodyMetrics(heightCm = 175.0, currentWeightKg = 82.0, goalWeightKg = 90.0))

        assertValidationCode(result, "GOAL_NOT_BELOW_CURRENT_WEIGHT")
    }

    @Test
    fun derivedHelpersIncludeKilogramsToLoseAndBmiDelta() {
        val result = factory.create(BodyMetrics(heightCm = 165.0, currentWeightKg = 78.0, goalWeightKg = 65.0))

        assertTrue(result.isSuccess)
        val summary = result.getOrThrow()
        assertEquals(13.0, summary.kilogramsToLose)
        assertEquals(4.8, summary.bmiDelta)
        assertEquals(13.0, summary.goalValidation.kilogramsToLose)
        assertEquals(4.8, summary.goalValidation.bmiDelta)
    }

    private fun assertValidationCode(result: Result<*>, expectedCode: String) {
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull() as ProfileDomainError.Validation
        assertEquals(expectedCode, error.code)
    }
}
