package org.kalpeshbkundanani.burnmate.presentation.dashboard

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
import org.kalpeshbkundanani.burnmate.dashboard.domain.DashboardReadModelService
import org.kalpeshbkundanani.burnmate.presentation.shared.LoadableUiState
import org.kalpeshbkundanani.burnmate.presentation.shared.UiMessage

class DashboardViewModel(
    private val dashboardService: DashboardReadModelService,
    private val uiMapper: DashboardUiMapper = DashboardUiMapper(),
    initialDate: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        DashboardUiState(
            selectedDate = initialDate
        )
    )
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun onEvent(event: DashboardEvent) {
        when (event) {
            DashboardEvent.Load -> loadDashboard()
            DashboardEvent.Retry -> loadDashboard()
            DashboardEvent.NextDayTapped -> {
                val nextDay = _uiState.value.selectedDate.plus(1, DateTimeUnit.DAY)
                _uiState.update { it.copy(selectedDate = nextDay) }
                loadDashboard()
            }
            DashboardEvent.PreviousDayTapped -> {
                val prevDay = _uiState.value.selectedDate.minus(1, DateTimeUnit.DAY)
                _uiState.update { it.copy(selectedDate = prevDay) }
                loadDashboard()
            }
            DashboardEvent.OpenLogging -> {
                // Handled in navigation
            }
        }
    }

    private fun loadDashboard() {
        val date = _uiState.value.selectedDate
        _uiState.update { it.copy(status = LoadableUiState.Loading, errorMessage = null, emptyMessage = null) }

        try {
            val snapshot = dashboardService.getDashboardSnapshot(date).getOrThrow()
            _uiState.update { currentState ->
                val cards = uiMapper.mapToCards(snapshot)
                currentState.copy(
                    status = LoadableUiState.Content,
                    todaySummary = cards.todayCard,
                    debtSummary = cards.debtCard,
                    weightSummary = cards.weightCard
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    status = LoadableUiState.Error,
                    errorMessage = UiMessage(e.message ?: "Failed to load dashboard", isError = true)
                )
            }
        }
    }
}
