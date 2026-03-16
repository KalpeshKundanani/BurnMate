package org.kalpeshbkundanani.burnmate.dashboard.model

data class WeightSummary(
    val currentWeightKg: Double,
    val goalWeightKg: Double,
    val remainingKg: Double,
    val progressPercentage: Double
)
