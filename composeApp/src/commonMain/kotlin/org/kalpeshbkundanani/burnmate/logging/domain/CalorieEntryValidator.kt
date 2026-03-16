package org.kalpeshbkundanani.burnmate.logging.domain

import org.kalpeshbkundanani.burnmate.logging.model.CalorieAmount
import org.kalpeshbkundanani.burnmate.logging.model.EntryDate

interface CalorieEntryValidator {
    fun validate(date: EntryDate, amount: CalorieAmount): Result<Unit>
}
