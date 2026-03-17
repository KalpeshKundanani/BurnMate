package org.kalpeshbkundanani.burnmate.settings.preferences

interface AppPreferencesStore {
    fun read(): AppPreferences
    fun update(transform: (AppPreferences) -> AppPreferences): AppPreferences
    fun reset(): AppPreferences
}
