package org.kalpeshbkundanani.burnmate.settings.reset

import kotlinx.datetime.LocalDate
import org.kalpeshbkundanani.burnmate.logging.model.EntryDate
import org.kalpeshbkundanani.burnmate.logging.repository.EntryRepository
import org.kalpeshbkundanani.burnmate.settings.preferences.AppPreferencesStore
import org.kalpeshbkundanani.burnmate.settings.state.AppSessionStore
import org.kalpeshbkundanani.burnmate.weight.repository.WeightHistoryRepository

class DefaultAppResetCoordinator(
    private val sessionStore: AppSessionStore,
    private val preferencesStore: AppPreferencesStore,
    private val entryRepository: EntryRepository,
    private val weightRepository: WeightHistoryRepository,
    private val integrationDisconnect: suspend () -> Result<Boolean>,
    private val dateRangeProvider: () -> Pair<EntryDate, EntryDate>,
    private val weightDatesProvider: () -> Result<List<LocalDate>>
) : AppResetCoordinator {

    override suspend fun reset(): Result<AppResetResult> {
        val disconnectedIntegration = integrationDisconnect().getOrElse {
            return Result.failure(IllegalStateException("Failed to disconnect Google Fit.", it))
        }

        val (startDate, endDate) = dateRangeProvider()
        val calorieEntries = entryRepository.fetchByDateRange(startDate, endDate).getOrElse {
            return Result.failure(IllegalStateException("Failed to clear calorie entries.", it))
        }
        calorieEntries.forEach { entry ->
            val deleteResult = entryRepository.deleteById(entry.id).getOrElse {
                return Result.failure(IllegalStateException("Failed to clear calorie entries.", it))
            }
            if (!deleteResult) {
                return Result.failure(IllegalStateException("Failed to clear calorie entries."))
            }
        }

        val weightDates = weightDatesProvider().getOrElse {
            return Result.failure(IllegalStateException("Failed to clear weight history.", it))
        }
        weightDates.forEach { date ->
            val deleteResult = weightRepository.deleteByDate(date).getOrElse {
                return Result.failure(IllegalStateException("Failed to clear weight history.", it))
            }
            if (!deleteResult) {
                return Result.failure(IllegalStateException("Failed to clear weight history."))
            }
        }

        preferencesStore.reset()
        sessionStore.reset()

        return Result.success(
            AppResetResult(
                clearedCalorieEntries = calorieEntries.size,
                clearedWeightEntries = weightDates.size,
                disconnectedIntegration = disconnectedIntegration
            )
        )
    }
}
