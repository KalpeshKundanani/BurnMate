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
import org.kalpeshbkundanani.burnmate.presentation.shared.SelectedDateCoordinator
import org.kalpeshbkundanani.burnmate.presentation.shared.UiMessage
import org.kalpeshbkundanani.burnmate.presentation.dashboard.charts.ChartRangeOption
import org.kalpeshbkundanani.burnmate.presentation.dashboard.charts.DashboardChartDataSource
import org.kalpeshbkundanani.burnmate.presentation.dashboard.charts.DashboardChartStateAdapter

class DashboardViewModel(
    private val dashboardService: DashboardReadModelService,
    private val chartDataSource: DashboardChartDataSource,
    private val chartAdapter: DashboardChartStateAdapter,
    private val uiMapper: DashboardUiMapper = DashboardUiMapper(),
    initialDate: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
    private val selectedDateCoordinator: SelectedDateCoordinator = SelectedDateCoordinator(initialDate)
) : ViewModel() {
    private val stopObservingSelectedDate: () -> Unit

    private val _uiState = MutableStateFlow(
        DashboardUiState(
            selectedDate = selectedDateCoordinator.selectedDate
        )
    )
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        stopObservingSelectedDate = selectedDateCoordinator.observe { date ->
            _uiState.update { it.copy(selectedDate = date) }
            loadDashboard(date)
        }
    }

    fun onEvent(event: DashboardEvent) {
        when (event) {
            DashboardEvent.Load -> loadDashboard()
            DashboardEvent.Retry -> loadDashboard()
            DashboardEvent.NextDayTapped -> {
                selectedDateCoordinator.updateSelectedDate(_uiState.value.selectedDate.plus(1, DateTimeUnit.DAY))
            }
            DashboardEvent.PreviousDayTapped -> {
                selectedDateCoordinator.updateSelectedDate(_uiState.value.selectedDate.minus(1, DateTimeUnit.DAY))
            }
            is DashboardEvent.ChartRangeSelected -> {
                val snapshot = dashboardService.getDashboardSnapshot(_uiState.value.selectedDate).getOrNull()
                loadVisualization(_uiState.value.selectedDate, event.range, snapshot?.weightSummary)
            }
            DashboardEvent.OpenLogging -> {
                // Handled in navigation
            }
        }
    }

    override fun onCleared() {
        stopObservingSelectedDate()
        super.onCleared()
    }

    private fun loadDashboard(date: LocalDate = _uiState.value.selectedDate) {
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
            loadVisualization(date, _uiState.value.visualization.selectedRange, snapshot.weightSummary)
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    status = LoadableUiState.Error,
                    errorMessage = UiMessage(e.message ?: "Failed to load dashboard", isError = true)
                )
            }
        }
    }

    private fun loadVisualization(
        selectedDate: LocalDate,
        range: ChartRangeOption,
        weightSummary: org.kalpeshbkundanani.burnmate.dashboard.model.WeightSummary?
    ) {
        _uiState.update { currentState ->
            currentState.copy(
                visualization = currentState.visualization.copy(status = LoadableUiState.Loading, selectedRange = range)
            )
        }

        try {
            val debtSnapshot = chartDataSource.loadDebtChartSnapshot(selectedDate, range).getOrThrow()
            val weightEntries = chartDataSource.loadWeightEntries(selectedDate, range).getOrThrow()
            
            val dashboardChartState = chartAdapter.map(
                range = range,
                debtPoints = debtSnapshot.debtChartPoints,
                weightSummary = weightSummary,
                weightEntries = weightEntries
            )
            
            val status = if (
                dashboardChartState.debtTrend == null &&
                dashboardChartState.weightTrend == null &&
                dashboardChartState.weeklyDeficit == null &&
                dashboardChartState.progressRing == null
            ) {
                LoadableUiState.Empty
            } else {
                LoadableUiState.Content
            }
            
            _uiState.update { currentState ->
                currentState.copy(
                    visualization = currentState.visualization.copy(
                        status = status,
                        charts = dashboardChartState
                    )
                )
            }
        } catch (e: Exception) {
            _uiState.update { currentState ->
                currentState.copy(
                    visualization = currentState.visualization.copy(
                        status = LoadableUiState.Error,
                        errorMessage = UiMessage(e.message ?: "Failed to load visualizations", isError = true)
                    )
                )
            }
        }
    }
}
