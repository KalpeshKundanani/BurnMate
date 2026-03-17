package org.kalpeshbkundanani.burnmate.presentation.integration

import androidx.lifecycle.ViewModel
import kotlin.math.max
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import org.kalpeshbkundanani.burnmate.integration.auth.GoogleAuthLaunchResult
import org.kalpeshbkundanani.burnmate.integration.auth.GoogleAuthService
import org.kalpeshbkundanani.burnmate.integration.fit.GoogleFitService
import org.kalpeshbkundanani.burnmate.integration.mapping.BurnImportMapper
import org.kalpeshbkundanani.burnmate.integration.model.FitPermissionState
import org.kalpeshbkundanani.burnmate.integration.model.GoogleAccountSession
import org.kalpeshbkundanani.burnmate.integration.model.GoogleAuthState
import org.kalpeshbkundanani.burnmate.integration.model.GoogleFitSyncSummary
import org.kalpeshbkundanani.burnmate.integration.model.GoogleIntegrationAvailability
import org.kalpeshbkundanani.burnmate.integration.model.GoogleIntegrationError
import org.kalpeshbkundanani.burnmate.integration.permission.FitPermissionRequestResult
import org.kalpeshbkundanani.burnmate.integration.permission.PermissionCoordinator
import org.kalpeshbkundanani.burnmate.integration.sync.ImportedBurnSyncService
import org.kalpeshbkundanani.burnmate.presentation.shared.SelectedDateCoordinator
import org.kalpeshbkundanani.burnmate.presentation.shared.UiMessage

