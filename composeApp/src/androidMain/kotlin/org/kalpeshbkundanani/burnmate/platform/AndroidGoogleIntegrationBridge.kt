package org.kalpeshbkundanani.burnmate.platform

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.kalpeshbkundanani.burnmate.integration.GoogleIntegrationPlatformBridge
import org.kalpeshbkundanani.burnmate.integration.auth.GoogleAuthServiceAndroid
import org.kalpeshbkundanani.burnmate.integration.fit.GoogleFitServiceAndroid
import org.kalpeshbkundanani.burnmate.integration.permission.AndroidPermissionCoordinator

@Composable
fun rememberAndroidGoogleIntegrationBridge(
    activity: ComponentActivity
): GoogleIntegrationPlatformBridge {
    val configuration = remember(activity) { GoogleIntegrationConfiguration.fromContext(activity) }
    val launcherHost = rememberAndroidGoogleIntegrationLauncherHost(activity)

    return remember(activity, configuration, launcherHost) {
        GoogleIntegrationPlatformBridge(
            authService = GoogleAuthServiceAndroid(
                activity = activity,
                configuration = configuration
            ),
            permissionCoordinator = AndroidPermissionCoordinator(
                activity = activity,
                configuration = configuration,
                launcherHost = launcherHost
            ),
            fitService = GoogleFitServiceAndroid(
                context = activity,
                configuration = configuration
            )
        )
    }
}
