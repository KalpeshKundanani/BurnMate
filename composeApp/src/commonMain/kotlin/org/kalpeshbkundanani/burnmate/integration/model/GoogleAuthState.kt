package org.kalpeshbkundanani.burnmate.integration.model

sealed interface GoogleAuthState {
    data object SignedOut : GoogleAuthState
    data object Authenticating : GoogleAuthState
    data class SignedIn(val session: GoogleAccountSession) : GoogleAuthState
}
