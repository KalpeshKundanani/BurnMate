package org.kalpeshbkundanani.burnmate.ui.organisms

import kotlin.test.Test
import kotlin.test.assertEquals
import org.kalpeshbkundanani.burnmate.presentation.dashboard.charts.DailyBalanceDirection
import org.kalpeshbkundanani.burnmate.presentation.dashboard.charts.WeeklyDeficitBar
import org.kalpeshbkundanani.burnmate.presentation.dashboard.charts.WeeklyDeficitChartState
import kotlinx.datetime.LocalDate

class DashboardVisualProgressSectionTest {
    @Test
    fun `weekly deficit null uses explicit empty-state message`() {
        assertEquals(WEEKLY_DEFICIT_EMPTY_MESSAGE, weeklyDeficitEmptyMessage(null))
    }

    @Test
    fun `weekly deficit present does not use empty-state message`() {
        val state = WeeklyDeficitChartState(
            bars = listOf(
                WeeklyDeficitBar(
                    date = LocalDate(2026, 3, 17),
                    deltaCalories = -100,
                    direction = DailyBalanceDirection.Deficit,
                    label = "MAR 17"
                )
            ),
            maxMagnitudeCalories = 100
        )

        assertEquals("", weeklyDeficitEmptyMessage(state))
    }
}
