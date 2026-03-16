package org.kalpeshbkundanani.burnmate.ui.organisms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.kalpeshbkundanani.burnmate.ui.atoms.IconButton
import org.kalpeshbkundanani.burnmate.ui.theme.BurnMateColors
import org.kalpeshbkundanani.burnmate.ui.theme.BurnMateTypography
import org.kalpeshbkundanani.burnmate.ui.theme.Spacing

@Composable
fun AppHeader(
    modifier: Modifier = Modifier,
    onProfileClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.Default),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "BURNMATE.",
            style = BurnMateTypography.titleLarge,
            color = BurnMateColors.TextPrimary
        )
        
        IconButton(
            icon = Icons.Filled.AccountCircle,
            contentDescription = "Profile",
            onClick = onProfileClick
        )
    }
}
