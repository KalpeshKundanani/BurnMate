package org.kalpeshbkundanani.burnmate.ui.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.kalpeshbkundanani.burnmate.ui.atoms.IconButton
import org.kalpeshbkundanani.burnmate.ui.theme.BurnMateColors
import org.kalpeshbkundanani.burnmate.ui.theme.BurnMateTypography
import org.kalpeshbkundanani.burnmate.ui.theme.Spacing

@Composable
fun DateSelector(
    dateLabel: String,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    canGoNext: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.Small),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = "Previous Day",
            onClick = onPreviousDay
        )
        
        Text(
            text = dateLabel,
            style = BurnMateTypography.titleMedium,
            color = BurnMateColors.TextPrimary
        )
        
        IconButton(
            icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Next Day",
            onClick = if (canGoNext) onNextDay else { {} },
            tint = if (canGoNext) BurnMateColors.TextSecondary else BurnMateColors.Divider
        )
    }
}
