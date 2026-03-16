package org.kalpeshbkundanani.burnmate.presentation.dashboard

import org.kalpeshbkundanani.burnmate.dashboard.model.DashboardSnapshot

class DashboardUiMapper {
    
    data class MappedCards(
        val todayCard: DashboardTodayCardState?,
        val debtCard: DashboardDebtCardState?,
        val weightCard: DashboardWeightCardState?
    )
    
    fun mapToCards(snapshot: DashboardSnapshot): MappedCards {
        val todaySum = snapshot.todaySummary
        val todayCard = DashboardTodayCardState(
            formattedCurrentDeficit = formatDeficit(todaySum.netCalories),
            formattedProgressVsYesterday = "${todaySum.remainingCalories} kcal left",
            deficitValue = todaySum.netCalories
        )
        
        val debtSum = snapshot.debtSummary
        val debtCard = if (debtSum != null) {
            DashboardDebtCardState(
                formattedWeeklyNet = formatDeficit(debtSum.currentDebtCalories),
                formattedComparisonStat = "Trend: ${debtSum.trend.name}"
            )
        } else null
        
        val wSum = snapshot.weightSummary
        val weightCard = if (wSum != null) {
            DashboardWeightCardState(
                formattedCurrentWeight = "${wSum.currentWeightKg} kg",
                formattedGoalWeight = "${wSum.goalWeightKg} kg",
                formattedProgress = "${wSum.progressPercentage}%"
            )
        } else null
        
        return MappedCards(todayCard, debtCard, weightCard)
    }
    
    private fun formatDeficit(kcal: Int): String {
        return if (kcal > 0) "+$kcal kcal" else "$kcal kcal"
    }

    private fun formatPercent(percent: Int): String {
        return if (percent > 0) "+$percent%" else "$percent%"
    }
}
