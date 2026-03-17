package org.kalpeshbkundanani.burnmate.settings.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class InMemoryAppSessionStore(
    private val defaultState: AppSessionState = AppSessionState()
) : AppSessionStore {

    private val mutableState = MutableStateFlow(defaultState)
    val state: StateFlow<AppSessionState> = mutableState.asStateFlow()

    override fun read(): AppSessionState = mutableState.value

    override fun update(transform: (AppSessionState) -> AppSessionState): AppSessionState {
        val updated = transform(mutableState.value)
        mutableState.value = updated
        return updated
    }

    override fun reset(): AppSessionState {
        mutableState.value = defaultState
        return mutableState.value
    }
}
