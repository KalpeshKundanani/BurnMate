package org.kalpeshbkundanani.burnmate.profile.model

data class GoalValidationResult(
    val isValid: Boolean,
    val reason: GoalValidationReason,
    val kilogramsToLose: Double?,
    val bmiDelta: Double?
)
