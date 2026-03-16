package org.kalpeshbkundanani.burnmate.caloriedebt.model

sealed class CalorieDebtError(message: String) : IllegalArgumentException(message) {
    data class Validation(
        val code: String,
        val detail: String
    ) : CalorieDebtError("$code: $detail")
}
