package org.kalpeshbkundanani.burnmate.ui.organisms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.kalpeshbkundanani.burnmate.ui.components.ActionCard
import org.kalpeshbkundanani.burnmate.ui.theme.Spacing

@Composable
fun ActionCardList(
    onAddIntakeClick: () -> Unit,
    onAddBurnClick: () -> Unit,
    onLogWeightClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
    ) {
        ActionCard(
            title = "Add Intake",
            subtitle = "Log meals & snacks",
            icon = Icons.Filled.Add,
            onClick = onAddIntakeClick
        )
        ActionCard(
            title = "Add Burn",
            subtitle = "Log workout session",
            icon = Icons.Filled.Whatshot,
            onClick = onAddBurnClick
        )
        ActionCard(
            title = "Log Weight",
            subtitle = "Track your progress",
            icon = Icons.Filled.MonitorWeight,
            onClick = onLogWeightClick
        )
    }
}
