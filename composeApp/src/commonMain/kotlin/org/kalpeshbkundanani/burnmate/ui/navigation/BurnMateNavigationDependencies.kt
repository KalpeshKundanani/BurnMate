package org.kalpeshbkundanani.burnmate.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.kalpeshbkundanani.burnmate.caloriedebt.domain.DefaultCalorieDebtCalculator
import org.kalpeshbkundanani.burnmate.dashboard.domain.DashboardReadModelService
import org.kalpeshbkundanani.burnmate.dashboard.domain.DefaultDashboardReadModelService
import org.kalpeshbkundanani.burnmate.logging.domain.DefaultCalorieEntryFactory
import org.kalpeshbkundanani.burnmate.logging.domain.DefaultCalorieEntryValidator
import org.kalpeshbkundanani.burnmate.logging.repository.EntryRepository
import org.kalpeshbkundanani.burnmate.logging.repository.LocalEntryRepository
import org.kalpeshbkundanani.burnmate.profile.domain.DefaultUserProfileFactory
import org.kalpeshbkundanani.burnmate.profile.domain.UserProfileFactory
import org.kalpeshbkundanani.burnmate.profile.model.UserProfileSummary
import org.kalpeshbkundanani.burnmate.weight.domain.DefaultWeightHistoryService
import org.kalpeshbkundanani.burnmate.weight.domain.WeightHistoryService
import org.kalpeshbkundanani.burnmate.weight.repository.LocalWeightRepository

internal data class BurnMateNavigationDependencies(
    val profileFactory: UserProfileFactory,
    val entryRepository: EntryRepository,
    val entryFactory: DefaultCalorieEntryFactory,
    val weightHistoryService: WeightHistoryService,
    val dailyTargetCalories: Int = DEFAULT_DAILY_TARGET_CALORIES
) {
    fun createDashboardService(
        profileSummary: UserProfileSummary,
        chartWindowDays: Int = DefaultDashboardReadModelService.DEFAULT_CHART_WINDOW_DAYS
    ): DashboardReadModelService {
        return DefaultDashboardReadModelService(
            entryRepository = entryRepository,
            debtCalculator = DefaultCalorieDebtCalculator(),
            weightHistoryService = weightHistoryService,
            bodyMetrics = profileSummary.metrics,
            dailyTargetCalories = dailyTargetCalories,
            chartWindowDays = chartWindowDays
        )
    }

    fun createChartDataSource(profileSummary: UserProfileSummary): org.kalpeshbkundanani.burnmate.presentation.dashboard.charts.DashboardChartDataSource {
        return org.kalpeshbkundanani.burnmate.presentation.dashboard.charts.DefaultDashboardChartDataSource(
            dashboardServiceFactory = { days -> createDashboardService(profileSummary, chartWindowDays = days) },
            weightHistoryService = weightHistoryService
        )
    }
}

@Composable
internal fun rememberBurnMateNavigationDependencies(): BurnMateNavigationDependencies {
    return remember {
        val entryRepository = LocalEntryRepository()
        BurnMateNavigationDependencies(
            profileFactory = DefaultUserProfileFactory(),
            entryRepository = entryRepository,
            entryFactory = DefaultCalorieEntryFactory(DefaultCalorieEntryValidator()),
            weightHistoryService = DefaultWeightHistoryService(repository = LocalWeightRepository())
        )
    }
}

internal data class BurnMateNavigationCoordinator(
    val activeProfile: UserProfileSummary? = null
) {
    fun startDestination(): BurnMateRoute {
        return if (activeProfile == null) BurnMateRoute.Onboarding else BurnMateRoute.Dashboard
    }

    fun applyOnboardingSuccess(event: org.kalpeshbkundanani.burnmate.presentation.onboarding.OnboardingSuccessEvent?): BurnMateNavigationCoordinator {
        return if (event == null) this else copy(activeProfile = event.profileSummary)
    }

    fun routeForTab(currentRoute: BurnMateRoute, tab: org.kalpeshbkundanani.burnmate.ui.organisms.NavigationTab): BurnMateRoute? {
        return when (tab) {
            org.kalpeshbkundanani.burnmate.ui.organisms.NavigationTab.HOME -> {
                if (currentRoute == BurnMateRoute.Dashboard) null else BurnMateRoute.Dashboard
            }
            org.kalpeshbkundanani.burnmate.ui.organisms.NavigationTab.ACTIVITY -> {
                if (currentRoute == BurnMateRoute.DailyLogging) null else BurnMateRoute.DailyLogging
            }
        }
    }
}

internal const val DEFAULT_DAILY_TARGET_CALORIES: Int = 2000
