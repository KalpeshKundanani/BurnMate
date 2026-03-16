package org.kalpeshbkundanani.burnmate.ui.navigation

sealed interface BurnMateRoute {
    data object Onboarding : BurnMateRoute
    data object Dashboard : BurnMateRoute
    data object DailyLogging : BurnMateRoute
}
