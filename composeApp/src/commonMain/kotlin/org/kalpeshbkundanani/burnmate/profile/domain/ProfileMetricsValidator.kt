package org.kalpeshbkundanani.burnmate.profile.domain

import org.kalpeshbkundanani.burnmate.profile.model.BodyMetrics

interface ProfileMetricsValidator {
    fun validate(metrics: BodyMetrics): Result<Unit>
}
