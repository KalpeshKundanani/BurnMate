package org.kalpeshbkundanani.burnmate.profile.model

data class UserProfileSummary(
    val metrics: BodyMetrics,
    val currentBmi: BmiSnapshot,
    val goalBmi: BmiSnapshot,
    val kilogramsToLose: Double,
    val bmiDelta: Double,
    val goalValidation: GoalValidationResult
)
