package org.kalpeshbkundanani.burnmate.settings.preferences

import kotlinx.coroutines.flow.StateFlow

interface AppPreferencesStore {
    val state: StateFlow<AppPreferences>
    fun read(): AppPreferences
    fun update(transform: (AppPreferences) -> AppPreferences): AppPreferences
    fun reset(): AppPreferences
}
