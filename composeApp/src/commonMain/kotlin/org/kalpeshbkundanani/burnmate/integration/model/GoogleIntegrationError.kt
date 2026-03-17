package org.kalpeshbkundanani.burnmate.integration.model

sealed class GoogleIntegrationError(message: String) : IllegalStateException(message) {
    data object Unavailable : GoogleIntegrationError("integration unavailable")
    data object SignInCancelled : GoogleIntegrationError("sign in cancelled")
    data object PermissionDenied : GoogleIntegrationError("permission denied")
    data class SignInFailed(val detail: String) : GoogleIntegrationError(detail)
    data class ImportFailed(val detail: String) : GoogleIntegrationError(detail)
    data class SyncFailed(val detail: String) : GoogleIntegrationError(detail)
}
