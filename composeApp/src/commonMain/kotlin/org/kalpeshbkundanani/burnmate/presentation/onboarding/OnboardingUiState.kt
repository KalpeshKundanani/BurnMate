package org.kalpeshbkundanani.burnmate.presentation.onboarding

import org.kalpeshbkundanani.burnmate.presentation.shared.UiMessage
import org.kalpeshbkundanani.burnmate.profile.model.UserProfileSummary

enum class OnboardingField {
    HEIGHT, CURRENT_WEIGHT, GOAL_WEIGHT
}

data class OnboardingUiState(
    val heightInput: String = "",
    val currentWeightInput: String = "",
    val goalWeightInput: String = "",
    val goalWeightSuggestion: UiMessage? = null,
    val isSubmitting: Boolean = false,
    val fieldErrors: Map<OnboardingField, UiMessage> = emptyMap(),
    val submitError: UiMessage? = null,
    val isSubmitEnabled: Boolean = false
)

data class OnboardingSuccessEvent(
    val eventId: Long,
    val profileSummary: UserProfileSummary
)

sealed interface OnboardingEvent {
    data class HeightChanged(val value: String) : OnboardingEvent
    data class CurrentWeightChanged(val value: String) : OnboardingEvent
    data class GoalWeightChanged(val value: String) : OnboardingEvent
    data object Submit : OnboardingEvent
    data object DismissError : OnboardingEvent
}
