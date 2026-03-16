package org.kalpeshbkundanani.burnmate.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.kalpeshbkundanani.burnmate.ui.theme.BurnMateColors
import org.kalpeshbkundanani.burnmate.ui.theme.Spacing

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(Spacing.Medium),
        colors = CardDefaults.cardColors(
            containerColor = BurnMateColors.SurfaceGlass
        ),
        border = BorderStroke(1.dp, BurnMateColors.Divider),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(Spacing.Default)) {
            content()
        }
    }
}
