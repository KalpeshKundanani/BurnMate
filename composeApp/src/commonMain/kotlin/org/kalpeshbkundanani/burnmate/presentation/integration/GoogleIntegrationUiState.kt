package org.kalpeshbkundanani.burnmate.presentation.integration

import kotlinx.datetime.LocalDate
import org.kalpeshbkundanani.burnmate.integration.model.FitPermissionState
import org.kalpeshbkundanani.burnmate.integration.model.GoogleAuthState
import org.kalpeshbkundanani.burnmate.integration.model.GoogleFitSyncSummary
import org.kalpeshbkundanani.burnmate.integration.model.GoogleIntegrationAvailability
import org.kalpeshbkundanani.burnmate.presentation.shared.UiMessage

enum class GoogleIntegrationPhase {
    SignedOut,
    Authenticating,
    SignedIn,
    PermissionRequired,
    Syncing,
    Imported,
    Error,
    Unavailable
}

data class GoogleIntegrationUiState(
    val phase: GoogleIntegrationPhase = GoogleIntegrationPhase.SignedOut,
    val availability: GoogleIntegrationAvailability = GoogleIntegrationAvailability.Available,
    val authState: GoogleAuthState = GoogleAuthState.SignedOut,
    val permissionState: FitPermissionState = FitPermissionState.Unknown,
    val importAnchorDate: LocalDate,
    val importWindowDays: Int = 30,
    val syncSummary: GoogleFitSyncSummary? = null,
    val message: UiMessage? = null
)

sealed interface GoogleIntegrationEvent {
    data object Load : GoogleIntegrationEvent
    data object SignInClicked : GoogleIntegrationEvent
    data object GrantPermissionsClicked : GoogleIntegrationEvent
    data object RefreshImportClicked : GoogleIntegrationEvent
    data object DisconnectClicked : GoogleIntegrationEvent
    data object DismissMessage : GoogleIntegrationEvent
}
