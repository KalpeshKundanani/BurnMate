package org.kalpeshbkundanani.burnmate.profile.domain

import kotlin.math.floor
import org.kalpeshbkundanani.burnmate.profile.model.BodyMetrics
import org.kalpeshbkundanani.burnmate.profile.model.BmiSnapshot
import org.kalpeshbkundanani.burnmate.profile.model.GoalValidationReason
import org.kalpeshbkundanani.burnmate.profile.model.GoalValidationResult

class DefaultHealthyGoalValidator : HealthyGoalValidator {
    override fun validate(
        metrics: BodyMetrics,
        currentBmi: BmiSnapshot,
        goalBmi: BmiSnapshot
    ): GoalValidationResult {
        if (metrics.goalWeightKg >= metrics.currentWeightKg) {
            return GoalValidationResult(
                isValid = false,
                reason = GoalValidationReason.GOAL_NOT_BELOW_CURRENT_WEIGHT,
                kilogramsToLose = null,
                bmiDelta = null
            )
        }

        val kilogramsToLose = metrics.currentWeightKg - metrics.goalWeightKg
        val bmiDelta = floor(((currentBmi.value - goalBmi.value) * 10.0) + 0.5) / 10.0

        return GoalValidationResult(
            isValid = true,
            reason = GoalValidationReason.VALID,
            kilogramsToLose = kilogramsToLose,
            bmiDelta = bmiDelta
        )
    }
}
