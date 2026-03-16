package org.kalpeshbkundanani.burnmate.presentation.shared

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class SelectedDateCoordinator(
    initialDate: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
) {
    private val observers = mutableSetOf<(LocalDate) -> Unit>()

    var selectedDate: LocalDate = initialDate
        private set

    fun observe(observer: (LocalDate) -> Unit): () -> Unit {
        observers += observer
        observer(selectedDate)
        return { observers -= observer }
    }

    fun updateSelectedDate(date: LocalDate) {
        if (date == selectedDate) {
            return
        }
        selectedDate = date
        observers.forEach { it(date) }
    }
}
