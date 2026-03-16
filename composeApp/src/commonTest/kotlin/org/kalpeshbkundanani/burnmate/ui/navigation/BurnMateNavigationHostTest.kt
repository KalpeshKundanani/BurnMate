package org.kalpeshbkundanani.burnmate.ui.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.kalpeshbkundanani.burnmate.dashboard.domain.DashboardReadModelService
import org.kalpeshbkundanani.burnmate.dashboard.model.DashboardSnapshot
import org.kalpeshbkundanani.burnmate.dashboard.model.DebtSummary
import org.kalpeshbkundanani.burnmate.dashboard.model.TodaySummary
import org.kalpeshbkundanani.burnmate.dashboard.model.WeightSummary
import org.kalpeshbkundanani.burnmate.logging.domain.CalorieEntryFactory
import org.kalpeshbkundanani.burnmate.logging.model.CalorieAmount
import org.kalpeshbkundanani.burnmate.logging.model.CalorieEntry
import org.kalpeshbkundanani.burnmate.logging.model.EntryDate
import org.kalpeshbkundanani.burnmate.logging.model.EntryId
import org.kalpeshbkundanani.burnmate.logging.repository.EntryRepository
import org.kalpeshbkundanani.burnmate.presentation.dashboard.DashboardEvent
import org.kalpeshbkundanani.burnmate.presentation.dashboard.DashboardViewModel
import org.kalpeshbkundanani.burnmate.presentation.logging.DailyLoggingEvent
import org.kalpeshbkundanani.burnmate.presentation.logging.DailyLoggingViewModel
import org.kalpeshbkundanani.burnmate.presentation.onboarding.OnboardingSuccessEvent
import org.kalpeshbkundanani.burnmate.presentation.shared.SelectedDateCoordinator
import org.kalpeshbkundanani.burnmate.profile.model.BmiCategory
import org.kalpeshbkundanani.burnmate.profile.model.BmiSnapshot
import org.kalpeshbkundanani.burnmate.profile.model.BodyMetrics
import org.kalpeshbkundanani.burnmate.profile.model.GoalValidationReason
import org.kalpeshbkundanani.burnmate.profile.model.GoalValidationResult
import org.kalpeshbkundanani.burnmate.profile.model.UserProfileSummary
import org.kalpeshbkundanani.burnmate.ui.organisms.NavigationTab

class BurnMateNavigationHostTest {

    @Test
    fun `coordinator keeps onboarding start destination until onboarding success`() {
        val initialCoordinator = BurnMateNavigationCoordinator()

        assertEquals(BurnMateRoute.Onboarding, initialCoordinator.startDestination())

        val updatedCoordinator = initialCoordinator.applyOnboardingSuccess(
            OnboardingSuccessEvent(
                eventId = 1L,
                profileSummary = validProfileSummary()
            )
        )

        assertEquals(BurnMateRoute.Dashboard, updatedCoordinator.startDestination())
    }

    @Test
    fun `null onboarding success does not trigger navigation state change`() {
        val coordinator = BurnMateNavigationCoordinator()

        assertEquals(coordinator, coordinator.applyOnboardingSuccess(null))
    }

    @Test
    fun `bottom nav is limited to dashboard and daily logging routes`() {
        val coordinator = BurnMateNavigationCoordinator(activeProfile = validProfileSummary())

        assertEquals(BurnMateRoute.DailyLogging, coordinator.routeForTab(BurnMateRoute.Dashboard, NavigationTab.ACTIVITY))
        assertEquals(BurnMateRoute.Dashboard, coordinator.routeForTab(BurnMateRoute.DailyLogging, NavigationTab.HOME))
        assertEquals(null, coordinator.routeForTab(BurnMateRoute.Dashboard, NavigationTab.HOME))
        assertEquals(null, coordinator.routeForTab(BurnMateRoute.DailyLogging, NavigationTab.ACTIVITY))
    }

    @Test
    fun `shared selected date stays aligned across dashboard and logging flow`() {
        val initialDate = LocalDate(2026, 3, 16)
        val sharedDateCoordinator = SelectedDateCoordinator(initialDate)
        val dashboardViewModel = DashboardViewModel(
            dashboardService = FakeDashboardReadModelService(
                snapshotsByDate = mapOf(
                    initialDate to dashboardSnapshot(initialDate),
                    LocalDate(2026, 3, 15) to dashboardSnapshot(LocalDate(2026, 3, 15)),
                    LocalDate(2026, 3, 16) to dashboardSnapshot(LocalDate(2026, 3, 16))
                )
            ),
            initialDate = initialDate,
            selectedDateCoordinator = sharedDateCoordinator
        )
        val loggingViewModel = DailyLoggingViewModel(
            repository = FakeEntryRepository(),
            factory = FakeCalorieEntryFactory(),
            initialDate = initialDate,
            selectedDateCoordinator = sharedDateCoordinator
        )

        dashboardViewModel.onEvent(DashboardEvent.PreviousDayTapped)

        assertEquals(LocalDate(2026, 3, 15), dashboardViewModel.uiState.value.selectedDate)
        assertEquals(LocalDate(2026, 3, 15), loggingViewModel.uiState.value.selectedDate)

        loggingViewModel.onEvent(DailyLoggingEvent.NextDayTapped)

        assertEquals(LocalDate(2026, 3, 16), dashboardViewModel.uiState.value.selectedDate)
        assertEquals(LocalDate(2026, 3, 16), loggingViewModel.uiState.value.selectedDate)
    }
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

private class FakeDashboardReadModelService(
    private val snapshotsByDate: Map<LocalDate, DashboardSnapshot>
) : DashboardReadModelService {
    override fun getDashboardSnapshot(today: LocalDate): Result<DashboardSnapshot> {
        return Result.success(snapshotsByDate.getValue(today))
    }
}

private class FakeEntryRepository : EntryRepository {
    override fun create(entry: CalorieEntry): Result<CalorieEntry> = Result.success(entry)

    override fun deleteById(id: EntryId): Result<Boolean> = Result.success(true)

    override fun fetchByDateRange(startDate: EntryDate, endDate: EntryDate): Result<List<CalorieEntry>> {
        return Result.success(emptyList())
    }

    override fun fetchByDate(date: EntryDate): Result<List<CalorieEntry>> {
        return Result.success(emptyList())
    }
}

private class FakeCalorieEntryFactory : CalorieEntryFactory {
    override fun create(date: EntryDate, amount: CalorieAmount): Result<CalorieEntry> {
        return Result.success(
            CalorieEntry(
                id = EntryId("generated"),
                date = date,
                amount = amount,
                createdAt = Instant.parse("2026-03-16T12:00:00Z")
            )
        )
    }
}

private fun dashboardSnapshot(date: LocalDate): DashboardSnapshot {
    return DashboardSnapshot(
        snapshotDate = date,
        todaySummary = TodaySummary(
            totalIntakeCalories = 800,
            totalBurnCalories = 200,
            netCalories = 600,
            remainingCalories = 1200,
            dailyTargetCalories = 2000
        ),
        debtSummary = DebtSummary(
            currentDebtCalories = 150,
            severity = org.kalpeshbkundanani.burnmate.caloriedebt.model.CalorieDebtSeverity.LOW,
            trend = org.kalpeshbkundanani.burnmate.caloriedebt.model.CalorieDebtTrend.INCREASED
        ),
        weightSummary = WeightSummary(
            currentWeightKg = 80.0,
            goalWeightKg = 70.0,
            remainingKg = 10.0,
            progressPercentage = 50.0
        ),
        debtChartPoints = emptyList()
    )
}
