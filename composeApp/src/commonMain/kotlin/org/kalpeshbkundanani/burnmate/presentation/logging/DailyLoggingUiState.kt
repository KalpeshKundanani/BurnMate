package org.kalpeshbkundanani.burnmate.presentation.logging

import kotlinx.datetime.LocalDate
import org.kalpeshbkundanani.burnmate.presentation.shared.LoadableUiState
import org.kalpeshbkundanani.burnmate.presentation.shared.UiMessage

data class LogEntryItemState(
    val id: String,
    val title: String,
    val timestamp: String,
    val formattedCalories: String,
    val isBurn: Boolean
)

data class LoggingEntryDraftState(
    val amountInput: String = "",
    val hasError: Boolean = false,
    val errorMessage: UiMessage? = null
)

data class DailyLoggingUiState(
    val selectedDate: LocalDate,
    val status: LoadableUiState = LoadableUiState.Loading,
    val entries: List<LogEntryItemState> = emptyList(),
    val entryDraft: LoggingEntryDraftState = LoggingEntryDraftState(),
    val supportsBurnEntry: Boolean = false,
    val emptyMessage: UiMessage? = null,
    val actionError: UiMessage? = null
)

sealed interface DailyLoggingEvent {
    data object Load : DailyLoggingEvent
    data class CalorieInputChanged(val value: String) : DailyLoggingEvent
    data object AddIntakeTapped : DailyLoggingEvent
    data object AddBurnTapped : DailyLoggingEvent
    data class DeleteEntryTapped(val id: String) : DailyLoggingEvent
    data object PreviousDayTapped : DailyLoggingEvent
    data object NextDayTapped : DailyLoggingEvent
    data object Retry : DailyLoggingEvent
}
