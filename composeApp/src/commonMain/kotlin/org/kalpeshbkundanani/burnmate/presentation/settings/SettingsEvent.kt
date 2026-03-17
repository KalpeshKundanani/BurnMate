package org.kalpeshbkundanani.burnmate.presentation.settings

sealed interface SettingsEvent {
    data object Load : SettingsEvent
    data class DailyTargetChanged(val value: String) : SettingsEvent
    data object SaveDailyTarget : SettingsEvent
    data object ExportTapped : SettingsEvent
    data object ResetTapped : SettingsEvent
    data object ConfirmReset : SettingsEvent
    data object DismissConfirmation : SettingsEvent
    data object DisconnectGoogleTapped : SettingsEvent
    data object DismissMessage : SettingsEvent
}
