package org.kalpeshbkundanani.burnmate.ui.organisms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.Text
import org.kalpeshbkundanani.burnmate.presentation.settings.SettingsActionPresentation
import org.kalpeshbkundanani.burnmate.ui.atoms.PrimaryButton
import org.kalpeshbkundanani.burnmate.ui.theme.BurnMateColors
import org.kalpeshbkundanani.burnmate.ui.theme.BurnMateTypography
import org.kalpeshbkundanani.burnmate.ui.theme.Spacing

@Composable
fun SettingsActionRow(
    presentation: SettingsActionPresentation,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Default)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)
        ) {
            Text(
                text = presentation.title,
                style = BurnMateTypography.titleMedium,
                color = if (presentation.isDanger) BurnMateColors.Error else BurnMateColors.TextPrimary
            )
            Text(
                text = presentation.detail,
                style = BurnMateTypography.bodyMedium,
                color = if (presentation.isDanger) BurnMateColors.Error else BurnMateColors.TextSecondary
            )
        }

        PrimaryButton(
            text = presentation.actionLabel,
            onClick = onClick,
            enabled = presentation.actionEnabled
        )
    }
}
