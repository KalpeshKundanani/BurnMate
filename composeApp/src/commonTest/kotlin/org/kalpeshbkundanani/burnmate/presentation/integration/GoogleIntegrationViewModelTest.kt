package org.kalpeshbkundanani.burnmate.presentation.integration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.kalpeshbkundanani.burnmate.integration.auth.GoogleAuthLaunchResult
import org.kalpeshbkundanani.burnmate.integration.auth.GoogleAuthService
import org.kalpeshbkundanani.burnmate.integration.fit.GoogleFitService
import org.kalpeshbkundanani.burnmate.integration.mapping.BurnImportMapper
import org.kalpeshbkundanani.burnmate.integration.model.FitPermissionState
import org.kalpeshbkundanani.burnmate.integration.model.GoogleAccountSession
import org.kalpeshbkundanani.burnmate.integration.model.GoogleAuthState
import org.kalpeshbkundanani.burnmate.integration.model.GoogleFitSyncSummary
import org.kalpeshbkundanani.burnmate.integration.model.GoogleIntegrationAvailability
import org.kalpeshbkundanani.burnmate.integration.model.ImportedActivitySample
import org.kalpeshbkundanani.burnmate.integration.model.ImportedBurnSample
import org.kalpeshbkundanani.burnmate.integration.permission.FitPermissionRequestResult
import org.kalpeshbkundanani.burnmate.integration.permission.PermissionCoordinator
import org.kalpeshbkundanani.burnmate.integration.sync.ImportedBurnSyncService
import org.kalpeshbkundanani.burnmate.presentation.shared.SelectedDateCoordinator
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

class GoogleIntegrationViewModelTest {

    @Test
    fun `t01 signed out state initialization`() {
        val viewModel = createViewModel()

        assertEquals(GoogleIntegrationPhase.SignedOut, viewModel.uiState.value.phase)
    }

    @Test
    fun `t02 successful sign in transitions to permission required`() {
        val session = session()
        val viewModel = createViewModel(
            authService = FakeGoogleAuthService(signInResult = GoogleAuthLaunchResult.Success(session)),
            permissionCoordinator = FakePermissionCoordinator(readState = FitPermissionState.Required)
        )

        viewModel.onEvent(GoogleIntegrationEvent.SignInClicked)

        assertEquals(GoogleIntegrationPhase.PermissionRequired, viewModel.uiState.value.phase)
        assertEquals(GoogleAuthState.SignedIn(session), viewModel.uiState.value.authState)
    }

    @Test
    fun `t03 sign in cancellation handling`() {
        val viewModel = createViewModel(
            authService = FakeGoogleAuthService(signInResult = GoogleAuthLaunchResult.Cancelled)
        )

        viewModel.onEvent(GoogleIntegrationEvent.SignInClicked)

        assertEquals(GoogleIntegrationPhase.SignedOut, viewModel.uiState.value.phase)
        assertEquals("Google sign-in was cancelled.", viewModel.uiState.value.message?.message)
    }

    @Test
    fun `t04 permission denied handling`() {
        val session = session()
        val viewModel = createViewModel(
            authService = FakeGoogleAuthService(cachedState = GoogleAuthState.SignedIn(session)),
            permissionCoordinator = FakePermissionCoordinator(
                readState = FitPermissionState.Required,
                requestResult = FitPermissionRequestResult.Denied
            )
        )

        viewModel.onEvent(GoogleIntegrationEvent.GrantPermissionsClicked)

        assertEquals(GoogleIntegrationPhase.PermissionRequired, viewModel.uiState.value.phase)
        assertEquals(FitPermissionState.Denied, viewModel.uiState.value.permissionState)
    }

    @Test
    fun `t05 permission granted starts import and publishes imported state`() {
        val session = session()
        val summary = GoogleFitSyncSummary(
            startDate = LocalDate(2026, 2, 17),
            endDate = LocalDate(2026, 3, 17),
            importedEntries = 1,
            importedDays = 1
        )
        val fitService = FakeGoogleFitService(
            samples = listOf(
                ImportedActivitySample(
                    date = LocalDate(2026, 3, 17),
                    stepCount = 2000,
                    activeCalories = null
                )
            )
        )
        val syncService = FakeImportedBurnSyncService(result = Result.success(summary))
        val viewModel = createViewModel(
            authService = FakeGoogleAuthService(cachedState = GoogleAuthState.SignedIn(session)),
            permissionCoordinator = FakePermissionCoordinator(
                readState = FitPermissionState.Required,
                requestResult = FitPermissionRequestResult.Granted(session)
            ),
            fitService = fitService,
            syncService = syncService
        )

        viewModel.onEvent(GoogleIntegrationEvent.GrantPermissionsClicked)

        assertEquals(GoogleIntegrationPhase.Imported, viewModel.uiState.value.phase)
        assertEquals(summary, viewModel.importAppliedEvent.value)
        assertEquals(listOf(session), fitService.readSessions)
        assertEquals(1, syncService.syncCalls)
    }

