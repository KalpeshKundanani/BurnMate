package org.kalpeshbkundanani.burnmate.weight.model

import kotlinx.datetime.LocalDate

sealed class WeightHistoryError(message: String) : IllegalArgumentException(message) {

    data class Validation(
        val code: String,
        val detail: String
    ) : WeightHistoryError("$code: $detail")

    data class DuplicateWeightDate(
        val date: LocalDate,
        val detail: String
    ) : WeightHistoryError("DUPLICATE_WEIGHT_DATE: $detail")

    data class WeightEntryNotFound(
        val date: LocalDate,
        val detail: String
    ) : WeightHistoryError("WEIGHT_ENTRY_NOT_FOUND: $detail")

    data class InvalidDateRange(
        val startDate: LocalDate,
        val endDate: LocalDate,
        val detail: String
    ) : WeightHistoryError("INVALID_DATE_RANGE: $detail")
}
