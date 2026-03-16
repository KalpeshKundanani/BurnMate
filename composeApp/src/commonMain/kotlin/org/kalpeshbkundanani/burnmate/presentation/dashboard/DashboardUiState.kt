package org.kalpeshbkundanani.burnmate.presentation.dashboard

import kotlinx.datetime.LocalDate
import org.kalpeshbkundanani.burnmate.presentation.shared.LoadableUiState
import org.kalpeshbkundanani.burnmate.presentation.shared.UiMessage

data class DashboardTodayCardState(
    val formattedCurrentDeficit: String,
    val formattedProgressVsYesterday: String,
    val deficitValue: Int
)

data class DashboardDebtCardState(
    val formattedWeeklyNet: String,
    val formattedComparisonStat: String
)

data class DashboardWeightCardState(
    val formattedCurrentWeight: String,
    val formattedGoalWeight: String,
    val formattedProgress: String
)

data class DashboardUiState(
    val selectedDate: LocalDate,
    val status: LoadableUiState = LoadableUiState.Loading,
    val todaySummary: DashboardTodayCardState? = null,
    val debtSummary: DashboardDebtCardState? = null,
    val weightSummary: DashboardWeightCardState? = null,
    val emptyMessage: UiMessage? = null,
    val errorMessage: UiMessage? = null
)

sealed interface DashboardEvent {
    data object Load : DashboardEvent
    data object PreviousDayTapped : DashboardEvent
    data object NextDayTapped : DashboardEvent
    data object Retry : DashboardEvent
    data object OpenLogging : DashboardEvent
}
