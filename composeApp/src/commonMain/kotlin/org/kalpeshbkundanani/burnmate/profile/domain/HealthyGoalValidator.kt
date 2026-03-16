package org.kalpeshbkundanani.burnmate.profile.domain

import org.kalpeshbkundanani.burnmate.profile.model.BodyMetrics
import org.kalpeshbkundanani.burnmate.profile.model.BmiSnapshot
import org.kalpeshbkundanani.burnmate.profile.model.GoalValidationResult

interface HealthyGoalValidator {
    fun validate(
        metrics: BodyMetrics,
        currentBmi: BmiSnapshot,
        goalBmi: BmiSnapshot
    ): GoalValidationResult
}
