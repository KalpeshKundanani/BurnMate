package org.kalpeshbkundanani.burnmate.presentation.settings

import org.kalpeshbkundanani.burnmate.integration.model.GoogleAuthState
import org.kalpeshbkundanani.burnmate.presentation.integration.GoogleIntegrationPhase
import org.kalpeshbkundanani.burnmate.presentation.integration.GoogleIntegrationUiState

class SettingsStateMapper {

    fun integrationSummary(integration: GoogleIntegrationUiState?): SettingsIntegrationSummary {
        if (integration == null) {
            return SettingsIntegrationSummary(
                detail = "Google Fit status is unavailable."
            )
        }

        return when (integration.phase) {
            GoogleIntegrationPhase.SignedOut -> SettingsIntegrationSummary(
                detail = "Google Fit is not connected.",
                actionLabel = null
            )
            GoogleIntegrationPhase.Authenticating -> SettingsIntegrationSummary(
                detail = "Google account connection is in progress.",
                actionLabel = null
            )
            GoogleIntegrationPhase.PermissionRequired -> SettingsIntegrationSummary(
                title = signedInTitle(integration.authState),
                detail = "Google Fit access still needs permission approval.",
                actionLabel = "DISCONNECT"
            )
            GoogleIntegrationPhase.Syncing -> SettingsIntegrationSummary(
                title = signedInTitle(integration.authState),
                detail = "BurnMate is syncing Google Fit data.",
                actionLabel = null
            )
            GoogleIntegrationPhase.SignedIn -> SettingsIntegrationSummary(
                title = signedInTitle(integration.authState),
                detail = integration.syncSummary?.let {
                    "Imported ${it.importedEntries} entries from ${it.startDate} to ${it.endDate}."
                } ?: "Google Fit is connected and ready to sync.",
                actionLabel = "DISCONNECT"
            )
            GoogleIntegrationPhase.Imported -> SettingsIntegrationSummary(
                title = signedInTitle(integration.authState),
                detail = integration.syncSummary?.let {
                    "Last import covered ${it.importedDays} day(s) and ${it.importedEntries} entries."
                } ?: "Google Fit import completed.",
                actionLabel = "DISCONNECT"
            )
            GoogleIntegrationPhase.Error -> SettingsIntegrationSummary(
                title = "Google Fit needs attention",
                detail = integration.message?.message ?: "Retry from the dashboard or disconnect from settings.",
                actionLabel = disconnectLabel(integration.authState)
            )
            GoogleIntegrationPhase.Unavailable -> SettingsIntegrationSummary(
                detail = integration.message?.message ?: "Google Fit is unavailable on this device.",
                actionLabel = null
            )
        }
    }

    fun exportPresentation(status: SettingsActionStatus): SettingsActionPresentation {
        return when (status) {
            SettingsActionStatus.Idle -> SettingsActionPresentation(
                title = "Export your data",
                detail = "Create a deterministic export of your profile, settings, calorie logs, weight history, and integration summary.",
                actionLabel = "EXPORT"
            )
            SettingsActionStatus.InProgress -> SettingsActionPresentation(
                title = "Exporting data",
                detail = "Building your export snapshot and handing it off to the system.",
                actionLabel = "EXPORTING",
                actionEnabled = false
            )
            is SettingsActionStatus.Success -> SettingsActionPresentation(
                title = "Export complete",
                detail = status.detail,
                actionLabel = "EXPORT AGAIN"
            )
            is SettingsActionStatus.Failure -> SettingsActionPresentation(
                title = "Export failed",
                detail = status.detail,
                actionLabel = "RETRY EXPORT"
            )
        }
    }

    fun resetPresentation(status: SettingsActionStatus): SettingsActionPresentation {
        return when (status) {
            SettingsActionStatus.Idle -> SettingsActionPresentation(
                title = "Reset app data",
                detail = "Clear your profile, calorie logs, weight history, daily target, and Google Fit access.",
                actionLabel = "RESET",
                isDanger = true
            )
            SettingsActionStatus.InProgress -> SettingsActionPresentation(
                title = "Resetting app data",
                detail = "Clearing all in-scope BurnMate data.",
                actionLabel = "RESETTING",
                actionEnabled = false,
                isDanger = true
            )
            is SettingsActionStatus.Success -> SettingsActionPresentation(
                title = "Reset complete",
                detail = status.detail,
                actionLabel = "RESET",
                isDanger = true
            )
            is SettingsActionStatus.Failure -> SettingsActionPresentation(
                title = "Reset failed",
                detail = status.detail,
                actionLabel = "TRY AGAIN",
                isDanger = true
            )
        }
    }

    private fun signedInTitle(authState: GoogleAuthState): String {
        val session = (authState as? GoogleAuthState.SignedIn)?.session
        return session?.displayName ?: session?.email ?: "Google Fit connected"
    }

    private fun disconnectLabel(authState: GoogleAuthState): String? {
        return if (authState is GoogleAuthState.SignedIn) "DISCONNECT" else null
    }
}
