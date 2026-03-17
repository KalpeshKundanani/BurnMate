package org.kalpeshbkundanani.burnmate.presentation.dashboard.charts

import org.kalpeshbkundanani.burnmate.presentation.shared.LoadableUiState
import org.kalpeshbkundanani.burnmate.presentation.shared.UiMessage

data class DashboardVisualizationUiState(
    val selectedRange: ChartRangeOption = ChartRangeOption.Last7Days,
    val status: LoadableUiState = LoadableUiState.Loading,
    val charts: DashboardChartState? = null,
    val emptyMessage: UiMessage? = null,
    val errorMessage: UiMessage? = null
)
