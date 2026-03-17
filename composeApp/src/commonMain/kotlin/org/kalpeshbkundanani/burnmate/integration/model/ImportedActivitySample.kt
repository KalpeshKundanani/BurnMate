package org.kalpeshbkundanani.burnmate.integration.model

import kotlinx.datetime.LocalDate

data class ImportedActivitySample(
    val date: LocalDate,
    val stepCount: Int?,
    val activeCalories: Int?,
    val source: String = "google_fit"
)
