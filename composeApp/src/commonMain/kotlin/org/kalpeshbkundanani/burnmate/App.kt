package org.kalpeshbkundanani.burnmate

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import org.kalpeshbkundanani.burnmate.dashboard.domain.DefaultDashboardReadModelService
import org.kalpeshbkundanani.burnmate.logging.domain.DefaultCalorieEntryFactory
import org.kalpeshbkundanani.burnmate.logging.domain.DefaultCalorieEntryValidator
import org.kalpeshbkundanani.burnmate.logging.repository.LocalEntryRepository
import org.kalpeshbkundanani.burnmate.presentation.dashboard.DashboardViewModel
import org.kalpeshbkundanani.burnmate.presentation.logging.DailyLoggingViewModel
import org.kalpeshbkundanani.burnmate.presentation.onboarding.OnboardingViewModel
import org.kalpeshbkundanani.burnmate.profile.domain.DefaultBmiCalculator
import org.kalpeshbkundanani.burnmate.profile.domain.DefaultHealthyGoalValidator
import org.kalpeshbkundanani.burnmate.profile.domain.DefaultProfileMetricsValidator
import org.kalpeshbkundanani.burnmate.profile.domain.DefaultUserProfileFactory
import org.kalpeshbkundanani.burnmate.ui.navigation.BurnMateNavigationHost
import org.kalpeshbkundanani.burnmate.ui.theme.BurnMateTheme

@Composable
fun App() {
    BurnMateTheme {
        // Since we are wiring without a DI framework for SLICE-0007 per scope:
        val profileFactory = DefaultUserProfileFactory()
        
        val onboardingViewModel = viewModel { OnboardingViewModel(profileFactory) }
        
        // Mock minimal wiring since SLICE-0007 is UI-only
        val entryRepo = LocalEntryRepository()
        val entryFactory = DefaultCalorieEntryFactory(DefaultCalorieEntryValidator())
        val dashboardService = object : org.kalpeshbkundanani.burnmate.dashboard.domain.DashboardReadModelService {
            override fun getDashboardSnapshot(today: kotlinx.datetime.LocalDate): Result<org.kalpeshbkundanani.burnmate.dashboard.model.DashboardSnapshot> {
                return Result.success(org.kalpeshbkundanani.burnmate.dashboard.model.DashboardSnapshot(
                    snapshotDate = today,
                    todaySummary = org.kalpeshbkundanani.burnmate.dashboard.model.TodaySummary(0, 0, 0, 0, 2000),
                    debtSummary = null,
                    weightSummary = null,
                    debtChartPoints = emptyList()
                ))
            }
        }
        
        val dashboardViewModel = viewModel { DashboardViewModel(dashboardService) }
        val dailyLoggingViewModel = viewModel { DailyLoggingViewModel(entryRepo, entryFactory) }

        BurnMateNavigationHost(
            onboardingViewModel = onboardingViewModel,
            dashboardViewModel = dashboardViewModel,
            dailyLoggingViewModel = dailyLoggingViewModel
        )
    }
}