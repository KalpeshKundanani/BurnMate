package org.kalpeshbkundanani.burnmate.logging.domain

import org.kalpeshbkundanani.burnmate.logging.model.EntryDate

data class EntryRangeQuery(
    val startDate: EntryDate,
    val endDate: EntryDate
)
