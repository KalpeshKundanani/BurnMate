package org.kalpeshbkundanani.burnmate.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import org.kalpeshbkundanani.burnmate.caloriedebt.domain.DefaultCalorieDebtCalculator
import org.kalpeshbkundanani.burnmate.dashboard.domain.DashboardReadModelService
import org.kalpeshbkundanani.burnmate.dashboard.domain.DefaultDashboardReadModelService
import org.kalpeshbkundanani.burnmate.integration.GoogleIntegrationPlatformBridge
import org.kalpeshbkundanani.burnmate.integration.mapping.BurnImportMapper
import org.kalpeshbkundanani.burnmate.integration.mapping.DefaultBurnImportMapper
import org.kalpeshbkundanani.burnmate.integration.sync.DefaultImportedBurnSyncService
import org.kalpeshbkundanani.burnmate.integration.sync.ImportedBurnSyncService
import org.kalpeshbkundanani.burnmate.logging.domain.DefaultCalorieEntryFactory
import org.kalpeshbkundanani.burnmate.logging.domain.DefaultCalorieEntryValidator
import org.kalpeshbkundanani.burnmate.logging.model.EntryDate
import org.kalpeshbkundanani.burnmate.logging.repository.EntryRepository
import org.kalpeshbkundanani.burnmate.logging.repository.LocalEntryRepository
import org.kalpeshbkundanani.burnmate.profile.domain.DefaultUserProfileFactory
import org.kalpeshbkundanani.burnmate.profile.domain.UserProfileFactory
import org.kalpeshbkundanani.burnmate.profile.model.UserProfileSummary
import org.kalpeshbkundanani.burnmate.settings.export.AppExportCoordinator
import org.kalpeshbkundanani.burnmate.settings.export.AppExportLauncher
import org.kalpeshbkundanani.burnmate.settings.export.DefaultAppExportCoordinator
import org.kalpeshbkundanani.burnmate.settings.export.NoOpAppExportLauncher
import org.kalpeshbkundanani.burnmate.settings.preferences.AppPreferencesStore
import org.kalpeshbkundanani.burnmate.settings.preferences.InMemoryAppPreferencesStore
import org.kalpeshbkundanani.burnmate.settings.reset.AppResetCoordinator
import org.kalpeshbkundanani.burnmate.settings.reset.DefaultAppResetCoordinator
import org.kalpeshbkundanani.burnmate.settings.state.AppSessionStore
import org.kalpeshbkundanani.burnmate.settings.state.InMemoryAppSessionStore
import org.kalpeshbkundanani.burnmate.weight.domain.DefaultWeightHistoryService
import org.kalpeshbkundanani.burnmate.weight.domain.WeightHistoryService
import org.kalpeshbkundanani.burnmate.weight.repository.LocalWeightRepository
import org.kalpeshbkundanani.burnmate.weight.repository.WeightHistoryRepository

internal data class BurnMateNavigationDependencies(
    val profileFactory: UserProfileFactory,
    val entryRepository: EntryRepository,
    val entryFactory: DefaultCalorieEntryFactory,
    val weightHistoryRepository: WeightHistoryRepository,
    val weightHistoryService: WeightHistoryService,
    val appPreferencesStore: AppPreferencesStore,
    val appSessionStore: AppSessionStore,
    val googleIntegrationBridge: GoogleIntegrationPlatformBridge = org.kalpeshbkundanani.burnmate.integration.unavailableGoogleIntegrationBridge(),
    val burnImportMapper: BurnImportMapper = DefaultBurnImportMapper(),
    val importedBurnSyncService: ImportedBurnSyncService = DefaultImportedBurnSyncService(entryRepository),
    val appExportLauncher: AppExportLauncher = NoOpAppExportLauncher
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
            dailyTargetCalories = appPreferencesStore.read().dailyTargetCalories,
            chartWindowDays = chartWindowDays
        )
    }

    fun createChartDataSource(profileSummary: UserProfileSummary): org.kalpeshbkundanani.burnmate.presentation.dashboard.charts.DashboardChartDataSource {
        return org.kalpeshbkundanani.burnmate.presentation.dashboard.charts.DefaultDashboardChartDataSource(
            dashboardServiceFactory = { days -> createDashboardService(profileSummary, chartWindowDays = days) },
            weightHistoryService = weightHistoryService
        )
    }

    fun createAppExportCoordinator(
        integrationStatusProvider: () -> String?
    ): AppExportCoordinator {
        return DefaultAppExportCoordinator(
            sessionStore = appSessionStore,
            preferencesStore = appPreferencesStore,
            entryRepository = entryRepository,
            weightRepository = weightHistoryRepository,
            integrationStatusProvider = integrationStatusProvider,
            exportLauncher = appExportLauncher,
            nowProvider = { Clock.System.now() },
            entryDateRangeProvider = ::appEntryDateRange
        )
    }

    fun createAppResetCoordinator(
        integrationDisconnect: suspend () -> Result<Boolean>
    ): AppResetCoordinator {
        return DefaultAppResetCoordinator(
            sessionStore = appSessionStore,
            preferencesStore = appPreferencesStore,
            entryRepository = entryRepository,
            weightRepository = weightHistoryRepository,
            integrationDisconnect = integrationDisconnect,
            dateRangeProvider = ::appEntryDateRange,
            weightDatesProvider = {
                weightHistoryRepository.getAll().map { entries -> entries.map { it.date } }
            }
        )
    }
}

@Composable
internal fun rememberBurnMateNavigationDependencies(
    googleIntegrationBridge: GoogleIntegrationPlatformBridge = org.kalpeshbkundanani.burnmate.integration.unavailableGoogleIntegrationBridge(),
    appExportLauncher: AppExportLauncher = NoOpAppExportLauncher,
    appPreferencesStore: AppPreferencesStore = InMemoryAppPreferencesStore(),
    appSessionStore: AppSessionStore = InMemoryAppSessionStore()
): BurnMateNavigationDependencies {
    return remember(googleIntegrationBridge, appExportLauncher, appPreferencesStore, appSessionStore) {
        val entryRepository = LocalEntryRepository()
        val weightHistoryRepository = LocalWeightRepository()
        BurnMateNavigationDependencies(
            profileFactory = DefaultUserProfileFactory(),
            entryRepository = entryRepository,
            entryFactory = DefaultCalorieEntryFactory(DefaultCalorieEntryValidator()),
            weightHistoryRepository = weightHistoryRepository,
            weightHistoryService = DefaultWeightHistoryService(repository = weightHistoryRepository),
            appPreferencesStore = appPreferencesStore,
            appSessionStore = appSessionStore,
            googleIntegrationBridge = googleIntegrationBridge,
            burnImportMapper = DefaultBurnImportMapper(),
            importedBurnSyncService = DefaultImportedBurnSyncService(entryRepository),
            appExportLauncher = appExportLauncher
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

    fun routeForSettings(currentRoute: BurnMateRoute): BurnMateRoute? {
        return if (currentRoute == BurnMateRoute.Dashboard) BurnMateRoute.Settings else null
    }

    fun routeAfterReset(): BurnMateRoute = BurnMateRoute.Onboarding
}

internal const val DEFAULT_DAILY_TARGET_CALORIES: Int = 2000

private fun appEntryDateRange(): Pair<EntryDate, EntryDate> {
    return EntryDate(LocalDate(1970, 1, 1)) to EntryDate(LocalDate(2100, 12, 31))
}
