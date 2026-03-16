package org.kalpeshbkundanani.burnmate.dashboard.model

import kotlinx.datetime.LocalDate

data class DashboardSnapshot(
    val snapshotDate: LocalDate,
    val todaySummary: TodaySummary,
    val debtSummary: DebtSummary?,
    val weightSummary: WeightSummary?,
    val debtChartPoints: List<DebtChartPoint>
)
