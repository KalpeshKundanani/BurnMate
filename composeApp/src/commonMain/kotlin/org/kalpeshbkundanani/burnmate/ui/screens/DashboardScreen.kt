package org.kalpeshbkundanani.burnmate.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.kalpeshbkundanani.burnmate.presentation.dashboard.DashboardEvent
import org.kalpeshbkundanani.burnmate.presentation.dashboard.DashboardUiState
import org.kalpeshbkundanani.burnmate.presentation.shared.LoadableUiState
import org.kalpeshbkundanani.burnmate.ui.atoms.PrimaryButton
import org.kalpeshbkundanani.burnmate.ui.molecules.DateSelector
import org.kalpeshbkundanani.burnmate.ui.organisms.ActionCardList
import org.kalpeshbkundanani.burnmate.ui.organisms.AppHeader
import org.kalpeshbkundanani.burnmate.ui.organisms.BottomNavigationBar
import org.kalpeshbkundanani.burnmate.ui.organisms.DashboardVisualProgressSection
import org.kalpeshbkundanani.burnmate.ui.organisms.DebtSummaryCard
import org.kalpeshbkundanani.burnmate.ui.organisms.HeroSummaryCard
import org.kalpeshbkundanani.burnmate.ui.organisms.NavigationTab
import org.kalpeshbkundanani.burnmate.ui.organisms.WeightSummaryCard
import org.kalpeshbkundanani.burnmate.ui.theme.BurnMateColors
import org.kalpeshbkundanani.burnmate.ui.theme.BurnMateTypography
import org.kalpeshbkundanani.burnmate.ui.theme.Spacing

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onEvent: (DashboardEvent) -> Unit,
    onTabSelected: (NavigationTab) -> Unit,
    onProfileClick: () -> Unit
) {
    Scaffold(
        containerColor = BurnMateColors.BackgroundPrimary,
        bottomBar = {
            BottomNavigationBar(
                currentTab = NavigationTab.HOME,
                onTabSelected = onTabSelected
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BurnMateColors.BackgroundPrimary)
        ) {
            AppHeader(
                modifier = Modifier.padding(horizontal = Spacing.Large),
                onProfileClick = onProfileClick
            )
            
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val isToday = state.selectedDate == today
            val dateLabel = if (isToday) "Today" else state.selectedDate.toString()
            
            DateSelector(
                dateLabel = dateLabel,
                onPreviousDay = { onEvent(DashboardEvent.PreviousDayTapped) },
                onNextDay = { onEvent(DashboardEvent.NextDayTapped) },
                canGoNext = !isToday,
                modifier = Modifier.padding(horizontal = Spacing.Large)
            )
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (state.status) {
                    LoadableUiState.Loading -> {
                        CircularProgressIndicator(
                            color = BurnMateColors.AccentPrimary,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    LoadableUiState.Error -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(Spacing.Large),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = state.errorMessage?.message ?: "Failed to load dashboard.",
                                style = BurnMateTypography.bodyLarge,
                                color = BurnMateColors.Error,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(Spacing.Medium))
                            PrimaryButton(text = "RETRY", onClick = { onEvent(DashboardEvent.Retry) })
                        }
                    }
                    LoadableUiState.Empty -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(Spacing.Large),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = state.emptyMessage?.message ?: "No data for this date.",
                                style = BurnMateTypography.bodyLarge,
                                color = BurnMateColors.TextSecondary
                            )
                        }
                    }
                    LoadableUiState.Content -> {
                        DashboardContent(state = state, onEvent = onEvent)
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardContent(
    state: DashboardUiState,
    onEvent: (DashboardEvent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.Large, vertical = Spacing.Medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.Large)
    ) {
        state.todaySummary?.let { sum ->
            HeroSummaryCard(
                title = "Today's Deficit",
                heroValue = sum.formattedCurrentDeficit,
                statLabel = "vs Yesterday",
                statValue = sum.formattedProgressVsYesterday
            )
        }
        
        state.debtSummary?.let { debt ->
            DebtSummaryCard(
                weeklyNet = debt.formattedWeeklyNet,
                comparisonStat = debt.formattedComparisonStat
            )
        }
        
        state.weightSummary?.let { weight ->
            WeightSummaryCard(
                currentWeight = weight.formattedCurrentWeight,
                goalWeight = weight.formattedGoalWeight,
                progress = weight.formattedProgress
            )
        }
        
        DashboardVisualProgressSection(
            state = state.visualization,
            onRangeSelected = { onEvent(DashboardEvent.ChartRangeSelected(it)) }
        )
        
        ActionCardList(
            onAddIntakeClick = { onEvent(DashboardEvent.OpenLogging) },
            onAddBurnClick = { onEvent(DashboardEvent.OpenLogging) },
            onLogWeightClick = { /* Not in scope of SLICE-0007 per PRD, ignore */ }
        )
        
        Spacer(modifier = Modifier.height(Spacing.Large))
    }
}
