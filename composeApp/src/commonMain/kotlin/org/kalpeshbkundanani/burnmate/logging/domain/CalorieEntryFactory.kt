package org.kalpeshbkundanani.burnmate.logging.domain

import org.kalpeshbkundanani.burnmate.logging.model.CalorieAmount
import org.kalpeshbkundanani.burnmate.logging.model.CalorieEntry
import org.kalpeshbkundanani.burnmate.logging.model.EntryDate

interface CalorieEntryFactory {
    fun create(date: EntryDate, amount: CalorieAmount): Result<CalorieEntry>
}