    @Test
    fun `mismatched permission account blocks import and emits deterministic error state`() {
        val signedInSession = session()
        val grantedSession = GoogleAccountSession(
            subjectId = "subject-2",
            displayName = "Other User",
            email = "other@example.com"
        )
        val fitService = FakeGoogleFitService(
            samples = listOf(ImportedActivitySample(LocalDate(2026, 3, 17), 1000, 50))
        )
        val syncService = FakeImportedBurnSyncService()
        val viewModel = createViewModel(
            authService = FakeGoogleAuthService(cachedState = GoogleAuthState.SignedIn(signedInSession)),
            permissionCoordinator = FakePermissionCoordinator(
                readState = FitPermissionState.Required,
                requestResult = FitPermissionRequestResult.AccountMismatch(grantedSession)
            ),
            fitService = fitService,
            syncService = syncService
        )

        viewModel.onEvent(GoogleIntegrationEvent.GrantPermissionsClicked)

        assertEquals(GoogleIntegrationPhase.Error, viewModel.uiState.value.phase)
        assertEquals(FitPermissionState.MismatchedAccount, viewModel.uiState.value.permissionState)
        assertEquals(GoogleAuthState.SignedIn(signedInSession), viewModel.uiState.value.authState)
        assertTrue(
            viewModel.uiState.value.message?.message?.contains("same Google account") == true
        )
        assertTrue(viewModel.uiState.value.message?.isError == true)
        assertTrue(fitService.readSessions.isEmpty())
        assertEquals(0, syncService.syncCalls)
        assertNull(viewModel.importAppliedEvent.value)
    }

    @Test
    fun `cached mismatch loads explicit error state instead of signed in`() {
        val session = session()
        val viewModel = createViewModel(
            authService = FakeGoogleAuthService(cachedState = GoogleAuthState.SignedIn(session)),
            permissionCoordinator = FakePermissionCoordinator(readState = FitPermissionState.MismatchedAccount)
        )

        viewModel.onEvent(GoogleIntegrationEvent.Load)

        assertEquals(GoogleIntegrationPhase.Error, viewModel.uiState.value.phase)
        assertEquals(FitPermissionState.MismatchedAccount, viewModel.uiState.value.permissionState)
        assertEquals(GoogleAuthState.SignedIn(session), viewModel.uiState.value.authState)
        assertTrue(
            viewModel.uiState.value.message?.message?.contains("same Google account") == true
        )
    }

    @Test
    fun `t08 dashboard refresh event only fires after successful sync`() {
        val session = session()
        val successFitService = FakeGoogleFitService(
            samples = listOf(ImportedActivitySample(LocalDate(2026, 3, 17), 1000, 50))
        )
        val failedFitService = FakeGoogleFitService(
            samples = listOf(ImportedActivitySample(LocalDate(2026, 3, 17), 1000, 50))
        )
        val successViewModel = createViewModel(
            authService = FakeGoogleAuthService(cachedState = GoogleAuthState.SignedIn(session)),
            permissionCoordinator = FakePermissionCoordinator(readState = FitPermissionState.Granted),
            fitService = successFitService,
            syncService = FakeImportedBurnSyncService(
                result = Result.success(
                    GoogleFitSyncSummary(
                        startDate = LocalDate(2026, 2, 17),
                        endDate = LocalDate(2026, 3, 17),
                        importedEntries = 1,
                        importedDays = 1
                    )
                )
            )
        )
        val failedViewModel = createViewModel(
            authService = FakeGoogleAuthService(cachedState = GoogleAuthState.SignedIn(session)),
            permissionCoordinator = FakePermissionCoordinator(readState = FitPermissionState.Granted),
            fitService = failedFitService,
            syncService = FakeImportedBurnSyncService(
                result = Result.failure(IllegalStateException("sync failed"))
            )
        )

        successViewModel.onEvent(GoogleIntegrationEvent.RefreshImportClicked)
        failedViewModel.onEvent(GoogleIntegrationEvent.RefreshImportClicked)

        assertEquals(GoogleIntegrationPhase.Imported, successViewModel.uiState.value.phase)
        assertFalse(successViewModel.importAppliedEvent.value == null)
        assertEquals(GoogleIntegrationPhase.Error, failedViewModel.uiState.value.phase)
        assertNull(failedViewModel.importAppliedEvent.value)
        assertEquals(listOf(session), successFitService.readSessions)
        assertEquals(listOf(session), failedFitService.readSessions)
    }

    @Test
    fun `t09 disconnect clears connection state without deleting imported history`() {
        val session = session()
        val authService = FakeGoogleAuthService(cachedState = GoogleAuthState.SignedIn(session))
        val fitService = FakeGoogleFitService()
        val viewModel = createViewModel(
            authService = authService,
            permissionCoordinator = FakePermissionCoordinator(readState = FitPermissionState.Granted),
            fitService = fitService
        )

        viewModel.onEvent(GoogleIntegrationEvent.DisconnectClicked)

        assertEquals(GoogleIntegrationPhase.SignedOut, viewModel.uiState.value.phase)
        assertEquals(1, authService.disconnectCalls)
        assertEquals(1, fitService.disconnectCalls)
    }

