package org.kalpeshbkundanani.burnmate.logging.domain

import kotlin.OptIn
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.kalpeshbkundanani.burnmate.logging.model.CalorieAmount
import org.kalpeshbkundanani.burnmate.logging.model.CalorieEntry
import org.kalpeshbkundanani.burnmate.logging.model.EntryDate
import org.kalpeshbkundanani.burnmate.logging.model.EntryId

@OptIn(ExperimentalUuidApi::class)
class DefaultCalorieEntryFactory(
    private val validator: CalorieEntryValidator = DefaultCalorieEntryValidator(),
    private val idGenerator: () -> String = { Uuid.random().toString() },
    private val clock: () -> Instant = { Clock.System.now() }
) : CalorieEntryFactory {

    override fun create(date: EntryDate, amount: CalorieAmount): Result<CalorieEntry> {
        val validationResult = validator.validate(date, amount)
        if (validationResult.isFailure) {
            return Result.failure(validationResult.exceptionOrNull()!!)
        }

        return Result.success(
            CalorieEntry(
                id = EntryId(idGenerator()),
                date = date,
                amount = amount,
                createdAt = clock()
            )
        )
    }
}
