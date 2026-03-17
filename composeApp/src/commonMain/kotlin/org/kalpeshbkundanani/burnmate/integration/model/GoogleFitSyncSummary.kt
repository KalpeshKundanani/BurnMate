package org.kalpeshbkundanani.burnmate.integration.model

import kotlinx.datetime.LocalDate

data class GoogleFitSyncSummary(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val importedEntries: Int,
    val importedDays: Int
)
