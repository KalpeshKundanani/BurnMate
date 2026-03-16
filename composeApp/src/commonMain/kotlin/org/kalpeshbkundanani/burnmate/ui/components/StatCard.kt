package org.kalpeshbkundanani.burnmate.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.kalpeshbkundanani.burnmate.ui.atoms.LabelText
import org.kalpeshbkundanani.burnmate.ui.theme.BurnMateColors
import org.kalpeshbkundanani.burnmate.ui.theme.BurnMateTypography
import org.kalpeshbkundanani.burnmate.ui.theme.Spacing

@Composable
fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        Column {
            LabelText(text = label)
            Text(
                text = value,
                style = BurnMateTypography.titleLarge,
                color = BurnMateColors.TextPrimary,
                modifier = Modifier.padding(top = Spacing.XSmall)
            )
        }
    }
}
