package org.kalpeshbkundanani.burnmate.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.kalpeshbkundanani.burnmate.ui.theme.BurnMateColors
import org.kalpeshbkundanani.burnmate.ui.theme.BurnMateTypography
import org.kalpeshbkundanani.burnmate.ui.theme.Spacing

@Composable
fun ActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(Spacing.Medium),
        colors = CardDefaults.cardColors(
            containerColor = BurnMateColors.SurfaceGlass
        ),
        border = BorderStroke(1.dp, BurnMateColors.Divider),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.Default),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = BurnMateTypography.titleMedium,
                    color = BurnMateColors.TextPrimary
                )
                Text(
                    text = subtitle,
                    style = BurnMateTypography.bodyMedium,
                    modifier = Modifier.padding(top = Spacing.XSmall)
                )
            }
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = BurnMateColors.AccentPrimary
            )
        }
    }
}
