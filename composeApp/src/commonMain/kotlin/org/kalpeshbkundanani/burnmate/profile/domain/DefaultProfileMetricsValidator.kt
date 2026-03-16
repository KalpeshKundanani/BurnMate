package org.kalpeshbkundanani.burnmate.profile.domain

import org.kalpeshbkundanani.burnmate.profile.model.BodyMetrics
import org.kalpeshbkundanani.burnmate.profile.model.ProfileDomainError

class DefaultProfileMetricsValidator : ProfileMetricsValidator {
    override fun validate(metrics: BodyMetrics): Result<Unit> {
        if (metrics.heightCm <= 0.0) {
            return validationFailure("INVALID_HEIGHT", "heightCm must be greater than zero")
        }

        if (metrics.currentWeightKg <= 0.0) {
            return validationFailure(
                "INVALID_CURRENT_WEIGHT",
                "currentWeightKg must be greater than zero"
            )
        }

        if (metrics.goalWeightKg <= 0.0) {
            return validationFailure("INVALID_GOAL_WEIGHT", "goalWeightKg must be greater than zero")
        }

        if (metrics.goalWeightKg >= metrics.currentWeightKg) {
            return validationFailure(
                "GOAL_NOT_BELOW_CURRENT_WEIGHT",
                "goalWeightKg must be lower than currentWeightKg"
            )
        }

        return Result.success(Unit)
    }

    private fun validationFailure(code: String, detail: String): Result<Unit> =
        Result.failure(ProfileDomainError.Validation(code = code, detail = detail))
}
