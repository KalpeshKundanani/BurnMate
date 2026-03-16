package org.kalpeshbkundanani.burnmate.profile.domain

import org.kalpeshbkundanani.burnmate.profile.model.BodyMetrics
import org.kalpeshbkundanani.burnmate.profile.model.ProfileDomainError
import org.kalpeshbkundanani.burnmate.profile.model.UserProfileSummary

class DefaultUserProfileFactory(
    private val validator: ProfileMetricsValidator = DefaultProfileMetricsValidator(),
    private val bmiCalculator: BmiCalculator = DefaultBmiCalculator(),
    private val healthyGoalValidator: HealthyGoalValidator = DefaultHealthyGoalValidator()
) : UserProfileFactory {

    override fun create(metrics: BodyMetrics): Result<UserProfileSummary> {
        val validation = validator.validate(metrics)
        if (validation.isFailure) {
            return Result.failure(validation.exceptionOrNull()!!)
        }

        val currentBmi = bmiCalculator.calculate(metrics.heightCm, metrics.currentWeightKg)
        val goalBmi = bmiCalculator.calculate(metrics.heightCm, metrics.goalWeightKg)
        val goalValidation = healthyGoalValidator.validate(metrics, currentBmi, goalBmi)

        if (!goalValidation.isValid) {
            return Result.failure(
                ProfileDomainError.Validation(
                    code = goalValidation.reason.name,
                    detail = goalValidation.reason.name
                )
            )
        }

        return Result.success(
            UserProfileSummary(
                metrics = metrics,
                currentBmi = currentBmi,
                goalBmi = goalBmi,
                kilogramsToLose = goalValidation.kilogramsToLose!!,
                bmiDelta = goalValidation.bmiDelta!!,
                goalValidation = goalValidation
            )
        )
    }
}
