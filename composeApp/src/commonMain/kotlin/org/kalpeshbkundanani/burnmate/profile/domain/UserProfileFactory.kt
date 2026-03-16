package org.kalpeshbkundanani.burnmate.profile.domain

import org.kalpeshbkundanani.burnmate.profile.model.BodyMetrics
import org.kalpeshbkundanani.burnmate.profile.model.UserProfileSummary

interface UserProfileFactory {
    fun create(metrics: BodyMetrics): Result<UserProfileSummary>
}
