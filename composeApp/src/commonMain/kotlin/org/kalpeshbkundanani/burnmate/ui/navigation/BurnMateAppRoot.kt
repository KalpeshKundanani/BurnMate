package org.kalpeshbkundanani.burnmate.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import org.kalpeshbkundanani.burnmate.integration.GoogleIntegrationPlatformBridge
import org.kalpeshbkundanani.burnmate.integration.model.GoogleAuthState
import org.kalpeshbkundanani.burnmate.presentation.dashboard.DashboardEvent
import org.kalpeshbkundanani.burnmate.presentation.dashboard.DashboardViewModel
import org.kalpeshbkundanani.burnmate.presentation.integration.GoogleIntegrationEvent
import org.kalpeshbkundanani.burnmate.presentation.integration.GoogleIntegrationViewModel
import org.kalpeshbkundanani.burnmate.presentation.logging.DailyLoggingEvent
import org.kalpeshbkundanani.burnmate.presentation.logging.DailyLoggingViewModel
import org.kalpeshbkundanani.burnmate.presentation.onboarding.OnboardingViewModel
import org.kalpeshbkundanani.burnmate.presentation.settings.SettingsViewModel
import org.kalpeshbkundanani.burnmate.presentation.shared.SelectedDateCoordinator
import org.kalpeshbkundanani.burnmate.settings.export.AppExportLauncher
import org.kalpeshbkundanani.burnmate.settings.export.NoOpAppExportLauncher

