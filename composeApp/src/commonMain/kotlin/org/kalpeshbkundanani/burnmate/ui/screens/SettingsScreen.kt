package org.kalpeshbkundanani.burnmate.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.kalpeshbkundanani.burnmate.presentation.settings.SettingsConfirmationState
import org.kalpeshbkundanani.burnmate.presentation.settings.SettingsEvent
import org.kalpeshbkundanani.burnmate.presentation.settings.SettingsUiState
import org.kalpeshbkundanani.burnmate.ui.organisms.AppHeader
import org.kalpeshbkundanani.burnmate.ui.organisms.SettingsActionRow
import org.kalpeshbkundanani.burnmate.ui.organisms.SettingsConfirmationDialog
import org.kalpeshbkundanani.burnmate.ui.organisms.SettingsPreferenceRow
import org.kalpeshbkundanani.burnmate.ui.organisms.SettingsSectionCard
import org.kalpeshbkundanani.burnmate.ui.theme.BurnMateColors
import org.kalpeshbkundanani.burnmate.ui.theme.BurnMateTypography
import org.kalpeshbkundanani.burnmate.ui.theme.Spacing

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = BurnMateColors.BackgroundPrimary
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BurnMateColors.BackgroundPrimary)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.Large, vertical = Spacing.Default),
            verticalArrangement = Arrangement.spacedBy(Spacing.Large)
        ) {
            AppHeader(onProfileClick = onBack)

            Text(
                text = "Settings",
                style = BurnMateTypography.displayMedium,
                color = BurnMateColors.TextPrimary
            )

            if (state.isLoading) {
                CircularProgressIndicator(
                    color = BurnMateColors.AccentPrimary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
                SettingsSectionCard(title = "Preferences") {
                    SettingsPreferenceRow(
                        value = state.dailyTargetCalories,
                        error = state.dailyTargetError,
                        onValueChange = { onEvent(SettingsEvent.DailyTargetChanged(it)) },
                        onSave = { onEvent(SettingsEvent.SaveDailyTarget) }
                    )
                }

                SettingsSectionCard(title = "Integrations") {
                    SettingsActionRow(
                        presentation = org.kalpeshbkundanani.burnmate.presentation.settings.SettingsActionPresentation(
                            title = state.integrationSummary.title,
                            detail = state.integrationSummary.detail,
                            actionLabel = state.integrationSummary.actionLabel ?: "CONNECTED",
                            actionEnabled = state.integrationSummary.actionLabel != null
                        ),
                        onClick = { onEvent(SettingsEvent.DisconnectGoogleTapped) }
                    )
                }

                SettingsSectionCard(title = "Export") {
                    SettingsActionRow(
                        presentation = state.exportPresentation,
                        onClick = { onEvent(SettingsEvent.ExportTapped) }
                    )
                }

                SettingsSectionCard(title = "Reset") {
                    SettingsActionRow(
                        presentation = state.resetPresentation,
                        onClick = { onEvent(SettingsEvent.ResetTapped) }
                    )
                }

                SettingsSectionCard(title = "App Info") {
                    Text(
                        text = state.appInfo,
                        style = BurnMateTypography.bodyLarge,
                        color = BurnMateColors.TextPrimary
                    )
                    Text(
                        text = state.message?.message ?: "Manage release-ready app settings, exports, and reset flows here.",
                        style = BurnMateTypography.bodyMedium,
                        color = if (state.message?.isError == true) BurnMateColors.Error else BurnMateColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.Large))
        }
    }

    if (state.pendingConfirmation == SettingsConfirmationState.ResetAppData) {
        SettingsConfirmationDialog(
            title = "Reset BurnMate?",
            body = "This clears your profile, logs, weights, settings, and Google Fit access. This action cannot be undone.",
            confirmLabel = "RESET APP DATA",
            dismissLabel = "CANCEL",
            onConfirm = { onEvent(SettingsEvent.ConfirmReset) },
            onDismiss = { onEvent(SettingsEvent.DismissConfirmation) }
        )
    }
}
