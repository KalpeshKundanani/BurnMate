package org.kalpeshbkundanani.burnmate.presentation.logging

import org.kalpeshbkundanani.burnmate.logging.model.CalorieEntry

class DailyLoggingUiMapper {

    fun mapToLogEntryItemState(entry: CalorieEntry): LogEntryItemState {
        val isBurn = entry.amount.value < 0
        val formattedCalories = if (isBurn) {
            "${entry.amount.value} kcal"
        } else {
            "+${entry.amount.value} kcal"
        }

        return LogEntryItemState(
            id = entry.id.value,
            title = if (isBurn) "Workout Burn" else "Calorie Intake",
            timestamp = entry.date.value.toString(),
            formattedCalories = formattedCalories,
            isBurn = isBurn
        )
    }
}
