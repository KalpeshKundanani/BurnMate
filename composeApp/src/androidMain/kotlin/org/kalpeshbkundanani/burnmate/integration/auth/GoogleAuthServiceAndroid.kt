package org.kalpeshbkundanani.burnmate.integration.auth

import androidx.activity.ComponentActivity
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import org.kalpeshbkundanani.burnmate.integration.model.GoogleAccountSession
import org.kalpeshbkundanani.burnmate.integration.model.GoogleAuthState
import org.kalpeshbkundanani.burnmate.platform.GoogleIntegrationConfiguration

class GoogleAuthServiceAndroid(
    private val activity: ComponentActivity,
    private val configuration: GoogleIntegrationConfiguration
) : GoogleAuthService {
    private val credentialManager = CredentialManager.create(activity)
    private var cachedSession: GoogleAccountSession? = null

    override fun readCachedState(): GoogleAuthState {
        cachedSession?.let { return GoogleAuthState.SignedIn(it) }
        val account = GoogleSignIn.getLastSignedInAccount(activity) ?: return GoogleAuthState.SignedOut
        return GoogleAuthState.SignedIn(account.toSession())
    }

    override suspend fun signIn(): GoogleAuthLaunchResult {
        val webClientId = configuration.webClientId
            ?: return GoogleAuthLaunchResult.Failure(IllegalStateException("Missing Google web client id"))

        return try {
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(
                    GetGoogleIdOption.Builder()
                        .setServerClientId(webClientId)
                        .setFilterByAuthorizedAccounts(false)
                        .setAutoSelectEnabled(false)
                        .build()
                )
                .build()
            val result = credentialManager.getCredential(context = activity, request = request)
            val customCredential = result.credential
            val lastSignedInAccount = GoogleSignIn.getLastSignedInAccount(activity)
            val tokenCredential = GoogleIdTokenCredential.createFrom(customCredential.data)
            val session = lastSignedInAccount?.toSession() ?: GoogleAccountSession(
                subjectId = tokenCredential.id,
                displayName = tokenCredential.displayName,
                email = tokenCredential.id.takeIf { it.contains('@') }
            )
            cachedSession = session
            GoogleAuthLaunchResult.Success(session)
        } catch (_: GetCredentialCancellationException) {
            GoogleAuthLaunchResult.Cancelled
        } catch (_: NoCredentialException) {
            GoogleAuthLaunchResult.Cancelled
        } catch (error: Throwable) {
            GoogleAuthLaunchResult.Failure(error)
        }
    }

    override suspend fun disconnect(): Result<Unit> {
        cachedSession = null
        return try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            GoogleSignIn.getClient(activity, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut()
            Result.success(Unit)
        } catch (error: Throwable) {
            Result.failure(error)
        }
    }

    private fun com.google.android.gms.auth.api.signin.GoogleSignInAccount.toSession(): GoogleAccountSession {
        return GoogleAccountSession(
            subjectId = id ?: email ?: displayName ?: "google-user",
            displayName = displayName,
            email = email
        )
    }
}
