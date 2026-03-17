package org.kalpeshbkundanani.burnmate.integration.permission

import org.kalpeshbkundanani.burnmate.integration.model.FitPermissionState
import org.kalpeshbkundanani.burnmate.integration.model.GoogleAccountSession

sealed interface FitPermissionRequestResult {
    data class Granted(val authorizedSession: GoogleAccountSession) : FitPermissionRequestResult
    data class AccountMismatch(val authorizedSession: GoogleAccountSession) : FitPermissionRequestResult
    data object Denied : FitPermissionRequestResult
    data object Cancelled : FitPermissionRequestResult
    data class Failure(val error: Throwable) : FitPermissionRequestResult
}

interface PermissionCoordinator {
    fun readState(session: GoogleAccountSession?): FitPermissionState
    suspend fun requestPermissions(session: GoogleAccountSession): FitPermissionRequestResult
}
