package org.kalpeshbkundanani.burnmate.settings.state

import kotlinx.coroutines.flow.StateFlow

interface AppSessionStore {
    val state: StateFlow<AppSessionState>
    fun read(): AppSessionState
    fun update(transform: (AppSessionState) -> AppSessionState): AppSessionState
    fun reset(): AppSessionState
}