class GoogleIntegrationViewModel(
    private val authService: GoogleAuthService,
    private val permissionCoordinator: PermissionCoordinator,
    private val fitService: GoogleFitService,
    private val burnImportMapper: BurnImportMapper,
    private val importedBurnSyncService: ImportedBurnSyncService,
    initialDate: LocalDate,
    private val selectedDateCoordinator: SelectedDateCoordinator
) : ViewModel() {
    private val stopObservingSelectedDate: () -> Unit

    private val _uiState = MutableStateFlow(GoogleIntegrationUiState(importAnchorDate = initialDate))
    val uiState: StateFlow<GoogleIntegrationUiState> = _uiState.asStateFlow()

    private val _importAppliedEvent = MutableStateFlow<GoogleFitSyncSummary?>(null)
    val importAppliedEvent: StateFlow<GoogleFitSyncSummary?> = _importAppliedEvent.asStateFlow()

    init {
        stopObservingSelectedDate = selectedDateCoordinator.observe { date ->
            _uiState.update { it.copy(importAnchorDate = date) }
            loadState(date)
        }
    }

    fun onEvent(event: GoogleIntegrationEvent) {
        when (event) {
            GoogleIntegrationEvent.Load -> loadState(_uiState.value.importAnchorDate)
            GoogleIntegrationEvent.SignInClicked -> runBlocking { signIn() }
            GoogleIntegrationEvent.GrantPermissionsClicked -> runBlocking { requestPermissions() }
            GoogleIntegrationEvent.RefreshImportClicked -> runBlocking { refreshImport() }
            GoogleIntegrationEvent.DisconnectClicked -> runBlocking { disconnect() }
            GoogleIntegrationEvent.DismissMessage -> _uiState.update { it.copy(message = null) }
        }
    }

    fun consumeImportAppliedEvent() {
        _importAppliedEvent.value = null
    }

    override fun onCleared() {
        stopObservingSelectedDate()
        super.onCleared()
    }

    private fun loadState(anchorDate: LocalDate) {
        val availability = fitService.availability()
        if (availability != GoogleIntegrationAvailability.Available) {
            _uiState.update {
                it.copy(
                    phase = GoogleIntegrationPhase.Unavailable,
                    availability = availability,
                    authState = GoogleAuthState.SignedOut,
                    permissionState = FitPermissionState.Unavailable,
                    importAnchorDate = anchorDate,
                    syncSummary = null,
                    message = UiMessage(availabilityMessage(availability), isError = true)
                )
            }
            return
        }

        when (val authState = authService.readCachedState()) {
            GoogleAuthState.SignedOut -> {
                _uiState.update {
                    it.copy(
                        phase = GoogleIntegrationPhase.SignedOut,
                        availability = availability,
                        authState = GoogleAuthState.SignedOut,
                        permissionState = FitPermissionState.Unknown,
                        importAnchorDate = anchorDate,
                        syncSummary = null
                    )
                }
            }
            GoogleAuthState.Authenticating -> {
                _uiState.update {
                    it.copy(
                        phase = GoogleIntegrationPhase.Authenticating,
                        availability = availability,
                        authState = authState,
                        importAnchorDate = anchorDate
                    )
                }
            }
            is GoogleAuthState.SignedIn -> {
                val permissionState = permissionCoordinator.readState(authState.session)
                _uiState.update {
                    it.copy(
                        phase = permissionPhase(permissionState),
                        availability = availability,
                        authState = authState,
                        permissionState = permissionState,
                        importAnchorDate = anchorDate
                    )
                }
            }
        }
    }

    private suspend fun signIn() {
        if (fitService.availability() != GoogleIntegrationAvailability.Available) {
            loadState(_uiState.value.importAnchorDate)
            return
        }

        _uiState.update {
            it.copy(
                phase = GoogleIntegrationPhase.Authenticating,
                authState = GoogleAuthState.Authenticating,
                message = null
            )
        }

        when (val result = authService.signIn()) {
            is GoogleAuthLaunchResult.Success -> {
                val permissionState = permissionCoordinator.readState(result.session)
                _uiState.update {
                    it.copy(
                        phase = permissionPhase(permissionState),
                        authState = GoogleAuthState.SignedIn(result.session),
                        permissionState = permissionState,
                        message = null
                    )
                }
                if (permissionState == FitPermissionState.Granted) {
                    importForSession(result.session)
                }
            }
            GoogleAuthLaunchResult.Cancelled -> {
                _uiState.update {
                    it.copy(
                        phase = GoogleIntegrationPhase.SignedOut,
                        authState = GoogleAuthState.SignedOut,
                        permissionState = FitPermissionState.Unknown,
                        message = UiMessage("Google sign-in was cancelled.")
                    )
                }
            }
            is GoogleAuthLaunchResult.Failure -> publishError(
                authState = GoogleAuthState.SignedOut,
                permissionState = FitPermissionState.Unknown,
                error = GoogleIntegrationError.SignInFailed(result.error.message ?: "Sign-in failed")
            )
        }
    }

    private suspend fun requestPermissions() {
        val session = signedInSession() ?: return
        _uiState.update {
            it.copy(
                phase = GoogleIntegrationPhase.PermissionRequired,
                permissionState = FitPermissionState.Requesting,
                message = null
            )
        }

        when (val result = permissionCoordinator.requestPermissions(session)) {
            FitPermissionRequestResult.Granted -> {
                _uiState.update {
                    it.copy(
                        phase = GoogleIntegrationPhase.SignedIn,
                        permissionState = FitPermissionState.Granted,
                        message = null
                    )
                }
                importForSession(session)
            }
            FitPermissionRequestResult.Denied,
            FitPermissionRequestResult.Cancelled -> {
                _uiState.update {
                    it.copy(
                        phase = GoogleIntegrationPhase.PermissionRequired,
                        permissionState = FitPermissionState.Denied,
                        message = UiMessage("Google Fit access is still required to import activity data.")
                    )
                }
            }
            is FitPermissionRequestResult.Failure -> publishError(
                authState = GoogleAuthState.SignedIn(session),
                permissionState = FitPermissionState.Denied,
                error = GoogleIntegrationError.ImportFailed(result.error.message ?: "Permission request failed")
            )
        }
    }

    private suspend fun refreshImport() {
        val session = signedInSession()
        if (session == null) {
            loadState(_uiState.value.importAnchorDate)
            return
        }

        if (_uiState.value.permissionState != FitPermissionState.Granted) {
            _uiState.update {
                it.copy(
                    phase = GoogleIntegrationPhase.PermissionRequired,
                    message = UiMessage("Google Fit permission is required before import.")
                )
            }
            return
        }

        importForSession(session)
    }

    private suspend fun disconnect() {
        val session = signedInSession()
        session?.let { fitService.disconnect(it) }
        authService.disconnect()
        _uiState.update {
            it.copy(
                phase = GoogleIntegrationPhase.SignedOut,
                authState = GoogleAuthState.SignedOut,
                permissionState = FitPermissionState.Unknown,
                syncSummary = null,
                message = UiMessage("Google Fit import disconnected.")
            )
        }
    }

    private suspend fun importForSession(session: GoogleAccountSession) {
        val anchorDate = _uiState.value.importAnchorDate
        val startDate = anchorDate.minus(max(_uiState.value.importWindowDays - 1, 0), DateTimeUnit.DAY)

        _uiState.update {
            it.copy(
                phase = GoogleIntegrationPhase.Syncing,
                authState = GoogleAuthState.SignedIn(session),
                permissionState = FitPermissionState.Granted,
                message = null
            )
        }

        val activitySamples = fitService
            .readDailyActivity(session, startDate, anchorDate)
            .getOrElse {
                publishError(
                    authState = GoogleAuthState.SignedIn(session),
                    permissionState = FitPermissionState.Granted,
                    error = GoogleIntegrationError.ImportFailed(it.message ?: "Import failed")
                )
                return
            }

        val burnSamples = try {
            burnImportMapper.map(activitySamples)
        } catch (error: Throwable) {
            publishError(
                authState = GoogleAuthState.SignedIn(session),
                permissionState = FitPermissionState.Granted,
                error = GoogleIntegrationError.ImportFailed(error.message ?: "Mapping failed")
            )
            return
        }

        val syncSummary = importedBurnSyncService
            .sync(startDate, anchorDate, burnSamples)
            .getOrElse {
                publishError(
                    authState = GoogleAuthState.SignedIn(session),
                    permissionState = FitPermissionState.Granted,
                    error = if (it is GoogleIntegrationError) it else GoogleIntegrationError.SyncFailed(it.message ?: "Sync failed")
                )
                return
            }

        _importAppliedEvent.value = syncSummary
        _uiState.update {
            it.copy(
                phase = GoogleIntegrationPhase.Imported,
                authState = GoogleAuthState.SignedIn(session),
                permissionState = FitPermissionState.Granted,
                syncSummary = syncSummary,
                message = if (syncSummary.importedEntries == 0) {
                    UiMessage("No Google Fit burn data was available for this window.")
                } else {
                    UiMessage("Imported ${syncSummary.importedEntries} Google Fit burn entries.")
                }
            )
        }
    }

    private fun permissionPhase(permissionState: FitPermissionState): GoogleIntegrationPhase {
        return when (permissionState) {
            FitPermissionState.Granted -> GoogleIntegrationPhase.SignedIn
            FitPermissionState.Required,
            FitPermissionState.Requesting,
            FitPermissionState.Denied -> GoogleIntegrationPhase.PermissionRequired
            FitPermissionState.Unavailable -> GoogleIntegrationPhase.Unavailable
            FitPermissionState.Unknown -> GoogleIntegrationPhase.SignedOut
        }
    }

    private fun signedInSession(): GoogleAccountSession? {
        val authState = _uiState.value.authState
        return if (authState is GoogleAuthState.SignedIn) authState.session else null
    }

    private fun publishError(
        authState: GoogleAuthState,
        permissionState: FitPermissionState,
        error: GoogleIntegrationError
    ) {
        _uiState.update {
            it.copy(
                phase = if (error == GoogleIntegrationError.Unavailable) {
                    GoogleIntegrationPhase.Unavailable
                } else {
                    GoogleIntegrationPhase.Error
                },
                authState = authState,
                permissionState = permissionState,
                message = UiMessage(error.message ?: "Google integration failed.", isError = true)
            )
        }
    }

    private fun availabilityMessage(availability: GoogleIntegrationAvailability): String {
        return when (availability) {
            GoogleIntegrationAvailability.Available -> "Google Fit is ready."
            GoogleIntegrationAvailability.UnsupportedPlatform -> "Google Fit import is only available on Android."
            GoogleIntegrationAvailability.ConfigurationMissing -> "Google Sign-In is not configured for this build."
            GoogleIntegrationAvailability.FitProjectUnavailable -> "This build is not connected to an approved Google Fit project."
        }
    }
}
