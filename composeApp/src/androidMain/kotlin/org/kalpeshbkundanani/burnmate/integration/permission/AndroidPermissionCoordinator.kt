package org.kalpeshbkundanani.burnmate.integration.permission

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import org.kalpeshbkundanani.burnmate.integration.model.FitPermissionState
import org.kalpeshbkundanani.burnmate.integration.model.GoogleAccountSession
import org.kalpeshbkundanani.burnmate.integration.model.GoogleIntegrationAvailability
import org.kalpeshbkundanani.burnmate.platform.AndroidGoogleIntegrationLauncherHost
import org.kalpeshbkundanani.burnmate.platform.GoogleIntegrationConfiguration

class AndroidPermissionCoordinator(
    private val activity: ComponentActivity,
    private val configuration: GoogleIntegrationConfiguration,
    private val launcherHost: AndroidGoogleIntegrationLauncherHost
) : PermissionCoordinator {
    private val fitnessOptions: FitnessOptions = FitnessOptions.builder()
        .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_CALORIES_EXPENDED, FitnessOptions.ACCESS_READ)
        .build()

    override fun readState(session: GoogleAccountSession?): FitPermissionState {
        if (configuration.availability() != GoogleIntegrationAvailability.Available) {
            return FitPermissionState.Unavailable
        }
        if (!hasActivityRecognitionPermission()) {
            return FitPermissionState.Required
        }
        val account = GoogleSignIn.getLastSignedInAccount(activity) ?: return FitPermissionState.Required
        return if (GoogleSignIn.hasPermissions(account, fitnessOptions)) {
            FitPermissionState.Granted
        } else {
            FitPermissionState.Required
        }
    }

    override suspend fun requestPermissions(session: GoogleAccountSession): FitPermissionRequestResult {
        if (configuration.availability() != GoogleIntegrationAvailability.Available) {
            return FitPermissionRequestResult.Failure(IllegalStateException("Integration unavailable"))
        }
        if (!hasActivityRecognitionPermission() && !launcherHost.requestActivityRecognitionPermission()) {
            return FitPermissionRequestResult.Denied
        }

        val currentAccount = GoogleSignIn.getLastSignedInAccount(activity)
        if (currentAccount != null && GoogleSignIn.hasPermissions(currentAccount, fitnessOptions)) {
            return FitPermissionRequestResult.Granted
        }

        return try {
            val result = launcherHost.launchIntent(
                GoogleSignIn.getClient(activity, googleSignInOptions()).signInIntent
            )
            if (result.resultCode != Activity.RESULT_OK || result.data == null) {
                FitPermissionRequestResult.Cancelled
            } else {
                val account = GoogleSignIn
                    .getSignedInAccountFromIntent(result.data)
                    .getResult(ApiException::class.java)
                if (GoogleSignIn.hasPermissions(account, fitnessOptions)) {
                    FitPermissionRequestResult.Granted
                } else {
                    FitPermissionRequestResult.Denied
                }
            }
        } catch (error: Throwable) {
            FitPermissionRequestResult.Failure(error)
        }
    }

    private fun hasActivityRecognitionPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return true
        }
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun googleSignInOptions(): GoogleSignInOptions {
        return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope("https://www.googleapis.com/auth/fitness.activity.read"))
            .build()
    }
}
