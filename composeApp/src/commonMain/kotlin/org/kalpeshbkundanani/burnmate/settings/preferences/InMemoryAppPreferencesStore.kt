package org.kalpeshbkundanani.burnmate.settings.preferences

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class InMemoryAppPreferencesStore(
    private val defaultPreferences: AppPreferences = AppPreferences()
) : AppPreferencesStore {

    private val mutableState = MutableStateFlow(defaultPreferences)
    val state: StateFlow<AppPreferences> = mutableState.asStateFlow()

    override fun read(): AppPreferences = mutableState.value

    override fun update(transform: (AppPreferences) -> AppPreferences): AppPreferences {
        val updated = transform(mutableState.value)
        mutableState.value = updated
        return updated
    }

    override fun reset(): AppPreferences {
        mutableState.value = defaultPreferences
        return mutableState.value
    }
}
