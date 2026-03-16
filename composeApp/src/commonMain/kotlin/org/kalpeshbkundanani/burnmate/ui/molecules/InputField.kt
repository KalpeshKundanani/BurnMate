package org.kalpeshbkundanani.burnmate.ui.molecules

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import org.kalpeshbkundanani.burnmate.ui.atoms.LabelText
import org.kalpeshbkundanani.burnmate.ui.theme.BurnMateColors
import org.kalpeshbkundanani.burnmate.ui.theme.BurnMateTypography
import org.kalpeshbkundanani.burnmate.ui.theme.Spacing

@Composable
fun InputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    placeholder: String? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        LabelText(
            text = label,
            modifier = Modifier.padding(bottom = Spacing.XSmall)
        )
        
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            isError = isError,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            placeholder = placeholder?.let { { Text(it, color = BurnMateColors.TextSecondary) } },
            textStyle = BurnMateTypography.bodyLarge.copy(color = BurnMateColors.TextPrimary),
            shape = RoundedCornerShape(Spacing.Small),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BurnMateColors.AccentPrimary,
                unfocusedBorderColor = BurnMateColors.Divider,
                errorBorderColor = BurnMateColors.Error,
                cursorColor = BurnMateColors.AccentPrimary,
                focusedContainerColor = BurnMateColors.BackgroundSecondary,
                unfocusedContainerColor = BurnMateColors.BackgroundSecondary,
                errorContainerColor = BurnMateColors.BackgroundSecondary
            )
        )
        
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                style = BurnMateTypography.labelSmall,
                color = BurnMateColors.Error,
                modifier = Modifier.padding(top = Spacing.XSmall)
            )
        }
    }
}
