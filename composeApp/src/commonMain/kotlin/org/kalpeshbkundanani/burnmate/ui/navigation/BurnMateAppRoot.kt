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
import org.kalpeshbkundanani.burnmate.presentation.dashboard.DashboardEvent
import org.kalpeshbkundanani.burnmate.presentation.dashboard.DashboardViewModel
import org.kalpeshbkundanani.burnmate.presentation.logging.DailyLoggingEvent
import org.kalpeshbkundanani.burnmate.presentation.logging.DailyLoggingViewModel
import org.kalpeshbkundanani.burnmate.presentation.onboarding.OnboardingViewModel
import org.kalpeshbkundanani.burnmate.presentation.shared.SelectedDateCoordinator

@Composable
internal fun BurnMateAppRoot(
    navController: NavHostController = rememberNavController()
) {
    val dependencies = rememberBurnMateNavigationDependencies()
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
                selectedDateCoordinator = selectedDateCoordinator
            )
        }
    }
    val successEvent by onboardingViewModel.successEvent.collectAsState()

    LaunchedEffect(successEvent?.eventId) {
        val event = successEvent ?: return@LaunchedEffect
        coordinator = coordinator.applyOnboardingSuccess(event)
        navController.navigate(BurnMateRoute.Dashboard.routeName()) {
            popUpTo(BurnMateRoute.Onboarding.routeName()) { inclusive = true }
        }
        onboardingViewModel.consumeSuccessEvent(event.eventId)
    }

    BurnMateNavigationHost(
        navController = navController,
        startDestination = coordinator.startDestination(),
        onboardingViewModel = onboardingViewModel,
        dashboardViewModel = dashboardViewModel,
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
