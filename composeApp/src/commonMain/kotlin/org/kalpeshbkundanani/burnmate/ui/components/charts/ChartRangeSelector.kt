package org.kalpeshbkundanani.burnmate.ui.components.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.kalpeshbkundanani.burnmate.presentation.dashboard.charts.ChartRangeOption
import org.kalpeshbkundanani.burnmate.ui.theme.BurnMateColors
import org.kalpeshbkundanani.burnmate.ui.theme.BurnMateTypography
import org.kalpeshbkundanani.burnmate.ui.theme.Spacing

@Composable
fun ChartRangeSelector(
    selectedRange: ChartRangeOption,
    onRangeSelected: (ChartRangeOption) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(BurnMateColors.BackgroundSecondary)
            .padding(2.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChartRangeOption.entries.forEach { option ->
            val isSelected = option == selectedRange
            val bgColor = if (isSelected) BurnMateColors.AccentPrimary else Color.Transparent
            val textColor = if (isSelected) BurnMateColors.BackgroundPrimary else BurnMateColors.TextSecondary

            Text(
                text = option.label,
                style = BurnMateTypography.labelMedium,
                color = textColor,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(bgColor)
                    .clickable { onRangeSelected(option) }
                    .padding(horizontal = Spacing.Medium, vertical = Spacing.Small)
            )
        }
    }
}
