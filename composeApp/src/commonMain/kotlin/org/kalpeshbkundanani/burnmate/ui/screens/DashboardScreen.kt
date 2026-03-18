package org.kalpeshbkundanani.burnmate.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.kalpeshbkundanani.burnmate.presentation.dashboard.DashboardEvent
import org.kalpeshbkundanani.burnmate.presentation.dashboard.DashboardUiState
import org.kalpeshbkundanani.burnmate.presentation.integration.GoogleIntegrationEvent
import org.kalpeshbkundanani.burnmate.presentation.integration.GoogleIntegrationUiState
import org.kalpeshbkundanani.burnmate.presentation.shared.LoadableUiState
import org.kalpeshbkundanani.burnmate.ui.atoms.PrimaryButton
import org.kalpeshbkundanani.burnmate.ui.molecules.DateSelector
import org.kalpeshbkundanani.burnmate.ui.molecules.InputField
import org.kalpeshbkundanani.burnmate.ui.organisms.ActionCardList
import org.kalpeshbkundanani.burnmate.ui.organisms.AppHeader
import org.kalpeshbkundanani.burnmate.ui.organisms.BottomNavigationBar
import org.kalpeshbkundanani.burnmate.ui.organisms.DashboardVisualProgressSection
import org.kalpeshbkundanani.burnmate.ui.organisms.DebtSummaryCard
import org.kalpeshbkundanani.burnmate.ui.organisms.GoogleIntegrationStatusSection
import org.kalpeshbkundanani.burnmate.ui.organisms.HeroSummaryCard
import org.kalpeshbkundanani.burnmate.ui.organisms.NavigationTab
import org.kalpeshbkundanani.burnmate.ui.organisms.WeightSummaryCard
import org.kalpeshbkundanani.burnmate.ui.theme.BurnMateColors
import org.kalpeshbkundanani.burnmate.ui.theme.BurnMateTypography
import org.kalpeshbkundanani.burnmate.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: DashboardUiState,
    integrationState: GoogleIntegrationUiState,
    onEvent: (DashboardEvent) -> Unit,
    onIntegrationEvent: (GoogleIntegrationEvent) -> Unit,
    onTabSelected: (NavigationTab) -> Unit,
    onProfileClick: () -> Unit
) {
    if (state.weightLogSheet.isVisible) {
        ModalBottomSheet(
            onDismissRequest = { onEvent(DashboardEvent.DismissWeightLogging) },
            containerColor = BurnMateColors.BackgroundSecondary
        ) {
            DashboardWeightLogSheet(
                state = state,
                onEvent = onEvent
            )
        }
    }

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
                        DashboardContent(
                            state = state,
                            integrationState = integrationState,
                            onEvent = onEvent,
                            onIntegrationEvent = onIntegrationEvent
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardContent(
    state: DashboardUiState,
    integrationState: GoogleIntegrationUiState,
    onEvent: (DashboardEvent) -> Unit,
    onIntegrationEvent: (GoogleIntegrationEvent) -> Unit
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

        GoogleIntegrationStatusSection(
            state = integrationState,
            onEvent = onIntegrationEvent
        )

        ActionCardList(
            onAddIntakeClick = { onEvent(DashboardEvent.OpenLogging) },
            onAddBurnClick = { onEvent(DashboardEvent.OpenLogging) },
            onLogWeightClick = { onEvent(DashboardEvent.OpenWeightLogging) }
        )
        
        Spacer(modifier = Modifier.height(Spacing.Large))
    }
}

@Composable
private fun DashboardWeightLogSheet(
    state: DashboardUiState,
    onEvent: (DashboardEvent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.Large, vertical = Spacing.Medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
    ) {
        Text(
            text = "Log Weight",
            style = BurnMateTypography.titleLarge,
            color = BurnMateColors.TextPrimary
        )
        Text(
            text = "Save your weight for ${state.selectedDate}. If a weight is already logged for this date, it will be updated.",
            style = BurnMateTypography.bodyMedium,
            color = BurnMateColors.TextSecondary
        )
        InputField(
            value = state.weightLogSheet.weightInput,
            onValueChange = { onEvent(DashboardEvent.WeightInputChanged(it)) },
            label = "WEIGHT (kg)",
            placeholder = "e.g. 82.4",
            keyboardType = KeyboardType.Decimal,
            isError = state.weightLogSheet.errorMessage != null,
            errorMessage = state.weightLogSheet.errorMessage?.message
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)
        ) {
            PrimaryButton(
                text = "CANCEL",
                onClick = { onEvent(DashboardEvent.DismissWeightLogging) },
                enabled = !state.weightLogSheet.isSaving,
                modifier = Modifier.weight(1f)
            )
            PrimaryButton(
                text = if (state.weightLogSheet.isSaving) "SAVING..." else "SAVE WEIGHT",
                onClick = { onEvent(DashboardEvent.SaveWeightEntry) },
                enabled = !state.weightLogSheet.isSaving,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(Spacing.Medium))
    }
}
