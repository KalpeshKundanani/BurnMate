package org.kalpeshbkundanani.burnmate.presentation.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.kalpeshbkundanani.burnmate.integration.model.FitPermissionState
import org.kalpeshbkundanani.burnmate.integration.model.GoogleAuthState
import org.kalpeshbkundanani.burnmate.integration.model.GoogleIntegrationAvailability
import org.kalpeshbkundanani.burnmate.presentation.integration.GoogleIntegrationPhase
import org.kalpeshbkundanani.burnmate.presentation.integration.GoogleIntegrationUiState
import org.kalpeshbkundanani.burnmate.profile.model.BmiCategory
import org.kalpeshbkundanani.burnmate.profile.model.BmiSnapshot
import org.kalpeshbkundanani.burnmate.profile.model.BodyMetrics
import org.kalpeshbkundanani.burnmate.profile.model.GoalValidationReason
import org.kalpeshbkundanani.burnmate.profile.model.GoalValidationResult
import org.kalpeshbkundanani.burnmate.profile.model.UserProfileSummary
import org.kalpeshbkundanani.burnmate.settings.export.AppExportCoordinator
import org.kalpeshbkundanani.burnmate.settings.export.AppExportSnapshot
import org.kalpeshbkundanani.burnmate.settings.preferences.AppPreferences
import org.kalpeshbkundanani.burnmate.settings.preferences.InMemoryAppPreferencesStore
import org.kalpeshbkundanani.burnmate.settings.reset.AppResetCoordinator
import org.kalpeshbkundanani.burnmate.settings.reset.AppResetResult
import org.kalpeshbkundanani.burnmate.settings.state.AppSessionState
import org.kalpeshbkundanani.burnmate.settings.state.InMemoryAppSessionStore

class SettingsViewModelTest {

    @Test
    fun `T-01 initializes from stored preferences and integration state`() {
        val preferencesStore = InMemoryAppPreferencesStore(AppPreferences(dailyTargetCalories = 2000))
        val integrationState = signedOutIntegrationState()
        val viewModel = SettingsViewModel(
            preferencesStore = preferencesStore,
            sessionStore = InMemoryAppSessionStore(AppSessionState(activeProfile = validProfileSummary())),
            exportCoordinator = FakeExportCoordinator(),
            resetCoordinator = FakeResetCoordinator(),
            integrationStateProvider = { integrationState },
            disconnectGoogle = { Result.success(Unit) },
            onResetCompleted = {}
        )

        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertEquals("2000", state.dailyTargetCalories)
        assertEquals(integrationState, state.integration)
        assertEquals("Google Fit is not connected.", state.integrationSummary.detail)
    }

    @Test
    fun `T-02 rejects blank daily target input`() {
        val preferencesStore = InMemoryAppPreferencesStore()
        val viewModel = SettingsViewModel(
            preferencesStore = preferencesStore,
            sessionStore = InMemoryAppSessionStore(),
            exportCoordinator = FakeExportCoordinator(),
            resetCoordinator = FakeResetCoordinator(),
            integrationStateProvider = ::signedOutIntegrationState,
            disconnectGoogle = { Result.success(Unit) },
            onResetCompleted = {}
        )

        viewModel.onEvent(SettingsEvent.DailyTargetChanged(""))
        viewModel.onEvent(SettingsEvent.SaveDailyTarget)

        assertEquals("Enter a valid positive calorie target.", viewModel.uiState.value.dailyTargetError?.message)
        assertEquals(2000, preferencesStore.read().dailyTargetCalories)
    }

    @Test
    fun `T-03 rejects non-positive daily target input`() {
        val preferencesStore = InMemoryAppPreferencesStore()
        val viewModel = SettingsViewModel(
            preferencesStore = preferencesStore,
            sessionStore = InMemoryAppSessionStore(),
            exportCoordinator = FakeExportCoordinator(),
            resetCoordinator = FakeResetCoordinator(),
            integrationStateProvider = ::signedOutIntegrationState,
            disconnectGoogle = { Result.success(Unit) },
            onResetCompleted = {}
        )

        viewModel.onEvent(SettingsEvent.DailyTargetChanged("0"))
        viewModel.onEvent(SettingsEvent.SaveDailyTarget)

        assertEquals("Enter a valid positive calorie target.", viewModel.uiState.value.dailyTargetError?.message)
        assertEquals(2000, preferencesStore.read().dailyTargetCalories)
    }

    @Test
    fun `T-04 persists valid daily target deterministically`() {
        val preferencesStore = InMemoryAppPreferencesStore()
        val viewModel = SettingsViewModel(
            preferencesStore = preferencesStore,
            sessionStore = InMemoryAppSessionStore(),
            exportCoordinator = FakeExportCoordinator(),
            resetCoordinator = FakeResetCoordinator(),
            integrationStateProvider = ::signedOutIntegrationState,
            disconnectGoogle = { Result.success(Unit) },
            onResetCompleted = {}
        )

        viewModel.onEvent(SettingsEvent.DailyTargetChanged("2300"))
        viewModel.onEvent(SettingsEvent.SaveDailyTarget)

        assertEquals(2300, preferencesStore.read().dailyTargetCalories)
        assertEquals("2300", viewModel.uiState.value.dailyTargetCalories)
        assertEquals("Daily target updated to 2300 kcal.", viewModel.uiState.value.message?.message)
    }

