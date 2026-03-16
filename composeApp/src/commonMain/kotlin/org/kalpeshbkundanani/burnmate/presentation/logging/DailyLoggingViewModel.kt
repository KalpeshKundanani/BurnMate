package org.kalpeshbkundanani.burnmate.presentation.logging

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.kalpeshbkundanani.burnmate.logging.domain.CalorieEntryFactory
import org.kalpeshbkundanani.burnmate.logging.model.CalorieAmount
import org.kalpeshbkundanani.burnmate.logging.model.EntryDate
import org.kalpeshbkundanani.burnmate.logging.model.EntryId
import org.kalpeshbkundanani.burnmate.logging.repository.EntryRepository
import org.kalpeshbkundanani.burnmate.presentation.shared.LoadableUiState
import org.kalpeshbkundanani.burnmate.presentation.shared.SelectedDateCoordinator
import org.kalpeshbkundanani.burnmate.presentation.shared.UiMessage

class DailyLoggingViewModel(
    private val repository: EntryRepository,
    private val factory: CalorieEntryFactory,
    private val mapper: DailyLoggingUiMapper = DailyLoggingUiMapper(),
    initialDate: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
    private val selectedDateCoordinator: SelectedDateCoordinator = SelectedDateCoordinator(initialDate)
) : ViewModel() {
    private val stopObservingSelectedDate: () -> Unit

    private val _uiState = MutableStateFlow(
        DailyLoggingUiState(
            selectedDate = selectedDateCoordinator.selectedDate,
            supportsBurnEntry = true // We present the UI option even if domain might reject negative values based on PRD requirement
        )
    )
    val uiState: StateFlow<DailyLoggingUiState> = _uiState.asStateFlow()

    init {
        stopObservingSelectedDate = selectedDateCoordinator.observe { date ->
            _uiState.update { it.copy(selectedDate = date) }
            loadEntries(date)
        }
    }

    fun onEvent(event: DailyLoggingEvent) {
        when (event) {
            DailyLoggingEvent.Load -> loadEntries()
            DailyLoggingEvent.Retry -> loadEntries()
            is DailyLoggingEvent.CalorieInputChanged -> {
                _uiState.update { 
                    it.copy(
                        entryDraft = it.entryDraft.copy(
                            amountInput = event.value,
                            hasError = false,
                            errorMessage = null
                        ),
                        actionError = null
                    ) 
                }
            }
            DailyLoggingEvent.AddIntakeTapped -> addEntry(isIntake = true)
            DailyLoggingEvent.AddBurnTapped -> addEntry(isIntake = false)
            is DailyLoggingEvent.DeleteEntryTapped -> deleteEntry(event.id)
            DailyLoggingEvent.NextDayTapped -> {
                selectedDateCoordinator.updateSelectedDate(_uiState.value.selectedDate.plus(1, DateTimeUnit.DAY))
            }
            DailyLoggingEvent.PreviousDayTapped -> {
                selectedDateCoordinator.updateSelectedDate(_uiState.value.selectedDate.minus(1, DateTimeUnit.DAY))
            }
        }
    }

    override fun onCleared() {
        stopObservingSelectedDate()
        super.onCleared()
    }

    private fun loadEntries(date: LocalDate = _uiState.value.selectedDate) {
        _uiState.update { it.copy(status = LoadableUiState.Loading, actionError = null, emptyMessage = null) }

        val result = repository.fetchByDate(EntryDate(date))
        result.fold(
            onSuccess = { entries ->
                if (entries.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            status = LoadableUiState.Empty,
                            entries = emptyList(),
                            emptyMessage = UiMessage("No activity logged.")
                        )
                    }
                } else {
                    val mapped = entries
                        .sortedByDescending { entry -> entry.createdAt }
                        .map { mapper.mapToLogEntryItemState(it) }
                    _uiState.update { it.copy(status = LoadableUiState.Content, entries = mapped) }
                }
            },
            onFailure = {
                _uiState.update {
                    it.copy(
                        status = LoadableUiState.Error,
                        actionError = UiMessage("Failed to load entries.", isError = true)
                    )
                }
            }
        )
    }

    private fun addEntry(isIntake: Boolean) {
        val state = _uiState.value
        val amountStr = state.entryDraft.amountInput
        val amountInt = amountStr.toIntOrNull()

        if (amountInt == null || amountInt <= 0) {
            _uiState.update { 
                it.copy(
                    entryDraft = it.entryDraft.copy(
                        hasError = true, 
                        errorMessage = UiMessage("Enter a valid positive number.", isError = true)
                    )
                ) 
            }
            return
        }

        val finalAmount = if (isIntake) amountInt else -amountInt

        val createResult = factory.create(
            date = EntryDate(state.selectedDate),
            amount = CalorieAmount(finalAmount)
        )

        createResult.fold(
            onSuccess = { newEntry ->
                val saveResult = repository.create(newEntry)
                saveResult.fold(
                    onSuccess = {
                        _uiState.update {
                            it.copy(
                                entryDraft = it.entryDraft.copy(amountInput = "", hasError = false, errorMessage = null),
                                actionError = null
                            )
                        }
                        loadEntries()
                    },
                    onFailure = {
                        _uiState.update { it.copy(actionError = UiMessage("Failed to save entry.", isError = true)) }
                    }
                )
            },
            onFailure = { error ->
                _uiState.update {
                    it.copy(
                        entryDraft = it.entryDraft.copy(
                            hasError = true,
                            errorMessage = UiMessage("Unsupported or invalid entry: ${error.message}", isError = true)
                        )
                    )
                }
            }
        )
    }

    private fun deleteEntry(id: String) {
        val result = repository.deleteById(EntryId(id))
        result.fold(
            onSuccess = { loadEntries() },
            onFailure = {
                _uiState.update { it.copy(actionError = UiMessage("Failed to delete entry.", isError = true)) }
            }
        )
    }
}
