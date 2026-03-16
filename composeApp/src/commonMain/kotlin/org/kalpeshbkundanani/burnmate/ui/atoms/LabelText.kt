package org.kalpeshbkundanani.burnmate.ui.atoms

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import org.kalpeshbkundanani.burnmate.ui.theme.BurnMateColors
import org.kalpeshbkundanani.burnmate.ui.theme.BurnMateTypography

@Composable
fun LabelText(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = BurnMateTypography.labelSmall,
        color = BurnMateColors.TextSecondary,
        textAlign = textAlign
    )
}
