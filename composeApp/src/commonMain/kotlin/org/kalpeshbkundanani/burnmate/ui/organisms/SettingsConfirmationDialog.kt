package org.kalpeshbkundanani.burnmate.ui.organisms

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import org.kalpeshbkundanani.burnmate.ui.theme.BurnMateColors
import org.kalpeshbkundanani.burnmate.ui.theme.BurnMateTypography

@Composable
fun SettingsConfirmationDialog(
    title: String,
    body: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = BurnMateTypography.titleMedium,
                color = BurnMateColors.TextPrimary
            )
        },
        text = {
            Text(
                text = body,
                style = BurnMateTypography.bodyMedium,
                color = BurnMateColors.TextSecondary
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = confirmLabel, color = BurnMateColors.Error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = dismissLabel, color = BurnMateColors.TextSecondary)
            }
        }
    )
}
