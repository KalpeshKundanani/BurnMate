package org.kalpeshbkundanani.burnmate.ui.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import org.kalpeshbkundanani.burnmate.presentation.onboarding.OnboardingSuccessEvent
import org.kalpeshbkundanani.burnmate.profile.model.BmiCategory
import org.kalpeshbkundanani.burnmate.profile.model.BmiSnapshot
import org.kalpeshbkundanani.burnmate.profile.model.BodyMetrics
import org.kalpeshbkundanani.burnmate.profile.model.GoalValidationReason
import org.kalpeshbkundanani.burnmate.profile.model.GoalValidationResult
import org.kalpeshbkundanani.burnmate.profile.model.UserProfileSummary
import org.kalpeshbkundanani.burnmate.ui.organisms.NavigationTab

class BurnMateNavigationHostTest {

    @Test
    fun `coordinator keeps onboarding start destination until onboarding success`() {
        val initialCoordinator = BurnMateNavigationCoordinator()

        assertEquals(BurnMateRoute.Onboarding, initialCoordinator.startDestination())

        val updatedCoordinator = initialCoordinator.applyOnboardingSuccess(
            OnboardingSuccessEvent(
                eventId = 1L,
                profileSummary = validProfileSummary()
            )
        )

        assertEquals(BurnMateRoute.Dashboard, updatedCoordinator.startDestination())
    }

    @Test
    fun `null onboarding success does not trigger navigation state change`() {
        val coordinator = BurnMateNavigationCoordinator()

        assertEquals(coordinator, coordinator.applyOnboardingSuccess(null))
    }

    @Test
    fun `bottom nav is limited to dashboard and daily logging routes`() {
        val coordinator = BurnMateNavigationCoordinator(activeProfile = validProfileSummary())

        assertEquals(BurnMateRoute.DailyLogging, coordinator.routeForTab(BurnMateRoute.Dashboard, NavigationTab.ACTIVITY))
        assertEquals(BurnMateRoute.Dashboard, coordinator.routeForTab(BurnMateRoute.DailyLogging, NavigationTab.HOME))
        assertEquals(null, coordinator.routeForTab(BurnMateRoute.Dashboard, NavigationTab.HOME))
        assertEquals(null, coordinator.routeForTab(BurnMateRoute.DailyLogging, NavigationTab.ACTIVITY))
    }
}

private fun validProfileSummary(): UserProfileSummary {
    return UserProfileSummary(
        metrics = BodyMetrics(175.0, 90.0, 70.0),
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
