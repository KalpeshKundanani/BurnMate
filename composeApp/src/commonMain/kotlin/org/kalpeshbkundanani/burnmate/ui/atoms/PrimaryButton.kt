package org.kalpeshbkundanani.burnmate.ui.atoms

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.kalpeshbkundanani.burnmate.ui.theme.BurnMateColors
import org.kalpeshbkundanani.burnmate.ui.theme.BurnMateTypography
import org.kalpeshbkundanani.burnmate.ui.theme.Spacing

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(Spacing.Medium),
        colors = ButtonDefaults.buttonColors(
            containerColor = BurnMateColors.AccentPrimary,
            contentColor = BurnMateColors.BackgroundPrimary,
            disabledContainerColor = BurnMateColors.SurfaceGlass,
            disabledContentColor = BurnMateColors.TextSecondary
        ),
        contentPadding = PaddingValues(horizontal = Spacing.Large)
    ) {
        Text(
            text = text,
            style = BurnMateTypography.bodyLarge
        )
    }
}
