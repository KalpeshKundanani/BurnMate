package org.kalpeshbkundanani.burnmate.ui.organisms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.kalpeshbkundanani.burnmate.ui.atoms.MetricText
import org.kalpeshbkundanani.burnmate.ui.components.GlassCard
import org.kalpeshbkundanani.burnmate.ui.molecules.SectionHeader
import org.kalpeshbkundanani.burnmate.ui.theme.BurnMateColors
import org.kalpeshbkundanani.burnmate.ui.theme.BurnMateTypography
import org.kalpeshbkundanani.burnmate.ui.theme.Spacing

@Composable
fun DebtSummaryCard(
    weeklyNet: String,
    comparisonStat: String,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = "Weekly Performance", modifier = Modifier.padding(bottom = Spacing.Small))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                MetricText(text = weeklyNet)
                Text(
                    text = comparisonStat,
                    style = BurnMateTypography.bodyMedium,
                    color = BurnMateColors.TextSecondary,
                    modifier = Modifier.padding(top = Spacing.XSmall)
                )
            }
        }
    }
}
