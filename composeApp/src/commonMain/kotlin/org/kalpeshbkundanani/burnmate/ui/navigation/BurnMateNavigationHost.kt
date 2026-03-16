package org.kalpeshbkundanani.burnmate.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.kalpeshbkundanani.burnmate.presentation.dashboard.DashboardEvent
import org.kalpeshbkundanani.burnmate.presentation.dashboard.DashboardViewModel
import org.kalpeshbkundanani.burnmate.presentation.logging.DailyLoggingViewModel
import org.kalpeshbkundanani.burnmate.presentation.onboarding.OnboardingViewModel
import org.kalpeshbkundanani.burnmate.ui.organisms.NavigationTab
import org.kalpeshbkundanani.burnmate.ui.screens.DailyLogScreen
import org.kalpeshbkundanani.burnmate.ui.screens.DashboardScreen
import org.kalpeshbkundanani.burnmate.ui.screens.OnboardingScreen

@Composable
fun BurnMateNavigationHost(
    navController: NavHostController = rememberNavController()
) {
    BurnMateAppRoot(navController = navController)
}

@Composable
internal fun BurnMateNavigationHost(
    navController: NavHostController,
    startDestination: BurnMateRoute,
    onboardingViewModel: OnboardingViewModel,
    dashboardViewModel: DashboardViewModel?,
    dailyLoggingViewModel: DailyLoggingViewModel,
    onDashboardEvent: (DashboardEvent) -> Unit,
    onDashboardTabSelected: (NavigationTab) -> Unit,
    onLoggingTabSelected: (NavigationTab) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = startDestination.routeName()
    ) {
        composable(route = BurnMateRoute.Onboarding.routeName()) {
            val state by onboardingViewModel.uiState.collectAsState()

            OnboardingScreen(
                state = state,
                onEvent = onboardingViewModel::onEvent
            )
        }

        composable(route = BurnMateRoute.Dashboard.routeName()) {
            val activeDashboardViewModel = dashboardViewModel ?: return@composable
            val state by activeDashboardViewModel.uiState.collectAsState()

            DashboardScreen(
                state = state,
                onEvent = onDashboardEvent,
                onTabSelected = onDashboardTabSelected,
                onProfileClick = { /* Not in scope */ }
            )
        }

        composable(route = BurnMateRoute.DailyLogging.routeName()) {
            val state by dailyLoggingViewModel.uiState.collectAsState()

            DailyLogScreen(
                state = state,
                onEvent = { event -> dailyLoggingViewModel.onEvent(event) },
                onTabSelected = onLoggingTabSelected
            )
        }
    }
}

internal fun BurnMateRoute.routeName(): String = when (this) {
    BurnMateRoute.Onboarding -> "onboarding"
    BurnMateRoute.Dashboard -> "dashboard"
    BurnMateRoute.DailyLogging -> "dailyLogging"
}
