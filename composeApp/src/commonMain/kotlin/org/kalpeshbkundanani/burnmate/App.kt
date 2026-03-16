package org.kalpeshbkundanani.burnmate

import androidx.compose.runtime.Composable
import org.kalpeshbkundanani.burnmate.ui.navigation.BurnMateNavigationHost
import org.kalpeshbkundanani.burnmate.ui.theme.BurnMateTheme

@Composable
fun App() {
    BurnMateTheme {
        BurnMateNavigationHost()
    }
}
