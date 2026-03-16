package org.kalpeshbkundanani.burnmate.logging.model

import kotlinx.datetime.Instant

data class CalorieEntry(
    val id: EntryId,
    val date: EntryDate,
    val amount: CalorieAmount,
    val createdAt: Instant
)
