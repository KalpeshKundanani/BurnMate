package org.kalpeshbkundanani.burnmate.ui.organisms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.kalpeshbkundanani.burnmate.ui.components.GlassCard
import org.kalpeshbkundanani.burnmate.ui.components.MetricDisplay
import org.kalpeshbkundanani.burnmate.ui.molecules.SectionHeader
import org.kalpeshbkundanani.burnmate.ui.theme.Spacing

@Composable
fun WeightSummaryCard(
    currentWeight: String,
    goalWeight: String,
    progress: String,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = "Weight Progress", modifier = Modifier.padding(bottom = Spacing.Small))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MetricDisplay(value = currentWeight, label = "Current")
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                MetricDisplay(value = progress, label = "Progress")
            }
            
            MetricDisplay(value = goalWeight, label = "Goal")
        }
    }
}
