package org.kalpeshbkundanani.burnmate.settings.export

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

class DefaultAppExportCoordinatorTest {

    @Test
    fun `T-05 assembles deterministic snapshot ordering`() {
        val launcher = RecordingExportLauncher()
        val coordinator = DefaultAppExportCoordinator(
            sessionStore = InMemoryAppSessionStore(AppSessionState(activeProfile = validProfileSummary())),
            preferencesStore = InMemoryAppPreferencesStore(AppPreferences(dailyTargetCalories = 2200)),
            entryRepository = FakeEntryRepository(
                listOf(
                    calorieEntry("c2", "2026-03-17T11:00:00Z", 500, LocalDate(2026, 3, 17)),
                    calorieEntry("c1", "2026-03-16T10:00:00Z", 300, LocalDate(2026, 3, 16)),
                    calorieEntry("c3", "2026-03-17T11:00:00Z", 450, LocalDate(2026, 3, 17))
                )
            ),
            weightRepository = FakeWeightHistoryRepository(
                listOf(
                    weightEntry(LocalDate(2026, 3, 17), 80.0),
                    weightEntry(LocalDate(2026, 3, 15), 81.0)
                )
            ),
            integrationStatusProvider = { "Google Fit connected" },
            exportLauncher = launcher,
            nowProvider = { Instant.parse("2026-03-17T10:00:00Z") },
            entryDateRangeProvider = { EntryDate(LocalDate(2026, 1, 1)) to EntryDate(LocalDate(2026, 12, 31)) }
        )

        val snapshot = runBlocking { coordinator.export().getOrThrow() }

        assertEquals(listOf("c1", "c2", "c3"), snapshot.calorieEntries.map { it.id.value })
        assertEquals(listOf(LocalDate(2026, 3, 15), LocalDate(2026, 3, 17)), snapshot.weightEntries.map { it.date })
        assertEquals("Google Fit connected", snapshot.integrationSummary)
        assertEquals(snapshot, launcher.lastSnapshot)
    }

    @Test
    fun `export launcher failure leaves source state unchanged`() {
        val sessionStore = InMemoryAppSessionStore(AppSessionState(activeProfile = validProfileSummary()))
        val preferencesStore = InMemoryAppPreferencesStore(AppPreferences(dailyTargetCalories = 2100))
        val entries = listOf(calorieEntry("c1", "2026-03-16T10:00:00Z", 300, LocalDate(2026, 3, 16)))
        val weights = listOf(weightEntry(LocalDate(2026, 3, 17), 80.0))
        val entryRepository = FakeEntryRepository(entries)
        val weightRepository = FakeWeightHistoryRepository(weights)
        val coordinator = DefaultAppExportCoordinator(
            sessionStore = sessionStore,
            preferencesStore = preferencesStore,
            entryRepository = entryRepository,
            weightRepository = weightRepository,
            integrationStatusProvider = { "Signed out" },
            exportLauncher = object : AppExportLauncher {
                override suspend fun launch(snapshot: AppExportSnapshot): Result<Unit> {
                    return Result.failure(IllegalStateException("launcher failed"))
                }
            },
            nowProvider = { Instant.parse("2026-03-17T10:00:00Z") },
            entryDateRangeProvider = { EntryDate(LocalDate(2026, 1, 1)) to EntryDate(LocalDate(2026, 12, 31)) }
        )

        val result = runBlocking { coordinator.export() }

        assertEquals("Failed to hand off export", result.exceptionOrNull()?.message)
        assertEquals(2100, preferencesStore.read().dailyTargetCalories)
        assertEquals(validProfileSummary(), sessionStore.read().activeProfile)
        assertEquals(entries, entryRepository.fetchByDateRange(EntryDate(LocalDate(2026, 1, 1)), EntryDate(LocalDate(2026, 12, 31))).getOrThrow())
        assertEquals(weights, weightRepository.getAll().getOrThrow())
        assertNull(result.getOrNull())
    }
}

private class RecordingExportLauncher : AppExportLauncher {
    var lastSnapshot: AppExportSnapshot? = null

    override suspend fun launch(snapshot: AppExportSnapshot): Result<Unit> {
        lastSnapshot = snapshot
        return Result.success(Unit)
    }
}

private class FakeEntryRepository(
    private val entries: List<CalorieEntry>
) : EntryRepository {
    override fun create(entry: CalorieEntry): Result<CalorieEntry> = Result.success(entry)

    override fun deleteById(id: EntryId): Result<Boolean> = Result.success(true)

    override fun fetchByDateRange(startDate: EntryDate, endDate: EntryDate): Result<List<CalorieEntry>> {
        return Result.success(entries)
    }

    override fun fetchByDate(date: EntryDate): Result<List<CalorieEntry>> = Result.success(entries.filter { it.date == date })
}

private class FakeWeightHistoryRepository(
    private val entries: List<WeightEntry>
) : WeightHistoryRepository {
    override fun save(entry: WeightEntry): Result<WeightEntry> = Result.success(entry)
    override fun update(entry: WeightEntry): Result<WeightEntry> = Result.success(entry)
    override fun deleteByDate(date: LocalDate): Result<Boolean> = Result.success(true)
    override fun getByDate(date: LocalDate): Result<WeightEntry?> = Result.success(entries.find { it.date == date })
    override fun getByDateRange(startDate: LocalDate, endDate: LocalDate): Result<List<WeightEntry>> = Result.success(entries)
    override fun getAll(): Result<List<WeightEntry>> = Result.success(entries)
}

private fun calorieEntry(id: String, createdAt: String, amount: Int, date: LocalDate): CalorieEntry {
    return CalorieEntry(
        id = EntryId(id),
        date = EntryDate(date),
        amount = CalorieAmount(amount),
        createdAt = Instant.parse(createdAt)
    )
}

private fun weightEntry(date: LocalDate, weight: Double): WeightEntry {
    return WeightEntry(
        date = date,
        weight = WeightValue(weight),
        createdAt = Instant.parse("2026-03-17T10:00:00Z")
    )
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
