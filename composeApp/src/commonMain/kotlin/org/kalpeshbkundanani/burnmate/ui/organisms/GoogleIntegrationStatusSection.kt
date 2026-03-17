package org.kalpeshbkundanani.burnmate.ui.organisms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.kalpeshbkundanani.burnmate.integration.model.GoogleAuthState
import org.kalpeshbkundanani.burnmate.presentation.integration.GoogleIntegrationEvent
import org.kalpeshbkundanani.burnmate.presentation.integration.GoogleIntegrationPhase
import org.kalpeshbkundanani.burnmate.presentation.integration.GoogleIntegrationUiState
import org.kalpeshbkundanani.burnmate.ui.atoms.PrimaryButton
import org.kalpeshbkundanani.burnmate.ui.components.GlassCard
import org.kalpeshbkundanani.burnmate.ui.molecules.SectionHeader
import org.kalpeshbkundanani.burnmate.ui.theme.BurnMateColors
import org.kalpeshbkundanani.burnmate.ui.theme.BurnMateTypography
import org.kalpeshbkundanani.burnmate.ui.theme.Spacing

@Composable
fun GoogleIntegrationStatusSection(
    state: GoogleIntegrationUiState,
    onEvent: (GoogleIntegrationEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.Large),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
        ) {
            SectionHeader(title = "Google Fit")
            Text(
                text = "Connect Google to import burn into BurnMate",
                style = BurnMateTypography.bodyMedium,
                color = BurnMateColors.TextSecondary
            )
            Text(
                text = headline(state),
                style = BurnMateTypography.titleMedium,
                color = BurnMateColors.TextPrimary
            )
            Text(
                text = bodyText(state),
                style = BurnMateTypography.bodyMedium,
                color = if (state.message?.isError == true) BurnMateColors.Error else BurnMateColors.TextSecondary
            )
            ActionRow(state = state, onEvent = onEvent)
        }
    }
}

@Composable
private fun ActionRow(
    state: GoogleIntegrationUiState,
    onEvent: (GoogleIntegrationEvent) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)
    ) {
        when (state.phase) {
            GoogleIntegrationPhase.SignedOut -> {
                PrimaryButton(
                    text = "CONNECT GOOGLE",
                    onClick = { onEvent(GoogleIntegrationEvent.SignInClicked) },
                    modifier = Modifier.weight(1f)
                )
            }
            GoogleIntegrationPhase.Authenticating -> {
                PrimaryButton(text = "CONNECTING...", onClick = {}, modifier = Modifier.weight(1f))
            }
            GoogleIntegrationPhase.PermissionRequired -> {
                PrimaryButton(
                    text = "GRANT ACCESS",
                    onClick = { onEvent(GoogleIntegrationEvent.GrantPermissionsClicked) },
                    modifier = Modifier.weight(1f)
                )
                PrimaryButton(
                    text = "DISCONNECT",
                    onClick = { onEvent(GoogleIntegrationEvent.DisconnectClicked) },
                    modifier = Modifier.weight(1f)
                )
            }
            GoogleIntegrationPhase.Syncing -> {
                PrimaryButton(text = "SYNCING...", onClick = {}, modifier = Modifier.weight(1f))
            }
            GoogleIntegrationPhase.SignedIn,
            GoogleIntegrationPhase.Imported,
            GoogleIntegrationPhase.Error -> {
                PrimaryButton(
                    text = "SYNC NOW",
                    onClick = { onEvent(GoogleIntegrationEvent.RefreshImportClicked) },
                    modifier = Modifier.weight(1f)
                )
                PrimaryButton(
                    text = "DISCONNECT",
                    onClick = { onEvent(GoogleIntegrationEvent.DisconnectClicked) },
                    modifier = Modifier.weight(1f)
                )
            }
            GoogleIntegrationPhase.Unavailable -> Unit
        }
    }
}

private fun headline(state: GoogleIntegrationUiState): String {
    return when (state.phase) {
        GoogleIntegrationPhase.SignedOut -> "Google Fit is not connected"
        GoogleIntegrationPhase.Authenticating -> "Connecting your Google account"
        GoogleIntegrationPhase.SignedIn -> accountText(state.authState) ?: "Google account connected"
        GoogleIntegrationPhase.PermissionRequired -> "Google Fit permission required"
        GoogleIntegrationPhase.Syncing -> "Importing Google Fit history"
        GoogleIntegrationPhase.Imported -> "Google Fit import complete"
        GoogleIntegrationPhase.Error -> "Google Fit import needs attention"
        GoogleIntegrationPhase.Unavailable -> "Google Fit is unavailable"
    }
}

private fun bodyText(state: GoogleIntegrationUiState): String {
    state.message?.let { return it.message }

    return when (state.phase) {
        GoogleIntegrationPhase.SignedOut -> "Connect a Google account to import the last 30 days of burn data."
        GoogleIntegrationPhase.Authenticating -> "Finish Google sign-in to continue."
        GoogleIntegrationPhase.SignedIn -> {
            val summary = state.syncSummary
            if (summary == null) {
                "Google is connected. You can import burn data for the selected date window."
            } else {
                "Last import: ${summary.importedEntries} entries from ${summary.startDate} to ${summary.endDate}."
            }
        }
        GoogleIntegrationPhase.PermissionRequired -> "BurnMate needs activity recognition and Google Fit read access."
        GoogleIntegrationPhase.Syncing -> "BurnMate is reading Google Fit steps and calories for the selected 30-day window."
        GoogleIntegrationPhase.Imported -> {
            val summary = state.syncSummary
            if (summary == null) {
                "Google Fit import completed."
            } else {
                "Imported ${summary.importedEntries} entries across ${summary.importedDays} day(s) from ${summary.startDate} to ${summary.endDate}."
            }
        }
        GoogleIntegrationPhase.Error -> "Retry the import or disconnect the current Google account."
        GoogleIntegrationPhase.Unavailable -> "This build cannot complete Google Sign-In and Google Fit import."
    }
}

private fun accountText(authState: GoogleAuthState): String? {
    val signedIn = authState as? GoogleAuthState.SignedIn ?: return null
    return signedIn.session.displayName ?: signedIn.session.email
}
