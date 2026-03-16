package org.kalpeshbkundanani.burnmate.ui.atoms

import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.kalpeshbkundanani.burnmate.ui.theme.BurnMateColors

@Composable
fun DividerLine(
    modifier: Modifier = Modifier
) {
    HorizontalDivider(
        modifier = modifier,
        thickness = 1.dp,
        color = BurnMateColors.Divider
    )
}
