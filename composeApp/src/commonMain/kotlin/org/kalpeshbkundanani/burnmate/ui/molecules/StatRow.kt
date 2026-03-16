package org.kalpeshbkundanani.burnmate.ui.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.kalpeshbkundanani.burnmate.ui.atoms.LabelText
import org.kalpeshbkundanani.burnmate.ui.theme.BurnMateColors
import org.kalpeshbkundanani.burnmate.ui.theme.BurnMateTypography
import org.kalpeshbkundanani.burnmate.ui.theme.Spacing

@Composable
fun StatRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.XSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LabelText(text = label)
        Text(
            text = value,
            style = BurnMateTypography.bodyLarge,
            color = BurnMateColors.TextPrimary
        )
    }
}