@Composable
internal fun BurnMateAppRoot(
    googleIntegrationBridge: GoogleIntegrationPlatformBridge = org.kalpeshbkundanani.burnmate.integration.unavailableGoogleIntegrationBridge(),
    appExportLauncher: AppExportLauncher = NoOpAppExportLauncher,
    navController: NavHostController = rememberNavController()
) {
    val dependencies = rememberBurnMateNavigationDependencies(googleIntegrationBridge, appExportLauncher)
    val selectedDateCoordinator = remember { SelectedDateCoordinator() }
    val sessionState by dependencies.appSessionStore.state.collectAsState()
    val preferences by dependencies.appPreferencesStore.state.collectAsState()
    val coordinator = remember(sessionState.activeProfile) {
        BurnMateNavigationCoordinator(activeProfile = sessionState.activeProfile)
    }
    val onboardingViewModel = viewModel { OnboardingViewModel(dependencies.profileFactory) }
    val dailyLoggingViewModel = viewModel {
        DailyLoggingViewModel(
            repository = dependencies.entryRepository,
            factory = dependencies.entryFactory,
            selectedDateCoordinator = selectedDateCoordinator
        )
    }
    val dashboardViewModel = sessionState.activeProfile?.let { profile ->
        viewModel(key = "dashboard-${profile.metrics.hashCode()}-${preferences.dailyTargetCalories}") {
            DashboardViewModel(
                dashboardService = dependencies.createDashboardService(profile),
                chartDataSource = dependencies.createChartDataSource(profile),
                chartAdapter = org.kalpeshbkundanani.burnmate.presentation.dashboard.charts.DashboardChartStateAdapter(),
                weightHistoryService = dependencies.weightHistoryService,
                selectedDateCoordinator = selectedDateCoordinator
            )
        }
    }
    val googleIntegrationViewModel = viewModel {
        GoogleIntegrationViewModel(
            authService = dependencies.googleIntegrationBridge.authService,
            permissionCoordinator = dependencies.googleIntegrationBridge.permissionCoordinator,
            fitService = dependencies.googleIntegrationBridge.fitService,
            burnImportMapper = dependencies.burnImportMapper,
            importedBurnSyncService = dependencies.importedBurnSyncService,
            initialDate = selectedDateCoordinator.selectedDate,
            selectedDateCoordinator = selectedDateCoordinator
        )
    }
    val settingsViewModel = viewModel(
        key = "settings-${preferences.dailyTargetCalories}-${sessionState.activeProfile?.metrics?.hashCode() ?: 0}"
    ) {
        SettingsViewModel(
            preferencesStore = dependencies.appPreferencesStore,
            sessionStore = dependencies.appSessionStore,
            exportCoordinator = dependencies.createAppExportCoordinator(
                integrationStatusProvider = {
                    googleIntegrationViewModel.uiState.value.message?.message ?: googleIntegrationViewModel.uiState.value.phase.name
                }
            ),
            resetCoordinator = dependencies.createAppResetCoordinator(
                integrationDisconnect = {
                    disconnectGoogleIntegration(dependencies, googleIntegrationViewModel).map { disconnected ->
                        disconnected
                    }
                }
            ),
            integrationStateProvider = { googleIntegrationViewModel.uiState.value },
            disconnectGoogle = {
                disconnectGoogleIntegration(dependencies, googleIntegrationViewModel).map { Unit }
            },
            onResetCompleted = {
                navController.navigate(coordinator.routeAfterReset().routeName()) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                }
            }
        )
    }
    val successEvent by onboardingViewModel.successEvent.collectAsState()
    val importAppliedEvent by googleIntegrationViewModel.importAppliedEvent.collectAsState()

    LaunchedEffect(successEvent?.eventId) {
        val event = successEvent ?: return@LaunchedEffect
        dependencies.appSessionStore.update { state -> state.copy(activeProfile = event.profileSummary) }
        navController.navigate(BurnMateRoute.Dashboard.routeName()) {
            popUpTo(BurnMateRoute.Onboarding.routeName()) { inclusive = true }
        }
        onboardingViewModel.consumeSuccessEvent(event.eventId)
    }

    LaunchedEffect(importAppliedEvent?.startDate, importAppliedEvent?.endDate, importAppliedEvent?.importedEntries) {
        importAppliedEvent ?: return@LaunchedEffect
        dashboardViewModel?.onEvent(DashboardEvent.Retry)
        dailyLoggingViewModel.onEvent(DailyLoggingEvent.Load)
        googleIntegrationViewModel.consumeImportAppliedEvent()
    }

    BurnMateNavigationHost(
        navController = navController,
        startDestination = coordinator.startDestination(),
        onboardingViewModel = onboardingViewModel,
        dashboardViewModel = dashboardViewModel,
        googleIntegrationViewModel = googleIntegrationViewModel,
        settingsViewModel = settingsViewModel,
        dailyLoggingViewModel = dailyLoggingViewModel,
        onDashboardEvent = { event ->
            when (event) {
                DashboardEvent.OpenLogging -> {
                    dailyLoggingViewModel.onEvent(DailyLoggingEvent.Load)
                    navController.navigate(BurnMateRoute.DailyLogging.routeName())
                }

                else -> dashboardViewModel?.onEvent(event)
            }
        },
        onIntegrationEvent = googleIntegrationViewModel::onEvent,
        onDashboardTabSelected = { tab ->
            val nextRoute = coordinator.routeForTab(BurnMateRoute.Dashboard, tab)
            if (nextRoute == BurnMateRoute.DailyLogging) {
                dailyLoggingViewModel.onEvent(DailyLoggingEvent.Load)
                navController.navigate(nextRoute.routeName())
            }
        },
        onLoggingTabSelected = { tab ->
            val nextRoute = coordinator.routeForTab(BurnMateRoute.DailyLogging, tab)
            if (nextRoute == BurnMateRoute.Dashboard) {
                dashboardViewModel?.onEvent(DashboardEvent.Retry)
                navController.navigate(nextRoute.routeName()) {
                    popUpTo(BurnMateRoute.Dashboard.routeName()) { inclusive = true }
                }
            }
        },
        onDashboardProfileClick = {
            coordinator.routeForSettings(BurnMateRoute.Dashboard)?.let { route ->
                navController.navigate(route.routeName())
            }
        },
        onSettingsBack = {
            navController.popBackStack()
        }
    )
}

private suspend fun disconnectGoogleIntegration(
    dependencies: BurnMateNavigationDependencies,
    googleIntegrationViewModel: GoogleIntegrationViewModel
): Result<Boolean> {
    val authState = dependencies.googleIntegrationBridge.authService.readCachedState()
    val signedIn = authState as? GoogleAuthState.SignedIn

    if (signedIn != null) {
        dependencies.googleIntegrationBridge.fitService.disconnect(signedIn.session).getOrElse {
            return Result.failure(it)
        }
    }

    return dependencies.googleIntegrationBridge.authService.disconnect().map {
        googleIntegrationViewModel.onEvent(GoogleIntegrationEvent.Load)
        signedIn != null
    }
}
