package org.kalpeshbkundanani.burnmate.integration.auth

import org.kalpeshbkundanani.burnmate.integration.model.GoogleAccountSession
import org.kalpeshbkundanani.burnmate.integration.model.GoogleAuthState

sealed interface GoogleAuthLaunchResult {
    data class Success(val session: GoogleAccountSession) : GoogleAuthLaunchResult
    data object Cancelled : GoogleAuthLaunchResult
    data class Failure(val error: Throwable) : GoogleAuthLaunchResult
}

interface GoogleAuthService {
    fun readCachedState(): GoogleAuthState
    suspend fun signIn(): GoogleAuthLaunchResult
    suspend fun disconnect(): Result<Unit>
}
