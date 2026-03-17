package org.kalpeshbkundanani.burnmate.settings.state

interface AppSessionStore {
    fun read(): AppSessionState
    fun update(transform: (AppSessionState) -> AppSessionState): AppSessionState
    fun reset(): AppSessionState
}
