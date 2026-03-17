package org.kalpeshbkundanani.burnmate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.kalpeshbkundanani.burnmate.platform.rememberAndroidAppExportLauncher
import org.kalpeshbkundanani.burnmate.platform.rememberAndroidGoogleIntegrationBridge
import org.kalpeshbkundanani.burnmate.ui.navigation.BurnMateNavigationHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            BurnMateNavigationHost(
                googleIntegrationBridge = rememberAndroidGoogleIntegrationBridge(this),
                appExportLauncher = rememberAndroidAppExportLauncher(this)
            )
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    BurnMateNavigationHost()
}