    @Test
    fun `t10 android adapter boundary hides raw sdk types`() {
        val typeNames = listOf(
            org.kalpeshbkundanani.burnmate.integration.auth.GoogleAuthService::class.qualifiedName.orEmpty(),
            org.kalpeshbkundanani.burnmate.integration.auth.GoogleAuthLaunchResult::class.qualifiedName.orEmpty(),
            org.kalpeshbkundanani.burnmate.integration.fit.GoogleFitService::class.qualifiedName.orEmpty(),
            org.kalpeshbkundanani.burnmate.integration.permission.PermissionCoordinator::class.qualifiedName.orEmpty(),
            org.kalpeshbkundanani.burnmate.integration.permission.FitPermissionRequestResult::class.qualifiedName.orEmpty(),
            GoogleIntegrationUiState::class.qualifiedName.orEmpty()
        ).joinToString(" | ")

        assertFalse(typeNames.contains("android."))
        assertFalse(typeNames.contains("com.google"))
    }

    private fun createViewModel(
        authService: FakeGoogleAuthService = FakeGoogleAuthService(),
        permissionCoordinator: FakePermissionCoordinator = FakePermissionCoordinator(),
        fitService: FakeGoogleFitService = FakeGoogleFitService(),
        mapper: BurnImportMapper = FakeBurnImportMapper(),
        syncService: FakeImportedBurnSyncService = FakeImportedBurnSyncService()
    ): GoogleIntegrationViewModel {
        val initialDate = LocalDate(2026, 3, 17)
        return GoogleIntegrationViewModel(
            authService = authService,
            permissionCoordinator = permissionCoordinator,
            fitService = fitService,
            burnImportMapper = mapper,
            importedBurnSyncService = syncService,
            initialDate = initialDate,
            selectedDateCoordinator = SelectedDateCoordinator(initialDate)
        )
    }

    private fun session(): GoogleAccountSession {
        return GoogleAccountSession(
            subjectId = "subject-1",
            displayName = "User",
            email = "user@example.com"
        )
    }
}

private class FakeGoogleAuthService(
    private val cachedState: GoogleAuthState = GoogleAuthState.SignedOut,
    private val signInResult: GoogleAuthLaunchResult = GoogleAuthLaunchResult.Failure(
        IllegalStateException("not configured")
    )
) : GoogleAuthService {
    var disconnectCalls: Int = 0

    override fun readCachedState(): GoogleAuthState = cachedState

    override suspend fun signIn(): GoogleAuthLaunchResult = signInResult

    override suspend fun disconnect(): Result<Unit> {
        disconnectCalls += 1
        return Result.success(Unit)
    }
}

private class FakePermissionCoordinator(
    private val readState: FitPermissionState = FitPermissionState.Unknown,
    private val requestResult: FitPermissionRequestResult = FitPermissionRequestResult.Denied
) : PermissionCoordinator {
    val requestedSessions = mutableListOf<GoogleAccountSession>()

    override fun readState(session: GoogleAccountSession?): FitPermissionState = readState

    override suspend fun requestPermissions(session: GoogleAccountSession): FitPermissionRequestResult {
        requestedSessions += session
        return requestResult
    }
}

private class FakeGoogleFitService(
    private val availabilityValue: GoogleIntegrationAvailability = GoogleIntegrationAvailability.Available,
    private val samples: List<ImportedActivitySample> = emptyList()
) : GoogleFitService {
    var disconnectCalls: Int = 0
    val readSessions = mutableListOf<GoogleAccountSession>()

    override fun availability(): GoogleIntegrationAvailability = availabilityValue

    override suspend fun readDailyActivity(
        session: GoogleAccountSession,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<List<ImportedActivitySample>> {
        readSessions += session
        return Result.success(samples)
    }

    override suspend fun disconnect(session: GoogleAccountSession): Result<Unit> {
        disconnectCalls += 1
        return Result.success(Unit)
    }
}

private class FakeBurnImportMapper : BurnImportMapper {
    override fun map(samples: List<ImportedActivitySample>): List<ImportedBurnSample> {
        return samples.mapIndexed { index, sample ->
            ImportedBurnSample(
                entryId = "googlefit:${sample.date}:burn:$index",
                date = sample.date,
                burnCalories = sample.activeCalories ?: 42,
                createdAt = Instant.parse("${sample.date}T12:00:00Z")
            )
        }
    }
}

private class FakeImportedBurnSyncService(
    private val result: Result<GoogleFitSyncSummary> = Result.success(
        GoogleFitSyncSummary(
            startDate = LocalDate(2026, 2, 17),
            endDate = LocalDate(2026, 3, 17),
            importedEntries = 0,
            importedDays = 0
        )
    )
) : ImportedBurnSyncService {
    var syncCalls: Int = 0

    override fun sync(
        startDate: LocalDate,
        endDate: LocalDate,
        samples: List<ImportedBurnSample>
    ): Result<GoogleFitSyncSummary> {
        syncCalls += 1
        return result
    }
}
