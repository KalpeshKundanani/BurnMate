package org.kalpeshbkundanani.burnmate.presentation.onboarding

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.kalpeshbkundanani.burnmate.presentation.shared.UiMessage
import org.kalpeshbkundanani.burnmate.profile.domain.UserProfileFactory
import org.kalpeshbkundanani.burnmate.profile.model.BodyMetrics

class OnboardingViewModel(
    private val profileFactory: UserProfileFactory,
    private val errorMapper: OnboardingErrorMapper = OnboardingErrorMapper()
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()
    private val _successEvent = MutableStateFlow<OnboardingSuccessEvent?>(null)
    val successEvent: StateFlow<OnboardingSuccessEvent?> = _successEvent.asStateFlow()
    private var nextSuccessEventId: Long = 0L

    fun onEvent(event: OnboardingEvent) {
        when (event) {
            is OnboardingEvent.HeightChanged -> {
                _uiState.update {
                    it.copy(
                        heightInput = event.value,
                        fieldErrors = it.fieldErrors - OnboardingField.HEIGHT,
                        submitError = null
                    ).refreshGoalWeightSuggestion().validateEnableSubmit()
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
                    ).refreshGoalWeightSuggestion().validateEnableSubmit()
                }
            }
            OnboardingEvent.Submit -> submitProfile()
            OnboardingEvent.DismissError -> {
                _uiState.update { it.copy(submitError = null) }
            }
        }
    }

    fun consumeSuccessEvent(eventId: Long) {
        if (_successEvent.value?.eventId == eventId) {
            _successEvent.value = null
        }
    }

    private fun OnboardingUiState.validateEnableSubmit(): OnboardingUiState {
        val isValid = heightInput.isNotBlank() && currentWeightInput.isNotBlank() && goalWeightInput.isNotBlank()
        return copy(isSubmitEnabled = isValid)
    }

    private fun OnboardingUiState.refreshGoalWeightSuggestion(): OnboardingUiState {
        val heightCm = heightInput.toDoubleOrNull()
        if (heightCm == null || heightCm <= 0.0) {
            return copy(goalWeightSuggestion = null)
        }

        val heightMeters = heightCm / 100.0
        val lowerBound = (18.5 * heightMeters * heightMeters).roundToSingleDecimal()
        val upperBound = (24.9 * heightMeters * heightMeters).roundToSingleDecimal()
        return copy(
            goalWeightSuggestion = UiMessage(
                "Healthy BMI range for your height: $lowerBound-$upperBound kg."
            )
        )
    }

    private fun Double.roundToSingleDecimal(): String {
        return (kotlin.math.round(this * 10.0) / 10.0).toString()
    }

    private fun submitProfile() {
        val state = _uiState.value
        _uiState.update { it.copy(isSubmitting = true, fieldErrors = emptyMap(), submitError = null) }

        val height = state.heightInput.toDoubleOrNull()
        val current = state.currentWeightInput.toDoubleOrNull()
        val goal = state.goalWeightInput.toDoubleOrNull()

        val inputErrorState = errorMapper.mapInputErrors(height, current, goal)
        if (inputErrorState.fieldErrors.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    isSubmitting = false,
                    fieldErrors = inputErrorState.fieldErrors,
                    submitError = inputErrorState.submitError
                )
            }
            return
        }

        val validHeight = requireNotNull(height)
        val validCurrentWeight = requireNotNull(current)
        val validGoalWeight = requireNotNull(goal)

        val result = profileFactory.create(BodyMetrics(
            heightCm = validHeight,
            currentWeightKg = validCurrentWeight,
            goalWeightKg = validGoalWeight
        ))

        result.fold(
            onSuccess = { summary ->
                _uiState.update { it.copy(isSubmitting = false, fieldErrors = emptyMap(), submitError = null) }
                nextSuccessEventId += 1
                _successEvent.value = OnboardingSuccessEvent(
                    eventId = nextSuccessEventId,
                    profileSummary = summary
                )
            },
            onFailure = { error ->
                val errorState = errorMapper.mapDomainError(error)
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        fieldErrors = errorState.fieldErrors,
                        submitError = errorState.submitError
                    )
                }
            }
        )
    }
}
