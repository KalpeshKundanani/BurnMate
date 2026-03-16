package org.kalpeshbkundanani.burnmate.logging.model

import kotlin.time.Instant

data class CalorieEntry(
    val id: EntryId,
    val date: EntryDate,
    val amount: CalorieAmount,
    val createdAt: Instant
)
