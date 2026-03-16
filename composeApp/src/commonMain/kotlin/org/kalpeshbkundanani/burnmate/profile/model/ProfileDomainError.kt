package org.kalpeshbkundanani.burnmate.profile.model

sealed class ProfileDomainError(message: String) : IllegalArgumentException(message) {
    data class Validation(
        val code: String,
        val detail: String
    ) : ProfileDomainError("$code: $detail")
}
