package org.kalpeshbkundanani.burnmate.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import org.kalpeshbkundanani.burnmate.settings.preferences.AndroidAppPreferencesStore
import org.kalpeshbkundanani.burnmate.settings.preferences.AppPreferencesStore
import org.kalpeshbkundanani.burnmate.settings.state.AndroidAppSessionStore
import org.kalpeshbkundanani.burnmate.settings.state.AppSessionStore

@Composable
fun rememberAndroidAppPreferencesStore(): AppPreferencesStore {
    val context = LocalContext.current.applicationContext
    return remember(context) { AndroidAppPreferencesStore(context) }
}

@Composable
fun rememberAndroidAppSessionStore(): AppSessionStore {
    val context = LocalContext.current.applicationContext
    return remember(context) { AndroidAppSessionStore(context) }
}
