package org.kalpeshbkundanani.burnmate.settings.state

import org.kalpeshbkundanani.burnmate.profile.model.UserProfileSummary

data class AppSessionState(
    val activeProfile: UserProfileSummary? = null
)
