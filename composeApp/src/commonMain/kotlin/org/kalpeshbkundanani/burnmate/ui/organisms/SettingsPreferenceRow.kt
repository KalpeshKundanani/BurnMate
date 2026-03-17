package org.kalpeshbkundanani.burnmate.ui.organisms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.Text
import org.kalpeshbkundanani.burnmate.presentation.shared.UiMessage
import org.kalpeshbkundanani.burnmate.ui.atoms.PrimaryButton
import org.kalpeshbkundanani.burnmate.ui.molecules.InputField
import org.kalpeshbkundanani.burnmate.ui.theme.BurnMateColors
import org.kalpeshbkundanani.burnmate.ui.theme.BurnMateTypography
import org.kalpeshbkundanani.burnmate.ui.theme.Spacing

@Composable
fun SettingsPreferenceRow(
    value: String,
    error: UiMessage?,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.Small)
    ) {
        Text(
            text = "Adjust the calorie target used by dashboard summaries and chart calculations.",
            style = BurnMateTypography.bodyMedium,
            color = BurnMateColors.TextSecondary
        )
        InputField(
            value = value,
            onValueChange = onValueChange,
            label = "Daily target calories",
            isError = error != null,
            errorMessage = error?.message,
            keyboardType = KeyboardType.Number,
            placeholder = "2000"
        )
        PrimaryButton(
            text = "SAVE TARGET",
            onClick = onSave
        )
    }
}
