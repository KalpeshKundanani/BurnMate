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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.kalpeshbkundanani.burnmate.presentation.logging.DailyLoggingEvent
import org.kalpeshbkundanani.burnmate.presentation.logging.DailyLoggingUiState
import org.kalpeshbkundanani.burnmate.presentation.shared.LoadableUiState
import org.kalpeshbkundanani.burnmate.ui.atoms.IconButton
import org.kalpeshbkundanani.burnmate.ui.atoms.PrimaryButton
import org.kalpeshbkundanani.burnmate.ui.components.GlassCard
import org.kalpeshbkundanani.burnmate.ui.molecules.DateSelector
import org.kalpeshbkundanani.burnmate.ui.molecules.InputField
import org.kalpeshbkundanani.burnmate.ui.organisms.BottomNavigationBar
import org.kalpeshbkundanani.burnmate.ui.organisms.NavigationTab
import org.kalpeshbkundanani.burnmate.ui.theme.BurnMateColors
import org.kalpeshbkundanani.burnmate.ui.theme.BurnMateTypography
import org.kalpeshbkundanani.burnmate.ui.theme.Spacing

@Composable
fun DailyLogScreen(
    state: DailyLoggingUiState,
    onEvent: (DailyLoggingEvent) -> Unit,
    onTabSelected: (NavigationTab) -> Unit
) {
    Scaffold(
        containerColor = BurnMateColors.BackgroundPrimary,
        bottomBar = {
            BottomNavigationBar(
                currentTab = NavigationTab.ACTIVITY,
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
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val isToday = state.selectedDate == today
            val dateLabel = if (isToday) "Today" else state.selectedDate.toString()
            
            DateSelector(
                dateLabel = dateLabel,
                onPreviousDay = { onEvent(DailyLoggingEvent.PreviousDayTapped) },
                onNextDay = { onEvent(DailyLoggingEvent.NextDayTapped) },
                canGoNext = !isToday,
                modifier = Modifier.padding(horizontal = Spacing.Large)
            )
            
            LoggingEntryEditor(state = state, onEvent = onEvent)
            
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
                                .fillMaxWidth()
                                .align(Alignment.Center)
                                .padding(horizontal = Spacing.Large),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
                        ) {
                            Text(
                                text = state.actionError?.message ?: "Failed to load entries.",
                                style = BurnMateTypography.bodyLarge,
                                color = BurnMateColors.Error
                            )
                            PrimaryButton(
                                text = "RETRY",
                                onClick = { onEvent(DailyLoggingEvent.Retry) }
                            )
                        }
                    }
                    LoadableUiState.Empty -> {
                        Text(
                            text = state.emptyMessage?.message ?: "No activity logged.",
                            style = BurnMateTypography.bodyLarge,
                            color = BurnMateColors.TextSecondary,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    LoadableUiState.Content -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = Spacing.Large),
                            verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
                        ) {
                            items(state.entries, key = { it.id }) { entry ->
                                GlassCard {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = entry.title,
                                                style = BurnMateTypography.titleMedium,
                                                color = BurnMateColors.TextPrimary
                                            )
                                            Text(
                                                text = entry.timestamp,
                                                style = BurnMateTypography.bodyMedium,
                                                color = BurnMateColors.TextSecondary,
                                                modifier = Modifier.padding(top = Spacing.XSmall)
                                            )
                                        }
                                        Text(
                                            text = entry.formattedCalories,
                                            style = BurnMateTypography.titleMedium,
                                            color = if (entry.isBurn) BurnMateColors.Success else BurnMateColors.Error,
                                            modifier = Modifier.padding(end = Spacing.Medium)
                                        )
                                        IconButton(
                                            icon = Icons.Filled.Delete,
                                            contentDescription = "Delete",
                                            onClick = { onEvent(DailyLoggingEvent.DeleteEntryTapped(entry.id)) }
                                        )
                                    }
                                }
                            }
                            item { Spacer(modifier = Modifier.height(Spacing.Large)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoggingEntryEditor(
    state: DailyLoggingUiState,
    onEvent: (DailyLoggingEvent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.Large, vertical = Spacing.Medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
    ) {
        InputField(
            value = state.entryDraft.amountInput,
            onValueChange = { onEvent(DailyLoggingEvent.CalorieInputChanged(it)) },
            label = "CALORIES",
            placeholder = "e.g. 500",
            keyboardType = KeyboardType.Number,
            isError = state.entryDraft.hasError,
            errorMessage = state.entryDraft.errorMessage?.message
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)
        ) {
            PrimaryButton(
                text = "ADD INTAKE",
                onClick = { onEvent(DailyLoggingEvent.AddIntakeTapped) },
                modifier = Modifier.weight(1f)
            )
            
            if (state.supportsBurnEntry) {
                PrimaryButton(
                    text = "ADD BURN",
                    onClick = { onEvent(DailyLoggingEvent.AddBurnTapped) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        state.actionError?.let { error ->
            Text(
                text = error.message,
                style = BurnMateTypography.bodyMedium,
                color = BurnMateColors.Error,
                modifier = Modifier.padding(top = Spacing.Small)
            )
        }
    }
}
