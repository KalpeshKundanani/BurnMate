package org.kalpeshbkundanani.burnmate.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import org.kalpeshbkundanani.burnmate.integration.GoogleIntegrationPlatformBridge
import org.kalpeshbkundanani.burnmate.presentation.dashboard.DashboardEvent
import org.kalpeshbkundanani.burnmate.presentation.dashboard.DashboardViewModel
import org.kalpeshbkundanani.burnmate.presentation.integration.GoogleIntegrationViewModel
import org.kalpeshbkundanani.burnmate.presentation.logging.DailyLoggingEvent
import org.kalpeshbkundanani.burnmate.presentation.logging.DailyLoggingViewModel
import org.kalpeshbkundanani.burnmate.presentation.onboarding.OnboardingViewModel
import org.kalpeshbkundanani.burnmate.presentation.shared.SelectedDateCoordinator

@Composable
internal fun BurnMateAppRoot(
    googleIntegrationBridge: GoogleIntegrationPlatformBridge = org.kalpeshbkundanani.burnmate.integration.unavailableGoogleIntegrationBridge(),
    navController: NavHostController = rememberNavController()
) {
    val dependencies = rememberBurnMateNavigationDependencies(googleIntegrationBridge)
    val selectedDateCoordinator = remember { SelectedDateCoordinator() }
    var coordinator by remember { mutableStateOf(BurnMateNavigationCoordinator()) }
    val onboardingViewModel = viewModel { OnboardingViewModel(dependencies.profileFactory) }
    val dailyLoggingViewModel = viewModel {
        DailyLoggingViewModel(
            repository = dependencies.entryRepository,
            factory = dependencies.entryFactory,
            selectedDateCoordinator = selectedDateCoordinator
        )
    }
    val dashboardViewModel = coordinator.activeProfile?.let { profile ->
        viewModel(key = "dashboard-${profile.metrics.hashCode()}") {
            DashboardViewModel(
                dashboardService = dependencies.createDashboardService(profile),
                chartDataSource = dependencies.createChartDataSource(profile),
                chartAdapter = org.kalpeshbkundanani.burnmate.presentation.dashboard.charts.DashboardChartStateAdapter(),
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
    val successEvent by onboardingViewModel.successEvent.collectAsState()
    val importAppliedEvent by googleIntegrationViewModel.importAppliedEvent.collectAsState()

    LaunchedEffect(successEvent?.eventId) {
        val event = successEvent ?: return@LaunchedEffect
        coordinator = coordinator.applyOnboardingSuccess(event)
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
                navController.navigate(nextRoute.routeName()) {
                    popUpTo(BurnMateRoute.Dashboard.routeName()) { inclusive = true }
                }
            }
        }
    )
}
