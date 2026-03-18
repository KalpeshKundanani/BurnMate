package org.kalpeshbkundanani.burnmate.presentation.onboarding

import org.kalpeshbkundanani.burnmate.presentation.shared.UiMessage
import org.kalpeshbkundanani.burnmate.profile.model.ProfileDomainError

data class OnboardingErrorState(
    val fieldErrors: Map<OnboardingField, UiMessage> = emptyMap(),
    val submitError: UiMessage? = null
)

class OnboardingErrorMapper {

    fun mapInputErrors(
        height: Double?,
        currentWeight: Double?,
        goalWeight: Double?
    ): OnboardingErrorState {
        val fieldErrors = buildMap {
            if (height == null) {
                put(OnboardingField.HEIGHT, UiMessage("Enter a valid height.", isError = true))
            }
            if (currentWeight == null) {
                put(OnboardingField.CURRENT_WEIGHT, UiMessage("Enter a valid current weight.", isError = true))
            }
            if (goalWeight == null) {
                put(OnboardingField.GOAL_WEIGHT, UiMessage("Enter a valid goal weight.", isError = true))
            }
        }

        return OnboardingErrorState(fieldErrors = fieldErrors)
    }

    fun mapDomainError(error: Throwable): OnboardingErrorState {
        val validationError = error as? ProfileDomainError.Validation
            ?: return OnboardingErrorState(
                submitError = UiMessage(error.message ?: "Unknown error occurred", isError = true)
            )

        val fieldError = when (validationError.code) {
            "INVALID_HEIGHT" -> OnboardingField.HEIGHT to "Height must be greater than zero."
            "INVALID_CURRENT_WEIGHT" -> OnboardingField.CURRENT_WEIGHT to "Current weight must be greater than zero."
            "INVALID_GOAL_WEIGHT" -> OnboardingField.GOAL_WEIGHT to "Goal weight must be greater than zero."
            "GOAL_NOT_BELOW_CURRENT_WEIGHT" -> {
                OnboardingField.GOAL_WEIGHT to "Goal weight must be lower than current weight."
            }
            else -> null
        }

        return if (fieldError != null) {
            OnboardingErrorState(
                fieldErrors = mapOf(fieldError.first to UiMessage(fieldError.second, isError = true))
            )
        } else {
            OnboardingErrorState(
                submitError = UiMessage(validationError.detail, isError = true)
            )
        }
    }
}
