package org.kalpeshbkundanani.burnmate.presentation.onboarding

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.kalpeshbkundanani.burnmate.presentation.shared.UiMessage
import org.kalpeshbkundanani.burnmate.profile.domain.UserProfileFactory
import org.kalpeshbkundanani.burnmate.profile.model.BodyMetrics
import org.kalpeshbkundanani.burnmate.profile.model.ProfileDomainError

class OnboardingViewModel(
    private val profileFactory: UserProfileFactory
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onEvent(event: OnboardingEvent) {
        when (event) {
            is OnboardingEvent.HeightChanged -> {
                _uiState.update { 
                    it.copy(
                        heightInput = event.value,
                        fieldErrors = it.fieldErrors - OnboardingField.HEIGHT,
                        submitError = null
                    ).validateEnableSubmit()
                }
            }
            is OnboardingEvent.CurrentWeightChanged -> {
                _uiState.update { 
                    it.copy(
                        currentWeightInput = event.value,
                        fieldErrors = it.fieldErrors - OnboardingField.CURRENT_WEIGHT,
                        submitError = null
                    ).validateEnableSubmit()
                }
            }
            is OnboardingEvent.GoalWeightChanged -> {
                _uiState.update { 
                    it.copy(
                        goalWeightInput = event.value,
                        fieldErrors = it.fieldErrors - OnboardingField.GOAL_WEIGHT,
                        submitError = null
                    ).validateEnableSubmit()
                }
            }
            OnboardingEvent.Submit -> submitProfile()
            OnboardingEvent.DismissError -> {
                _uiState.update { it.copy(submitError = null) }
            }
        }
    }

    private fun OnboardingUiState.validateEnableSubmit(): OnboardingUiState {
        val isValid = heightInput.isNotBlank() && currentWeightInput.isNotBlank() && goalWeightInput.isNotBlank()
        return copy(isSubmitEnabled = isValid)
    }

    private fun submitProfile() {
        val state = _uiState.value
        _uiState.update { it.copy(isSubmitting = true, fieldErrors = emptyMap(), submitError = null) }

        val height = state.heightInput.toDoubleOrNull()
        val current = state.currentWeightInput.toDoubleOrNull()
        val goal = state.goalWeightInput.toDoubleOrNull()

        if (height == null || current == null || goal == null) {
            _uiState.update {
                it.copy(
                    isSubmitting = false,
                    submitError = UiMessage("Please enter valid decimal numbers.", isError = true)
                )
            }
            return
        }

        val result = profileFactory.create(BodyMetrics(
            heightCm = height,
            currentWeightKg = current,
            goalWeightKg = goal
        ))

        result.fold(
            onSuccess = {
                // In a real app we would save it. For SLICE-0007, we just succeed.
                _uiState.update { it.copy(isSubmitting = false) }
            },
            onFailure = { error ->
                var genericMsg: String? = null
                
                if (error is ProfileDomainError.Validation) {
                    genericMsg = error.detail
                } else {
                    genericMsg = error.message ?: "Unknown error occurred"
                }

                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        submitError = genericMsg.let { msg -> UiMessage(msg, true) }
                    )
                }
            }
        )
    }
}
