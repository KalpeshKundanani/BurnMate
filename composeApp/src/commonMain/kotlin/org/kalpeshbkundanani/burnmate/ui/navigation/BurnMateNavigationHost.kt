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
import org.kalpeshbkundanani.burnmate.presentation.logging.DailyLoggingEvent
import org.kalpeshbkundanani.burnmate.presentation.logging.DailyLoggingViewModel
import org.kalpeshbkundanani.burnmate.presentation.onboarding.OnboardingEvent
import org.kalpeshbkundanani.burnmate.presentation.onboarding.OnboardingViewModel
import org.kalpeshbkundanani.burnmate.ui.organisms.NavigationTab
import org.kalpeshbkundanani.burnmate.ui.screens.DailyLogScreen
import org.kalpeshbkundanani.burnmate.ui.screens.DashboardScreen
import org.kalpeshbkundanani.burnmate.ui.screens.OnboardingScreen

@Composable
fun BurnMateNavigationHost(
    navController: NavHostController = rememberNavController(),
    onboardingViewModel: OnboardingViewModel,
    dashboardViewModel: DashboardViewModel,
    dailyLoggingViewModel: DailyLoggingViewModel,
    startDestination: BurnMateRoute = BurnMateRoute.Onboarding
) {
    NavHost(
        navController = navController,
        startDestination = startDestination.javaClass.simpleName
    ) {
        composable(route = BurnMateRoute.Onboarding.javaClass.simpleName) {
            val state by onboardingViewModel.uiState.collectAsState()
            
            OnboardingScreen(
                state = state,
                onEvent = { event ->
                    onboardingViewModel.onEvent(event)
                    if (event == OnboardingEvent.Submit && state.submitError == null && !state.isSubmitting) {
                        navController.navigate(BurnMateRoute.Dashboard.javaClass.simpleName) {
                            popUpTo(BurnMateRoute.Onboarding.javaClass.simpleName) { inclusive = true }
                        }
                    }
                }
            )
        }
        
        composable(route = BurnMateRoute.Dashboard.javaClass.simpleName) {
            val state by dashboardViewModel.uiState.collectAsState()
            
            DashboardScreen(
                state = state,
                onEvent = { event ->
                    if (event is DashboardEvent.OpenLogging) {
                        dailyLoggingViewModel.onEvent(DailyLoggingEvent.Load) // ensure sync
                        navController.navigate(BurnMateRoute.DailyLogging.javaClass.simpleName)
                    } else {
                        dashboardViewModel.onEvent(event)
                    }
                },
                onTabSelected = { tab ->
                    if (tab == NavigationTab.ACTIVITY) {
                        dailyLoggingViewModel.onEvent(DailyLoggingEvent.Load)
                        navController.navigate(BurnMateRoute.DailyLogging.javaClass.simpleName)
                    }
                },
                onProfileClick = { /* Not in scope */ }
            )
        }
        
        composable(route = BurnMateRoute.DailyLogging.javaClass.simpleName) {
            val state by dailyLoggingViewModel.uiState.collectAsState()
            
            DailyLogScreen(
                state = state,
                onEvent = { event -> dailyLoggingViewModel.onEvent(event) },
                onTabSelected = { tab ->
                    if (tab == NavigationTab.HOME) {
                        navController.navigate(BurnMateRoute.Dashboard.javaClass.simpleName) {
                            popUpTo(BurnMateRoute.Dashboard.javaClass.simpleName) { inclusive = true }
                        }
                    }
                }
            )
        }
    }
}
