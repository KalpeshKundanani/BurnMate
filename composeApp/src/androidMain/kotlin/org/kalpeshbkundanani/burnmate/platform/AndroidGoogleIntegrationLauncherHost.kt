package org.kalpeshbkundanani.burnmate.platform

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine

class AndroidGoogleIntegrationLauncherHost(
    private val requestActivityRecognitionPermissionBlock: suspend () -> Boolean,
    private val launchIntentBlock: suspend (Intent) -> ActivityResult
) {
    suspend fun requestActivityRecognitionPermission(): Boolean {
        return requestActivityRecognitionPermissionBlock()
    }

    suspend fun launchIntent(intent: Intent): ActivityResult {
        return launchIntentBlock(intent)
    }
}

@Composable
fun rememberAndroidGoogleIntegrationLauncherHost(
    activity: ComponentActivity
): AndroidGoogleIntegrationLauncherHost {
    val permissionContinuation = remember { mutableStateOf<CancellableContinuation<Boolean>?>(null) }
    val intentContinuation = remember { mutableStateOf<CancellableContinuation<ActivityResult>?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionContinuation.value?.resume(granted)
        permissionContinuation.value = null
    }
    val intentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        intentContinuation.value?.resume(result)
        intentContinuation.value = null
    }

    return remember(activity, permissionLauncher, intentLauncher) {
        AndroidGoogleIntegrationLauncherHost(
            requestActivityRecognitionPermissionBlock = {
                suspendCancellableCoroutine { continuation ->
                    permissionContinuation.value = continuation
                    permissionLauncher.launch(android.Manifest.permission.ACTIVITY_RECOGNITION)
                }
            },
            launchIntentBlock = { intent ->
                suspendCancellableCoroutine { continuation ->
                    intentContinuation.value = continuation
                    intentLauncher.launch(intent)
                }
            }
        )
    }
}
