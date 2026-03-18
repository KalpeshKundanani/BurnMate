package org.kalpeshbkundanani.burnmate.settings.state

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.kalpeshbkundanani.burnmate.profile.domain.DefaultUserProfileFactory
import org.kalpeshbkundanani.burnmate.profile.model.BodyMetrics

class AndroidAppSessionStore(
    context: Context,
    private val profileFactory: DefaultUserProfileFactory = DefaultUserProfileFactory()
) : AppSessionStore {

    private val sharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val mutableState = MutableStateFlow(readFromPreferences())

    override val state: StateFlow<AppSessionState> = mutableState.asStateFlow()

    override fun read(): AppSessionState = mutableState.value

    override fun update(transform: (AppSessionState) -> AppSessionState): AppSessionState {
        val updated = transform(mutableState.value)
        writeToPreferences(updated)
        mutableState.value = updated
        return updated
    }

    override fun reset(): AppSessionState {
        val resetValue = AppSessionState()
        writeToPreferences(resetValue)
        mutableState.value = resetValue
        return resetValue
    }

    private fun readFromPreferences(): AppSessionState {
        val heightCm = sharedPreferences.getString(KEY_HEIGHT_CM, null)?.toDoubleOrNull()
        val currentWeightKg = sharedPreferences.getString(KEY_CURRENT_WEIGHT_KG, null)?.toDoubleOrNull()
        val goalWeightKg = sharedPreferences.getString(KEY_GOAL_WEIGHT_KG, null)?.toDoubleOrNull()
        if (heightCm == null || currentWeightKg == null || goalWeightKg == null) {
            return AppSessionState()
        }

        val profile = profileFactory.create(
            BodyMetrics(
                heightCm = heightCm,
                currentWeightKg = currentWeightKg,
                goalWeightKg = goalWeightKg
            )
        ).getOrNull()

        return AppSessionState(activeProfile = profile)
    }

    private fun writeToPreferences(value: AppSessionState) {
        val editor = sharedPreferences.edit()
        val profile = value.activeProfile
        if (profile == null) {
            editor.remove(KEY_HEIGHT_CM)
            editor.remove(KEY_CURRENT_WEIGHT_KG)
            editor.remove(KEY_GOAL_WEIGHT_KG)
        } else {
            editor.putString(KEY_HEIGHT_CM, profile.metrics.heightCm.toString())
            editor.putString(KEY_CURRENT_WEIGHT_KG, profile.metrics.currentWeightKg.toString())
            editor.putString(KEY_GOAL_WEIGHT_KG, profile.metrics.goalWeightKg.toString())
        }
        editor.apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "burnmate_session"
        const val KEY_HEIGHT_CM = "height_cm"
        const val KEY_CURRENT_WEIGHT_KG = "current_weight_kg"
        const val KEY_GOAL_WEIGHT_KG = "goal_weight_kg"
    }
}
