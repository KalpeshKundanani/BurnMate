package org.kalpeshbkundanani.burnmate.settings.export

import kotlinx.datetime.Instant
import org.kalpeshbkundanani.burnmate.logging.repository.EntryRepository
import org.kalpeshbkundanani.burnmate.settings.preferences.AppPreferencesStore
import org.kalpeshbkundanani.burnmate.settings.state.AppSessionStore
import org.kalpeshbkundanani.burnmate.weight.repository.WeightHistoryRepository

class DefaultAppExportCoordinator(
    private val sessionStore: AppSessionStore,
    private val preferencesStore: AppPreferencesStore,
    private val entryRepository: EntryRepository,
    private val weightRepository: WeightHistoryRepository,
    private val integrationStatusProvider: () -> String?,
    private val exportLauncher: AppExportLauncher,
    private val nowProvider: () -> Instant,
    private val entryDateRangeProvider: () -> Pair<org.kalpeshbkundanani.burnmate.logging.model.EntryDate, org.kalpeshbkundanani.burnmate.logging.model.EntryDate>
) : AppExportCoordinator {

    override suspend fun export(): Result<AppExportSnapshot> {
        val (startDate, endDate) = entryDateRangeProvider()
        val calorieEntries = entryRepository.fetchByDateRange(startDate, endDate)
            .mapCatching { entries ->
                entries.sortedWith(
                    compareBy<org.kalpeshbkundanani.burnmate.logging.model.CalorieEntry> { it.date.value }
                        .thenBy { it.createdAt }
                        .thenBy { it.id.value }
                )
            }
            .getOrElse {
                return Result.failure(IllegalStateException("Failed to export calorie entries", it))
            }

        val weightEntries = weightRepository.getAll()
            .mapCatching { entries -> entries.sortedBy { it.date } }
            .getOrElse {
                return Result.failure(IllegalStateException("Failed to export weight history", it))
            }

        val snapshot = AppExportSnapshot(
            exportedAt = nowProvider(),
            profile = sessionStore.read().activeProfile,
            preferences = preferencesStore.read(),
            calorieEntries = calorieEntries,
            weightEntries = weightEntries,
            integrationSummary = integrationStatusProvider()
        )

        val launchResult = exportLauncher.launch(snapshot)
        return launchResult.fold(
            onSuccess = { Result.success(snapshot) },
            onFailure = { Result.failure(IllegalStateException("Failed to hand off export", it)) }
        )
    }
}
