package org.kalpeshbkundanani.burnmate.settings.reset

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.kalpeshbkundanani.burnmate.logging.model.CalorieAmount
import org.kalpeshbkundanani.burnmate.logging.model.CalorieEntry
import org.kalpeshbkundanani.burnmate.logging.model.EntryDate
import org.kalpeshbkundanani.burnmate.logging.model.EntryId
import org.kalpeshbkundanani.burnmate.logging.repository.EntryRepository
import org.kalpeshbkundanani.burnmate.profile.model.BmiCategory
import org.kalpeshbkundanani.burnmate.profile.model.BmiSnapshot
import org.kalpeshbkundanani.burnmate.profile.model.BodyMetrics
import org.kalpeshbkundanani.burnmate.profile.model.GoalValidationReason
import org.kalpeshbkundanani.burnmate.profile.model.GoalValidationResult
import org.kalpeshbkundanani.burnmate.profile.model.UserProfileSummary
import org.kalpeshbkundanani.burnmate.settings.preferences.AppPreferences
import org.kalpeshbkundanani.burnmate.settings.preferences.InMemoryAppPreferencesStore
import org.kalpeshbkundanani.burnmate.settings.state.AppSessionState
import org.kalpeshbkundanani.burnmate.settings.state.InMemoryAppSessionStore
import org.kalpeshbkundanani.burnmate.weight.model.WeightEntry
import org.kalpeshbkundanani.burnmate.weight.model.WeightValue
import org.kalpeshbkundanani.burnmate.weight.repository.WeightHistoryRepository

class DefaultAppResetCoordinatorTest {

    @Test
    fun `reset clears app-managed state in scope`() {
        val preferencesStore = InMemoryAppPreferencesStore()
        preferencesStore.update { AppPreferences(dailyTargetCalories = 2400) }
        val sessionStore = InMemoryAppSessionStore()
        sessionStore.update { AppSessionState(activeProfile = validProfileSummary()) }
        val entryRepository = MutableEntryRepository()
        entryRepository.create(
            CalorieEntry(
                id = EntryId("entry-1"),
                date = EntryDate(LocalDate(2026, 3, 16)),
                amount = CalorieAmount(300),
                createdAt = Instant.parse("2026-03-16T10:00:00Z")
            )
        )
        val weightRepository = MutableWeightHistoryRepository(
            mutableListOf(
                WeightEntry(
                    date = LocalDate(2026, 3, 17),
                    weight = WeightValue(80.0),
                    createdAt = Instant.parse("2026-03-17T09:00:00Z")
                )
            )
        )
        val coordinator = DefaultAppResetCoordinator(
            sessionStore = sessionStore,
            preferencesStore = preferencesStore,
            entryRepository = entryRepository,
            weightRepository = weightRepository,
            integrationDisconnect = { Result.success(true) },
            dateRangeProvider = { EntryDate(LocalDate(2026, 1, 1)) to EntryDate(LocalDate(2026, 12, 31)) },
            weightDatesProvider = { weightRepository.getAll().map { entries -> entries.map { it.date } } }
        )

        val result = runBlocking { coordinator.reset().getOrThrow() }

        assertEquals(1, result.clearedCalorieEntries)
        assertEquals(1, result.clearedWeightEntries)
        assertEquals(2000, preferencesStore.read().dailyTargetCalories)
        assertNull(sessionStore.read().activeProfile)
        assertEquals(emptyList(), entryRepository.fetchByDateRange(EntryDate(LocalDate(2026, 1, 1)), EntryDate(LocalDate(2026, 12, 31))).getOrThrow())
        assertEquals(emptyList(), weightRepository.getAll().getOrThrow())
    }
}

private class MutableEntryRepository : EntryRepository {
    private val entries = linkedMapOf<EntryId, CalorieEntry>()

    override fun create(entry: CalorieEntry): Result<CalorieEntry> {
        entries[entry.id] = entry
        return Result.success(entry)
    }

    override fun deleteById(id: EntryId): Result<Boolean> = Result.success(entries.remove(id) != null)

    override fun fetchByDateRange(startDate: EntryDate, endDate: EntryDate): Result<List<CalorieEntry>> {
        return Result.success(entries.values.toList())
    }

    override fun fetchByDate(date: EntryDate): Result<List<CalorieEntry>> = Result.success(entries.values.filter { it.date == date })
}

private class MutableWeightHistoryRepository(
    private val entries: MutableList<WeightEntry>
) : WeightHistoryRepository {
    override fun save(entry: WeightEntry): Result<WeightEntry> {
        entries += entry
        return Result.success(entry)
    }

    override fun update(entry: WeightEntry): Result<WeightEntry> = Result.success(entry)
    override fun deleteByDate(date: LocalDate): Result<Boolean> = Result.success(entries.removeAll { it.date == date })
    override fun getByDate(date: LocalDate): Result<WeightEntry?> = Result.success(entries.find { it.date == date })
    override fun getByDateRange(startDate: LocalDate, endDate: LocalDate): Result<List<WeightEntry>> = Result.success(entries.toList())
    override fun getAll(): Result<List<WeightEntry>> = Result.success(entries.toList())
}

private fun validProfileSummary(): UserProfileSummary {
    return UserProfileSummary(
        metrics = BodyMetrics(175.0, 90.0, 70.0),
        currentBmi = BmiSnapshot(29.4, BmiCategory.OVERWEIGHT),
        goalBmi = BmiSnapshot(22.9, BmiCategory.HEALTHY),
        kilogramsToLose = 20.0,
        bmiDelta = 6.5,
        goalValidation = GoalValidationResult(
            isValid = true,
            reason = GoalValidationReason.VALID,
            kilogramsToLose = 20.0,
            bmiDelta = 6.5
        )
    )
}
