package org.kalpeshbkundanani.burnmate.ui.atoms

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton as MaterialIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import org.kalpeshbkundanani.burnmate.ui.theme.BurnMateColors

@Composable
fun IconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = BurnMateColors.TextSecondary
) {
    MaterialIconButton(
        onClick = onClick,
        modifier = modifier,
        colors = IconButtonDefaults.iconButtonColors(contentColor = tint)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint
        )
    }
}