    @Test
    fun `T-07 reset requires explicit confirmation`() {
        var resetInvoked = false
        val viewModel = SettingsViewModel(
            preferencesStore = InMemoryAppPreferencesStore(),
            sessionStore = InMemoryAppSessionStore(),
            exportCoordinator = FakeExportCoordinator(),
            resetCoordinator = object : AppResetCoordinator {
                override suspend fun reset(): Result<AppResetResult> {
                    resetInvoked = true
                    return Result.success(AppResetResult(0, 0, false))
                }
            },
            integrationStateProvider = ::signedOutIntegrationState,
            disconnectGoogle = { Result.success(Unit) },
            onResetCompleted = {}
        )

        viewModel.onEvent(SettingsEvent.ResetTapped)

        assertEquals(SettingsConfirmationState.ResetAppData, viewModel.uiState.value.pendingConfirmation)
        assertFalse(resetInvoked)
    }

    @Test
    fun `T-08 confirmed reset clears app state and triggers completion`() {
        val preferencesStore = InMemoryAppPreferencesStore()
        preferencesStore.update { it.copy(dailyTargetCalories = 2300) }
        val sessionStore = InMemoryAppSessionStore()
        sessionStore.update { AppSessionState(activeProfile = validProfileSummary()) }
        var callbackInvoked = false
        val viewModel = SettingsViewModel(
            preferencesStore = preferencesStore,
            sessionStore = sessionStore,
            exportCoordinator = FakeExportCoordinator(),
            resetCoordinator = object : AppResetCoordinator {
                override suspend fun reset(): Result<AppResetResult> {
                    preferencesStore.reset()
                    sessionStore.reset()
                    return Result.success(AppResetResult(4, 2, true))
                }
            },
            integrationStateProvider = ::signedOutIntegrationState,
            disconnectGoogle = { Result.success(Unit) },
            onResetCompleted = { callbackInvoked = true }
        )

        viewModel.onEvent(SettingsEvent.ResetTapped)
        viewModel.onEvent(SettingsEvent.ConfirmReset)

        assertTrue(callbackInvoked)
        assertNull(sessionStore.read().activeProfile)
        assertEquals(2000, preferencesStore.read().dailyTargetCalories)
        assertEquals("2000", viewModel.uiState.value.dailyTargetCalories)
        assertNull(viewModel.uiState.value.pendingConfirmation)
    }

    @Test
    fun `T-09 disconnect delegates and refreshes integration state`() {
        var disconnectCalls = 0
        var signedOut = false
        val viewModel = SettingsViewModel(
            preferencesStore = InMemoryAppPreferencesStore(),
            sessionStore = InMemoryAppSessionStore(),
            exportCoordinator = FakeExportCoordinator(),
            resetCoordinator = FakeResetCoordinator(),
            integrationStateProvider = {
                if (signedOut) {
                    signedOutIntegrationState()
                } else {
                    GoogleIntegrationUiState(
                        phase = GoogleIntegrationPhase.SignedIn,
                        availability = GoogleIntegrationAvailability.Available,
                        authState = GoogleAuthState.SignedOut,
                        permissionState = FitPermissionState.Granted,
                        importAnchorDate = LocalDate(2026, 3, 17)
                    )
                }
            },
            disconnectGoogle = {
                disconnectCalls += 1
                signedOut = true
                Result.success(Unit)
            },
            onResetCompleted = {}
        )

        viewModel.onEvent(SettingsEvent.DisconnectGoogleTapped)

        assertEquals(1, disconnectCalls)
        assertEquals(GoogleIntegrationPhase.SignedOut, viewModel.uiState.value.integration?.phase)
        assertEquals("Google Fit disconnected.", viewModel.uiState.value.message?.message)
    }
}

private class FakeExportCoordinator(
    private val result: Result<AppExportSnapshot> = Result.success(
        AppExportSnapshot(
            exportedAt = Instant.parse("2026-03-17T10:00:00Z"),
            profile = null,
            preferences = AppPreferences(),
            calorieEntries = emptyList(),
            weightEntries = emptyList(),
            integrationSummary = "Signed out"
        )
    )
) : AppExportCoordinator {
    override suspend fun export(): Result<AppExportSnapshot> = result
}

private class FakeResetCoordinator : AppResetCoordinator {
    override suspend fun reset(): Result<AppResetResult> = Result.success(
        AppResetResult(
            clearedCalorieEntries = 0,
            clearedWeightEntries = 0,
            disconnectedIntegration = false
        )
    )
}

private fun signedOutIntegrationState(): GoogleIntegrationUiState {
    return GoogleIntegrationUiState(
        phase = GoogleIntegrationPhase.SignedOut,
        availability = GoogleIntegrationAvailability.Available,
        authState = GoogleAuthState.SignedOut,
        permissionState = FitPermissionState.Unknown,
        importAnchorDate = LocalDate(2026, 3, 17)
    )
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
