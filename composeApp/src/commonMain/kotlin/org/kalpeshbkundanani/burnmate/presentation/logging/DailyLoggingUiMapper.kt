package org.kalpeshbkundanani.burnmate.presentation.logging

import org.kalpeshbkundanani.burnmate.logging.model.CalorieEntry

class DailyLoggingUiMapper {

    fun mapToLogEntryItemState(entry: CalorieEntry): LogEntryItemState {
        val isBurn = entry.amount.value < 0
        val isImported = entry.id.value.startsWith("googlefit:")
        val formattedCalories = if (isBurn) {
            "${entry.amount.value} kcal"
        } else {
            "+${entry.amount.value} kcal"
        }

        return LogEntryItemState(
            id = entry.id.value,
            title = when {
                isImported && isBurn -> "Google Fit Burn"
                isBurn -> "Workout Burn"
                else -> "Calorie Intake"
            },
            timestamp = entry.date.value.toString(),
            formattedCalories = formattedCalories,
            isBurn = isBurn,
            isImported = isImported
        )
    }
}
