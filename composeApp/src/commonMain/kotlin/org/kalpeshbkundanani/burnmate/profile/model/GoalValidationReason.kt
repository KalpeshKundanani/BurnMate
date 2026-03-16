package org.kalpeshbkundanani.burnmate.profile.model

enum class GoalValidationReason {
    VALID,
    INVALID_HEIGHT,
    INVALID_CURRENT_WEIGHT,
    INVALID_GOAL_WEIGHT,
    GOAL_NOT_BELOW_CURRENT_WEIGHT,
    GOAL_BMI_BELOW_HEALTHY_RANGE,
    GOAL_BMI_ABOVE_HEALTHY_RANGE
}
