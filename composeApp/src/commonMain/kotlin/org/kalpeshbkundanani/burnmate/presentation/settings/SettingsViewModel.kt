package org.kalpeshbkundanani.burnmate.presentation.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import org.kalpeshbkundanani.burnmate.presentation.integration.GoogleIntegrationUiState
import org.kalpeshbkundanani.burnmate.presentation.shared.UiMessage
import org.kalpeshbkundanani.burnmate.settings.export.AppExportCoordinator
import org.kalpeshbkundanani.burnmate.settings.preferences.AppPreferencesStore
import org.kalpeshbkundanani.burnmate.settings.reset.AppResetCoordinator
import org.kalpeshbkundanani.burnmate.settings.state.AppSessionStore

class SettingsViewModel(
    private val preferencesStore: AppPreferencesStore,
    private val sessionStore: AppSessionStore,
    private val exportCoordinator: AppExportCoordinator,
    private val resetCoordinator: AppResetCoordinator,
    private val integrationStateProvider: () -> GoogleIntegrationUiState,
    private val disconnectGoogle: suspend () -> Result<Unit>,
    private val onResetCompleted: () -> Unit,
    private val stateMapper: SettingsStateMapper = SettingsStateMapper()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            SettingsEvent.Load -> load()
            is SettingsEvent.DailyTargetChanged -> {
                _uiState.update {
                    it.copy(
                        dailyTargetCalories = event.value,
                        dailyTargetError = null,
                        message = null
                    )
                }
            }
            SettingsEvent.SaveDailyTarget -> saveDailyTarget()
            SettingsEvent.ExportTapped -> runBlocking { exportData() }
            SettingsEvent.ResetTapped -> {
                _uiState.update { it.copy(pendingConfirmation = SettingsConfirmationState.ResetAppData) }
            }
            SettingsEvent.ConfirmReset -> runBlocking { confirmReset() }
            SettingsEvent.DismissConfirmation -> {
                _uiState.update { it.copy(pendingConfirmation = null) }
            }
            SettingsEvent.DisconnectGoogleTapped -> runBlocking { disconnectGoogleFit() }
            SettingsEvent.DismissMessage -> {
                _uiState.update { it.copy(message = null) }
            }
        }
    }

    private fun load() {
        val preferences = preferencesStore.read()
        val integration = integrationStateProvider()
        sessionStore.read()
        _uiState.value = SettingsUiState(
            isLoading = false,
            dailyTargetCalories = preferences.dailyTargetCalories.toString(),
            integration = integration,
            integrationSummary = stateMapper.integrationSummary(integration),
            exportPresentation = stateMapper.exportPresentation(SettingsActionStatus.Idle),
            resetPresentation = stateMapper.resetPresentation(SettingsActionStatus.Idle)
        )
    }

    private fun saveDailyTarget() {
        val parsedValue = _uiState.value.dailyTargetCalories.toIntOrNull()
        if (parsedValue == null || parsedValue <= 0) {
            _uiState.update {
                it.copy(
                    dailyTargetError = UiMessage("Enter a valid positive calorie target.", isError = true)
                )
            }
            return
        }

        preferencesStore.update { current -> current.copy(dailyTargetCalories = parsedValue) }
        _uiState.update {
            it.copy(
                dailyTargetCalories = parsedValue.toString(),
                dailyTargetError = null,
                message = UiMessage("Daily target updated to $parsedValue kcal."),
                integration = integrationStateProvider(),
                integrationSummary = stateMapper.integrationSummary(integrationStateProvider())
            )
        }
    }

    private suspend fun exportData() {
        if (_uiState.value.exportStatus == SettingsActionStatus.InProgress) return

        updateExportStatus(SettingsActionStatus.InProgress)
        val result = exportCoordinator.export()
        result.fold(
            onSuccess = { snapshot ->
                val detail = "Exported ${snapshot.calorieEntries.size} calorie entries and ${snapshot.weightEntries.size} weight entries at ${snapshot.exportedAt}."
                updateExportStatus(SettingsActionStatus.Success(detail))
                _uiState.update { it.copy(message = UiMessage(detail)) }
            },
            onFailure = { error ->
                val detail = error.message ?: "Failed to export app data."
                updateExportStatus(SettingsActionStatus.Failure(detail))
                _uiState.update { it.copy(message = UiMessage(detail, isError = true)) }
            }
        )
    }

    private suspend fun confirmReset() {
        if (_uiState.value.pendingConfirmation != SettingsConfirmationState.ResetAppData) return
        if (_uiState.value.resetStatus == SettingsActionStatus.InProgress) return

        updateResetStatus(SettingsActionStatus.InProgress)
        val result = resetCoordinator.reset()
        result.fold(
            onSuccess = { resetResult ->
                val detail = "Removed ${resetResult.clearedCalorieEntries} calorie entries and ${resetResult.clearedWeightEntries} weight entries."
                updateResetStatus(SettingsActionStatus.Success(detail))
                _uiState.update {
                    it.copy(
                        pendingConfirmation = null,
                        dailyTargetCalories = preferencesStore.read().dailyTargetCalories.toString(),
                        dailyTargetError = null,
                        integration = integrationStateProvider(),
                        integrationSummary = stateMapper.integrationSummary(integrationStateProvider()),
                        message = UiMessage("BurnMate has been reset.")
                    )
                }
                onResetCompleted()
            },
            onFailure = { error ->
                val detail = error.message ?: "Failed to reset app data."
                updateResetStatus(SettingsActionStatus.Failure(detail))
                _uiState.update {
                    it.copy(
                        pendingConfirmation = null,
                        message = UiMessage(detail, isError = true)
                    )
                }
            }
        )
    }

    private suspend fun disconnectGoogleFit() {
        val result = disconnectGoogle()
        result.fold(
            onSuccess = {
                val integration = integrationStateProvider()
                _uiState.update {
                    it.copy(
                        integration = integration,
                        integrationSummary = stateMapper.integrationSummary(integration),
                        message = UiMessage("Google Fit disconnected.")
                    )
                }
            },
            onFailure = {
                _uiState.update {
                    it.copy(message = UiMessage("Failed to disconnect Google Fit.", isError = true))
                }
            }
        )
    }

    private fun updateExportStatus(status: SettingsActionStatus) {
        _uiState.update {
            it.copy(
                exportStatus = status,
                exportPresentation = stateMapper.exportPresentation(status)
            )
        }
    }

    private fun updateResetStatus(status: SettingsActionStatus) {
        _uiState.update {
            it.copy(
                resetStatus = status,
                resetPresentation = stateMapper.resetPresentation(status)
            )
        }
    }
}
