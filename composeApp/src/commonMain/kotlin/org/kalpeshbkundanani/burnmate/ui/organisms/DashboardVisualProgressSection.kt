package org.kalpeshbkundanani.burnmate.ui.organisms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.kalpeshbkundanani.burnmate.presentation.dashboard.charts.ChartRangeOption
import org.kalpeshbkundanani.burnmate.presentation.dashboard.charts.DashboardVisualizationUiState
import org.kalpeshbkundanani.burnmate.presentation.shared.LoadableUiState
import org.kalpeshbkundanani.burnmate.ui.components.charts.ChartRangeSelector
import org.kalpeshbkundanani.burnmate.ui.components.charts.ChartSurface
import org.kalpeshbkundanani.burnmate.ui.components.charts.DebtTrendChart
import org.kalpeshbkundanani.burnmate.ui.components.charts.GoalProgressRing
import org.kalpeshbkundanani.burnmate.ui.components.charts.WeeklyDeficitBarChart
import org.kalpeshbkundanani.burnmate.ui.components.charts.WeightTrendChart
import org.kalpeshbkundanani.burnmate.ui.theme.BurnMateColors
import org.kalpeshbkundanani.burnmate.ui.theme.BurnMateTypography
import org.kalpeshbkundanani.burnmate.ui.theme.Spacing

@Composable
fun DashboardVisualProgressSection(
    state: DashboardVisualizationUiState,
    onRangeSelected: (ChartRangeOption) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.Large),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ChartRangeSelector(
            selectedRange = state.selectedRange,
            onRangeSelected = onRangeSelected
        )
        
        when (state.status) {
            LoadableUiState.Loading -> {
                CircularProgressIndicator(
                    color = BurnMateColors.AccentPrimary,
                    modifier = Modifier.padding(Spacing.Large)
                )
            }
            LoadableUiState.Error -> {
                Text(
                    text = state.errorMessage?.message ?: "Failed to load visualizations.",
                    style = BurnMateTypography.bodyMedium,
                    color = BurnMateColors.Error,
                    modifier = Modifier.padding(Spacing.Large)
                )
            }
            LoadableUiState.Empty -> {
                Text(
                    text = state.emptyMessage?.message ?: "Not enough data to display visualizations.",
                    style = BurnMateTypography.bodyMedium,
                    color = BurnMateColors.TextSecondary,
                    modifier = Modifier.padding(Spacing.Large)
                )
            }
            LoadableUiState.Content -> {
                state.charts?.let { charts ->
                    charts.debtTrend?.let { debtTrend ->
                        DebtTrendChart(state = debtTrend, modifier = Modifier.fillMaxWidth())
                    } ?: run {
                        EmptyChartState(message = DEBT_TREND_EMPTY_MESSAGE)
                    }

                    charts.weeklyDeficit?.let { weeklyDeficit ->
                        WeeklyDeficitBarChart(state = weeklyDeficit, modifier = Modifier.fillMaxWidth())
                    } ?: run {
                        EmptyChartState(
                            title = "Weekly Deficit",
                            message = weeklyDeficitEmptyMessage(charts.weeklyDeficit)
                        )
                    }

                    charts.weightTrend?.let { weightTrend ->
                        WeightTrendChart(state = weightTrend, modifier = Modifier.fillMaxWidth())
                    } ?: run {
                        EmptyChartState(message = WEIGHT_TREND_EMPTY_MESSAGE)
                    }

                    charts.progressRing?.let { progressRing ->
                        GoalProgressRing(state = progressRing, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(Spacing.Medium))
    }
}

@Composable
private fun EmptyChartState(
    message: String,
    title: String? = null
) {
    ChartSurface(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            title?.let {
                Text(
                    text = it,
                    style = BurnMateTypography.labelLarge,
                    color = BurnMateColors.TextSecondary
                )
            }
            Text(
                text = message,
                style = BurnMateTypography.bodyMedium,
                color = BurnMateColors.TextSecondary,
                modifier = Modifier.padding(vertical = Spacing.Medium)
            )
        }
    }
}

internal const val DEBT_TREND_EMPTY_MESSAGE: String = "No calorie debt history in selected range."
internal const val WEEKLY_DEFICIT_EMPTY_MESSAGE: String = "Need at least two days of debt history to show weekly deficit changes."
internal const val WEIGHT_TREND_EMPTY_MESSAGE: String = "No weight history in selected range."

internal fun weeklyDeficitEmptyMessage(
    weeklyDeficit: org.kalpeshbkundanani.burnmate.presentation.dashboard.charts.WeeklyDeficitChartState?
): String {
    return if (weeklyDeficit == null) WEEKLY_DEFICIT_EMPTY_MESSAGE else ""
}
