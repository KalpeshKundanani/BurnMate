package org.kalpeshbkundanani.burnmate.ui.organisms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.kalpeshbkundanani.burnmate.ui.atoms.MetricText
import org.kalpeshbkundanani.burnmate.ui.components.GlassCard
import org.kalpeshbkundanani.burnmate.ui.molecules.SectionHeader
import org.kalpeshbkundanani.burnmate.ui.molecules.StatRow
import org.kalpeshbkundanani.burnmate.ui.theme.Spacing

@Composable
fun HeroSummaryCard(
    title: String,
    heroValue: String,
    statLabel: String,
    statValue: String,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = title, modifier = Modifier.padding(bottom = Spacing.XSmall))
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.Medium),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MetricText(text = heroValue)
        }
        
        StatRow(label = statLabel, value = statValue)
    }
}
