package org.kalpeshbkundanani.burnmate.presentation.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.kalpeshbkundanani.burnmate.logging.model.CalorieAmount
import org.kalpeshbkundanani.burnmate.logging.model.CalorieEntry
import org.kalpeshbkundanani.burnmate.logging.model.EntryDate
import org.kalpeshbkundanani.burnmate.logging.model.EntryId
import org.kalpeshbkundanani.burnmate.logging.repository.EntryRepository
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
import org.kalpeshbkundanani.burnmate.settings.export.AppExportLauncher
import org.kalpeshbkundanani.burnmate.settings.export.DefaultAppExportCoordinator
import org.kalpeshbkundanani.burnmate.settings.preferences.AppPreferences
import org.kalpeshbkundanani.burnmate.settings.preferences.InMemoryAppPreferencesStore
import org.kalpeshbkundanani.burnmate.settings.reset.AppResetCoordinator
import org.kalpeshbkundanani.burnmate.settings.reset.AppResetResult
import org.kalpeshbkundanani.burnmate.settings.state.AppSessionState
import org.kalpeshbkundanani.burnmate.settings.state.InMemoryAppSessionStore
import org.kalpeshbkundanani.burnmate.weight.model.WeightEntry
import org.kalpeshbkundanani.burnmate.weight.model.WeightValue
import org.kalpeshbkundanani.burnmate.weight.repository.WeightHistoryRepository

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
    fun `T-06 export failure sets failure state without mutating app state`() {
        val preferencesStore = InMemoryAppPreferencesStore(AppPreferences(dailyTargetCalories = 2100))
        val profile = validProfileSummary()
        val sessionStore = InMemoryAppSessionStore(AppSessionState(activeProfile = profile))
        val calorieEntries = listOf(
            CalorieEntry(
                id = EntryId("entry-1"),
                date = EntryDate(LocalDate(2026, 3, 16)),
                amount = CalorieAmount(300),
                createdAt = Instant.parse("2026-03-16T10:00:00Z")
            )
        )
        val weightEntries = listOf(
            WeightEntry(
                date = LocalDate(2026, 3, 17),
                weight = WeightValue(80.0),
                createdAt = Instant.parse("2026-03-17T09:00:00Z")
            )
        )
        val entryRepository = FixedEntryRepository(calorieEntries)
        val weightRepository = FixedWeightHistoryRepository(weightEntries)
        val viewModel = SettingsViewModel(
            preferencesStore = preferencesStore,
            sessionStore = sessionStore,
            exportCoordinator = DefaultAppExportCoordinator(
                sessionStore = sessionStore,
                preferencesStore = preferencesStore,
                entryRepository = entryRepository,
                weightRepository = weightRepository,
                integrationStatusProvider = { "Signed out" },
                exportLauncher = object : AppExportLauncher {
                    override suspend fun launch(snapshot: AppExportSnapshot): Result<Unit> {
                        return Result.failure(IllegalStateException("launcher failed"))
                    }
                },
                nowProvider = { Instant.parse("2026-03-17T10:00:00Z") },
                entryDateRangeProvider = { EntryDate(LocalDate(2026, 1, 1)) to EntryDate(LocalDate(2026, 12, 31)) }
            ),
            resetCoordinator = FakeResetCoordinator(),
            integrationStateProvider = ::signedOutIntegrationState,
            disconnectGoogle = { Result.success(Unit) },
            onResetCompleted = {}
        )

        viewModel.onEvent(SettingsEvent.ExportTapped)

        assertEquals(SettingsActionStatus.Failure("Failed to hand off export"), viewModel.uiState.value.exportStatus)
        assertEquals("Export failed", viewModel.uiState.value.exportPresentation.title)
        assertEquals("Failed to hand off export", viewModel.uiState.value.message?.message)
        assertTrue(viewModel.uiState.value.message?.isError == true)
        assertEquals(2100, preferencesStore.read().dailyTargetCalories)
        assertEquals(profile, sessionStore.read().activeProfile)
        assertEquals(calorieEntries, entryRepository.fetchByDateRange(EntryDate(LocalDate(2026, 1, 1)), EntryDate(LocalDate(2026, 12, 31))).getOrThrow())
        assertEquals(weightEntries, weightRepository.getAll().getOrThrow())
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

private class FixedEntryRepository(
    private val entries: List<CalorieEntry>
) : EntryRepository {
    override fun create(entry: CalorieEntry): Result<CalorieEntry> = Result.success(entry)
    override fun deleteById(id: EntryId): Result<Boolean> = Result.success(true)
    override fun fetchByDateRange(startDate: EntryDate, endDate: EntryDate): Result<List<CalorieEntry>> = Result.success(entries)
    override fun fetchByDate(date: EntryDate): Result<List<CalorieEntry>> = Result.success(entries.filter { it.date == date })
}

private class FixedWeightHistoryRepository(
    private val entries: List<WeightEntry>
) : WeightHistoryRepository {
    override fun save(entry: WeightEntry): Result<WeightEntry> = Result.success(entry)
    override fun update(entry: WeightEntry): Result<WeightEntry> = Result.success(entry)
    override fun deleteByDate(date: LocalDate): Result<Boolean> = Result.success(true)
    override fun getByDate(date: LocalDate): Result<WeightEntry?> = Result.success(entries.find { it.date == date })
    override fun getByDateRange(startDate: LocalDate, endDate: LocalDate): Result<List<WeightEntry>> = Result.success(entries)
    override fun getAll(): Result<List<WeightEntry>> = Result.success(entries)
}
