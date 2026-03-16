package org.kalpeshbkundanani.burnmate.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = BurnMateColors.AccentPrimary,
    secondary = BurnMateColors.AccentSecondary,
    background = BurnMateColors.BackgroundPrimary,
    surface = BurnMateColors.SurfaceGlass,
    error = BurnMateColors.Error,
    onPrimary = BurnMateColors.BackgroundPrimary,
    onSecondary = BurnMateColors.BackgroundPrimary,
    onBackground = BurnMateColors.TextPrimary,
    onSurface = BurnMateColors.TextPrimary,
    onError = BurnMateColors.TextPrimary
)

@Composable
fun BurnMateTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = BurnMateTypography,
        content = content
    )
}
