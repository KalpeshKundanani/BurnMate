package org.kalpeshbkundanani.burnmate.settings.export

import kotlinx.datetime.Instant
import org.kalpeshbkundanani.burnmate.logging.model.CalorieEntry
import org.kalpeshbkundanani.burnmate.profile.model.UserProfileSummary
import org.kalpeshbkundanani.burnmate.settings.preferences.AppPreferences
import org.kalpeshbkundanani.burnmate.weight.model.WeightEntry

data class AppExportSnapshot(
    val exportedAt: Instant,
    val profile: UserProfileSummary?,
    val preferences: AppPreferences,
    val calorieEntries: List<CalorieEntry>,
    val weightEntries: List<WeightEntry>,
    val integrationSummary: String?
)
