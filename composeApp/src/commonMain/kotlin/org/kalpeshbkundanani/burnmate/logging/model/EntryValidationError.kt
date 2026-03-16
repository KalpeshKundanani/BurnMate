package org.kalpeshbkundanani.burnmate.logging.model

sealed class EntryValidationError(message: String) : IllegalArgumentException(message) {
    data class InvalidCalorieAmount(
        val amount: Int,
        val detail: String
    ) : EntryValidationError("INVALID_CALORIE_AMOUNT: $detail")

    data class UnrealisticCalorieAmount(
        val amount: Int,
        val maxAllowed: Int,
        val detail: String
    ) : EntryValidationError("UNREALISTIC_CALORIE_AMOUNT: $detail")
}
