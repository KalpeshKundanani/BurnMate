package org.kalpeshbkundanani.burnmate.weight.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

data class WeightEntry(
    val date: LocalDate,
    val weight: WeightValue,
    val createdAt: Instant
)
