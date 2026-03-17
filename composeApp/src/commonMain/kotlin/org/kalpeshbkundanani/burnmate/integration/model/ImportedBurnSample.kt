package org.kalpeshbkundanani.burnmate.integration.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

data class ImportedBurnSample(
    val entryId: String,
    val date: LocalDate,
    val burnCalories: Int,
    val createdAt: Instant,
    val source: String = "google_fit"
)
