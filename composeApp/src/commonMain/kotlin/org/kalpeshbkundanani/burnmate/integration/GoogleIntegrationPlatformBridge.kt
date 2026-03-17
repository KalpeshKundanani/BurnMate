package org.kalpeshbkundanani.burnmate.integration

import kotlinx.datetime.LocalDate
import org.kalpeshbkundanani.burnmate.integration.auth.GoogleAuthLaunchResult
import org.kalpeshbkundanani.burnmate.integration.auth.GoogleAuthService
import org.kalpeshbkundanani.burnmate.integration.fit.GoogleFitService
import org.kalpeshbkundanani.burnmate.integration.model.FitPermissionState
import org.kalpeshbkundanani.burnmate.integration.model.GoogleAccountSession
import org.kalpeshbkundanani.burnmate.integration.model.GoogleAuthState
import org.kalpeshbkundanani.burnmate.integration.model.GoogleIntegrationAvailability
import org.kalpeshbkundanani.burnmate.integration.model.GoogleIntegrationError
import org.kalpeshbkundanani.burnmate.integration.model.ImportedActivitySample
import org.kalpeshbkundanani.burnmate.integration.permission.FitPermissionRequestResult
import org.kalpeshbkundanani.burnmate.integration.permission.PermissionCoordinator

data class GoogleIntegrationPlatformBridge(
    val authService: GoogleAuthService,
    val permissionCoordinator: PermissionCoordinator,
    val fitService: GoogleFitService
)

fun unavailableGoogleIntegrationBridge(
    availability: GoogleIntegrationAvailability = GoogleIntegrationAvailability.UnsupportedPlatform
): GoogleIntegrationPlatformBridge {
    val unavailableError = GoogleIntegrationError.Unavailable
    return GoogleIntegrationPlatformBridge(
        authService = object : GoogleAuthService {
            override fun readCachedState(): GoogleAuthState = GoogleAuthState.SignedOut
            override suspend fun signIn(): GoogleAuthLaunchResult = GoogleAuthLaunchResult.Failure(unavailableError)
            override suspend fun disconnect(): Result<Unit> = Result.success(Unit)
        },
        permissionCoordinator = object : PermissionCoordinator {
            override fun readState(session: GoogleAccountSession?): FitPermissionState = FitPermissionState.Unavailable

            override suspend fun requestPermissions(session: GoogleAccountSession): FitPermissionRequestResult {
                return FitPermissionRequestResult.Failure(unavailableError)
            }
        },
        fitService = object : GoogleFitService {
            override fun availability(): GoogleIntegrationAvailability = availability

            override suspend fun readDailyActivity(
                session: GoogleAccountSession,
                startDate: LocalDate,
                endDate: LocalDate
            ): Result<List<ImportedActivitySample>> = Result.failure(unavailableError)

            override suspend fun disconnect(session: GoogleAccountSession): Result<Unit> = Result.success(Unit)
        }
    )
}
