package org.kalpeshbkundanani.burnmate.presentation.settings

import org.kalpeshbkundanani.burnmate.presentation.integration.GoogleIntegrationUiState
import org.kalpeshbkundanani.burnmate.presentation.shared.UiMessage

data class SettingsUiState(
    val isLoading: Boolean = true,
    val dailyTargetCalories: String = "",
    val dailyTargetError: UiMessage? = null,
    val integration: GoogleIntegrationUiState? = null,
    val integrationSummary: SettingsIntegrationSummary = SettingsIntegrationSummary(),
    val exportStatus: SettingsActionStatus = SettingsActionStatus.Idle,
    val exportPresentation: SettingsActionPresentation = SettingsActionPresentation(),
    val resetStatus: SettingsActionStatus = SettingsActionStatus.Idle,
    val resetPresentation: SettingsActionPresentation = SettingsActionPresentation(
        title = "Reset app data",
        detail = "Clear your profile, calorie logs, weight history, settings, and Google Fit access.",
        actionLabel = "RESET"
    ),
    val pendingConfirmation: SettingsConfirmationState? = null,
    val message: UiMessage? = null,
    val appInfo: String = "BurnMate 1.0"
)

sealed interface SettingsActionStatus {
    data object Idle : SettingsActionStatus
    data object InProgress : SettingsActionStatus
    data class Success(val detail: String) : SettingsActionStatus
    data class Failure(val detail: String) : SettingsActionStatus
}

sealed interface SettingsConfirmationState {
    data object ResetAppData : SettingsConfirmationState
}

data class SettingsIntegrationSummary(
    val title: String = "Google Fit",
    val detail: String = "Review your import status and disconnect access from here.",
    val actionLabel: String? = null
)

data class SettingsActionPresentation(
    val title: String = "",
    val detail: String = "",
    val actionLabel: String = "",
    val actionEnabled: Boolean = true,
    val isDanger: Boolean = false
)
