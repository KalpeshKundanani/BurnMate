package org.kalpeshbkundanani.burnmate.settings.reset

data class AppResetResult(
    val clearedCalorieEntries: Int,
    val clearedWeightEntries: Int,
    val disconnectedIntegration: Boolean
)

interface AppResetCoordinator {
    suspend fun reset(): Result<AppResetResult>
}
