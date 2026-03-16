package org.kalpeshbkundanani.burnmate.presentation.onboarding

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.kalpeshbkundanani.burnmate.profile.domain.UserProfileFactory
import org.kalpeshbkundanani.burnmate.profile.model.BmiCategory
import org.kalpeshbkundanani.burnmate.profile.model.BmiSnapshot
import org.kalpeshbkundanani.burnmate.profile.model.BodyMetrics
import org.kalpeshbkundanani.burnmate.profile.model.GoalValidationReason
import org.kalpeshbkundanani.burnmate.profile.model.GoalValidationResult
import org.kalpeshbkundanani.burnmate.profile.model.ProfileDomainError
import org.kalpeshbkundanani.burnmate.profile.model.UserProfileSummary

class OnboardingViewModelTest {

    @Test
    fun `t01 initializes with empty form and disabled submit`() {
        val viewModel = OnboardingViewModel(profileFactory = FakeUserProfileFactory())

        val state = viewModel.uiState.value

        assertEquals("", state.heightInput)
        assertEquals("", state.currentWeightInput)
        assertEquals("", state.goalWeightInput)
        assertTrue(state.fieldErrors.isEmpty())
        assertNull(state.submitError)
        assertFalse(state.isSubmitEnabled)
        assertNull(viewModel.successEvent.value)
    }

    @Test
    fun `t02 maps parsing failures to field errors instead of submit error`() {
        val viewModel = OnboardingViewModel(profileFactory = FakeUserProfileFactory())

        viewModel.onEvent(OnboardingEvent.HeightChanged("abc"))
        viewModel.onEvent(OnboardingEvent.CurrentWeightChanged("80"))
        viewModel.onEvent(OnboardingEvent.GoalWeightChanged("goal"))
        viewModel.onEvent(OnboardingEvent.Submit)

        val state = viewModel.uiState.value

        assertEquals("Enter a valid height.", state.fieldErrors[OnboardingField.HEIGHT]?.message)
        assertEquals("Enter a valid goal weight.", state.fieldErrors[OnboardingField.GOAL_WEIGHT]?.message)
        assertNull(state.fieldErrors[OnboardingField.CURRENT_WEIGHT])
        assertNull(state.submitError)
        assertNull(viewModel.successEvent.value)
    }

    @Test
    fun `maps profile validation failures to field errors and emits one-shot success on valid submit`() {
        val failingFactory = FakeUserProfileFactory(
            nextResult = Result.failure(
                ProfileDomainError.Validation(
                    code = "GOAL_NOT_BELOW_CURRENT_WEIGHT",
                    detail = "goalWeightKg must be lower than currentWeightKg"
                )
            )
        )
        val failingViewModel = OnboardingViewModel(profileFactory = failingFactory)

        failingViewModel.onEvent(OnboardingEvent.HeightChanged("175"))
        failingViewModel.onEvent(OnboardingEvent.CurrentWeightChanged("90"))
        failingViewModel.onEvent(OnboardingEvent.GoalWeightChanged("95"))
        failingViewModel.onEvent(OnboardingEvent.Submit)

        val failedState = failingViewModel.uiState.value
        assertEquals(
            "Goal weight must be lower than current weight.",
            failedState.fieldErrors[OnboardingField.GOAL_WEIGHT]?.message
        )
        assertNull(failedState.submitError)
        assertNull(failingViewModel.successEvent.value)

        val successViewModel = OnboardingViewModel(
            profileFactory = FakeUserProfileFactory(nextResult = Result.success(validProfileSummary()))
        )
        successViewModel.onEvent(OnboardingEvent.HeightChanged("175"))
        successViewModel.onEvent(OnboardingEvent.CurrentWeightChanged("90"))
        successViewModel.onEvent(OnboardingEvent.GoalWeightChanged("70"))
        successViewModel.onEvent(OnboardingEvent.Submit)

        val successEvent = successViewModel.successEvent.value
        assertNotNull(successEvent)
        assertEquals(validProfileSummary(), successEvent.profileSummary)
        successViewModel.consumeSuccessEvent(successEvent.eventId)
        assertNull(successViewModel.successEvent.value)
    }
}

private class FakeUserProfileFactory(
    private val nextResult: Result<UserProfileSummary> = Result.success(validProfileSummary())
) : UserProfileFactory {
    override fun create(metrics: BodyMetrics): Result<UserProfileSummary> = nextResult
}

private fun validProfileSummary(): UserProfileSummary {
    return UserProfileSummary(
        metrics = BodyMetrics(
            heightCm = 175.0,
            currentWeightKg = 90.0,
            goalWeightKg = 70.0
        ),
        currentBmi = BmiSnapshot(29.4, BmiCategory.OVERWEIGHT),
        goalBmi = BmiSnapshot(22.9, BmiCategory.HEALTHY),
        kilogramsToLose = 20.0,
        bmiDelta = 6.5,
        goalValidation = GoalValidationResult(
            isValid = true,
            reason = GoalValidationReason.VALID,
            kilogramsToLose = 20.0,
            bmiDelta = 6.5
        )
    )
}
