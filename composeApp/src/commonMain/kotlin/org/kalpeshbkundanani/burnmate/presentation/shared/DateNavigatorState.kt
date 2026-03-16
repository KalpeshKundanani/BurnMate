package org.kalpeshbkundanani.burnmate.presentation.shared

import kotlinx.datetime.LocalDate

data class DateNavigatorState(
    val selectedDate: LocalDate,
    val dateLabel: String,
    val canGoForward: Boolean
)
