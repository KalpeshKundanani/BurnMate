package org.kalpeshbkundanani.burnmate.logging.model

sealed class EntryRepositoryError(message: String) : IllegalArgumentException(message) {
    data class DuplicateEntry(
        val id: EntryId,
        val detail: String
    ) : EntryRepositoryError("DUPLICATE_ENTRY: $detail")

    data class InvalidDateRange(
        val startDate: EntryDate,
        val endDate: EntryDate,
        val detail: String
    ) : EntryRepositoryError("INVALID_DATE_RANGE: $detail")
}
