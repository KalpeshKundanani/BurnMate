package org.kalpeshbkundanani.burnmate.presentation.dashboard.charts

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.kalpeshbkundanani.burnmate.dashboard.model.DebtChartPoint
import org.kalpeshbkundanani.burnmate.dashboard.model.WeightSummary
import org.kalpeshbkundanani.burnmate.weight.model.WeightEntry
import org.kalpeshbkundanani.burnmate.weight.model.WeightValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DashboardChartStateAdapterTest {
    private val adapter = DashboardChartStateAdapter()

    @Test
    fun `map handles empty lists gracefully`() {
        val state = adapter.map(
            range = ChartRangeOption.Last7Days,
            debtPoints = emptyList(),
            weightSummary = null,
            weightEntries = emptyList()
        )
        assertNull(state.debtTrend)
        assertNull(state.weeklyDeficit)
        assertNull(state.weightTrend)
        assertNull(state.progressRing)
    }

    @Test
    fun `mapDebtTrend maps and trims points correctly`() {
        val debtPoints = List(10) { i ->
            DebtChartPoint(LocalDate(2026, 3, i + 1), i * 100)
        }
        val state = adapter.map(
            range = ChartRangeOption.Last7Days,
            debtPoints = debtPoints,
            weightSummary = null,
            weightEntries = emptyList()
        )
        assertNotNull(state.debtTrend)
        assertEquals(7, state.debtTrend?.points?.size)
        assertEquals(300, state.debtTrend?.points?.first()?.cumulativeDebtCalories)
        assertEquals(900, state.debtTrend?.points?.last()?.cumulativeDebtCalories)
        assertEquals(300, state.debtTrend?.minDebtCalories)
        assertEquals(900, state.debtTrend?.maxDebtCalories)
    }

    @Test
    fun `mapWeeklyDeficit handles 8 points to produce 7 bars with correct direction`() {
        val cumulativeValues = listOf(1000, 900, 1100, 1100, 1050, 1350, 1340, 1340)
        val debtPoints = cumulativeValues.mapIndexed { index, value ->
            DebtChartPoint(LocalDate(2026, 3, index + 1), value)
        }
        
        val state = adapter.map(
            range = ChartRangeOption.Last7Days,
            debtPoints = debtPoints,
            weightSummary = null,
            weightEntries = emptyList()
        )
        assertNotNull(state.weeklyDeficit)
        val bars = state.weeklyDeficit?.bars!!
        assertEquals(7, bars.size)
        
        assertEquals(-100, bars[0].deltaCalories)
        assertEquals(DailyBalanceDirection.Deficit, bars[0].direction)
        
        assertEquals(200, bars[1].deltaCalories)
        assertEquals(DailyBalanceDirection.Surplus, bars[1].direction)
        
        assertEquals(0, bars[2].deltaCalories)
        assertEquals(DailyBalanceDirection.Neutral, bars[2].direction)
        
        assertEquals(300, state.weeklyDeficit?.maxMagnitudeCalories)
    }

    @Test
    fun `mapWeightTrend defaults to latest createdAt for same date`() {
        val weightEntries = listOf(
            WeightEntry(LocalDate(2026, 3, 1), WeightValue(75.5), Instant.parse("2026-03-01T10:00:00Z")),
            WeightEntry(LocalDate(2026, 3, 1), WeightValue(75.2), Instant.parse("2026-03-01T12:00:00Z")),
            WeightEntry(LocalDate(2026, 3, 2), WeightValue(74.8), Instant.parse("2026-03-02T10:00:00Z"))
        )
        
        val state = adapter.map(
            range = ChartRangeOption.Last7Days,
            debtPoints = emptyList(),
            weightSummary = null,
            weightEntries = weightEntries
        )
        
        assertNotNull(state.weightTrend)
        assertEquals(2, state.weightTrend?.points?.size)
        assertEquals(75.2, state.weightTrend?.points?.get(0)?.weightKg)
        assertEquals(74.8, state.weightTrend?.points?.get(1)?.weightKg)
    }

    @Test
    fun `mapProgressRing maps correctly for goal reached and unreached`() {
        val summaryNotReached = WeightSummary(
            currentWeightKg = 75.0,
            goalWeightKg = 70.0,
            remainingKg = 5.0,
            progressPercentage = 50.0
        )
        
        val state1 = adapter.map(
            range = ChartRangeOption.Last7Days,
            debtPoints = emptyList(),
            weightSummary = summaryNotReached,
            weightEntries = emptyList()
        )
        
        assertNotNull(state1.progressRing)
        assertEquals(0.5f, state1.progressRing?.progressFraction)
        assertFalse(state1.progressRing?.isGoalReached ?: true)
        assertEquals("5.0 kg to goal", state1.progressRing?.supportingLabel)
        
        val summaryReached = WeightSummary(
            currentWeightKg = 69.5,
            goalWeightKg = 70.0,
            remainingKg = 0.0,
            progressPercentage = 100.0
        )
        
        val state2 = adapter.map(
            range = ChartRangeOption.Last7Days,
            debtPoints = emptyList(),
            weightSummary = summaryReached,
            weightEntries = emptyList()
        )
        
        assertNotNull(state2.progressRing)
        assertEquals(1.0f, state2.progressRing?.progressFraction)
        assertTrue(state2.progressRing?.isGoalReached ?: false)
        assertEquals("Goal reached", state2.progressRing?.supportingLabel)
    }
}
