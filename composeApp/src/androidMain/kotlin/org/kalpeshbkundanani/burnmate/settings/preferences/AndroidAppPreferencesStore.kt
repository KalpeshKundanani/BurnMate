package org.kalpeshbkundanani.burnmate.settings.preferences

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidAppPreferencesStore(context: Context) : AppPreferencesStore {

    private val sharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val mutableState = MutableStateFlow(readFromPreferences())

    override val state: StateFlow<AppPreferences> = mutableState.asStateFlow()

    override fun read(): AppPreferences = mutableState.value

    override fun update(transform: (AppPreferences) -> AppPreferences): AppPreferences {
        val updated = transform(mutableState.value)
        writeToPreferences(updated)
        mutableState.value = updated
        return updated
    }

    override fun reset(): AppPreferences {
        val resetValue = AppPreferences()
        writeToPreferences(resetValue)
        mutableState.value = resetValue
        return resetValue
    }

    private fun readFromPreferences(): AppPreferences {
        return AppPreferences(
            dailyTargetCalories = sharedPreferences.getInt(KEY_DAILY_TARGET_CALORIES, AppPreferences().dailyTargetCalories)
        )
    }

    private fun writeToPreferences(value: AppPreferences) {
        sharedPreferences.edit()
            .putInt(KEY_DAILY_TARGET_CALORIES, value.dailyTargetCalories)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "burnmate_preferences"
        const val KEY_DAILY_TARGET_CALORIES = "daily_target_calories"
    }
}
