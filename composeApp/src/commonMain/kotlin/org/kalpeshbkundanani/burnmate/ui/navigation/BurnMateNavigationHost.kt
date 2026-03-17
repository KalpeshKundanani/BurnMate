package org.kalpeshbkundanani.burnmate.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.kalpeshbkundanani.burnmate.integration.GoogleIntegrationPlatformBridge
import org.kalpeshbkundanani.burnmate.presentation.dashboard.DashboardEvent
import org.kalpeshbkundanani.burnmate.presentation.dashboard.DashboardViewModel
import org.kalpeshbkundanani.burnmate.presentation.integration.GoogleIntegrationEvent
import org.kalpeshbkundanani.burnmate.presentation.integration.GoogleIntegrationViewModel
import org.kalpeshbkundanani.burnmate.presentation.logging.DailyLoggingViewModel
import org.kalpeshbkundanani.burnmate.presentation.onboarding.OnboardingViewModel
import org.kalpeshbkundanani.burnmate.presentation.settings.SettingsViewModel
import org.kalpeshbkundanani.burnmate.settings.export.AppExportLauncher
import org.kalpeshbkundanani.burnmate.settings.export.NoOpAppExportLauncher
import org.kalpeshbkundanani.burnmate.ui.organisms.NavigationTab
import org.kalpeshbkundanani.burnmate.ui.screens.DailyLogScreen
import org.kalpeshbkundanani.burnmate.ui.screens.DashboardScreen
import org.kalpeshbkundanani.burnmate.ui.screens.OnboardingScreen
import org.kalpeshbkundanani.burnmate.ui.screens.SettingsScreen

@Composable
fun BurnMateNavigationHost(
    googleIntegrationBridge: GoogleIntegrationPlatformBridge = org.kalpeshbkundanani.burnmate.integration.unavailableGoogleIntegrationBridge(),
    appExportLauncher: AppExportLauncher = NoOpAppExportLauncher,
    navController: NavHostController = rememberNavController()
) {
    BurnMateAppRoot(
        googleIntegrationBridge = googleIntegrationBridge,
        appExportLauncher = appExportLauncher,
        navController = navController
    )
}

@Composable
internal fun BurnMateNavigationHost(
    navController: NavHostController,
    startDestination: BurnMateRoute,
    onboardingViewModel: OnboardingViewModel,
    dashboardViewModel: DashboardViewModel?,
    googleIntegrationViewModel: GoogleIntegrationViewModel,
    settingsViewModel: SettingsViewModel,
    dailyLoggingViewModel: DailyLoggingViewModel,
    onDashboardEvent: (DashboardEvent) -> Unit,
    onIntegrationEvent: (GoogleIntegrationEvent) -> Unit,
    onDashboardTabSelected: (NavigationTab) -> Unit,
    onLoggingTabSelected: (NavigationTab) -> Unit,
    onDashboardProfileClick: () -> Unit,
    onSettingsBack: () -> Unit
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
            val integrationState by googleIntegrationViewModel.uiState.collectAsState()

            LaunchedEffect(state.selectedDate) {
                googleIntegrationViewModel.onEvent(GoogleIntegrationEvent.Load)
            }

            DashboardScreen(
                state = state,
                integrationState = integrationState,
                onEvent = onDashboardEvent,
                onIntegrationEvent = onIntegrationEvent,
                onTabSelected = onDashboardTabSelected,
                onProfileClick = onDashboardProfileClick
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

        composable(route = BurnMateRoute.Settings.routeName()) {
            val state by settingsViewModel.uiState.collectAsState()

            SettingsScreen(
                state = state,
                onEvent = settingsViewModel::onEvent,
                onBack = onSettingsBack
            )
        }
    }
}

internal fun BurnMateRoute.routeName(): String = when (this) {
    BurnMateRoute.Onboarding -> "onboarding"
    BurnMateRoute.Dashboard -> "dashboard"
    BurnMateRoute.DailyLogging -> "dailyLogging"
    BurnMateRoute.Settings -> "settings"
}
