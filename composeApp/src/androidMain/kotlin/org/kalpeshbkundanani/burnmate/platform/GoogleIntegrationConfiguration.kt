package org.kalpeshbkundanani.burnmate.platform

import android.content.Context
import android.content.pm.PackageManager
import org.kalpeshbkundanani.burnmate.integration.model.GoogleIntegrationAvailability

data class GoogleIntegrationConfiguration(
    val webClientId: String?,
    val fitProjectEnabled: Boolean
) {
    fun availability(): GoogleIntegrationAvailability {
        return when {
            webClientId.isNullOrBlank() -> GoogleIntegrationAvailability.ConfigurationMissing
            !fitProjectEnabled -> GoogleIntegrationAvailability.FitProjectUnavailable
            else -> GoogleIntegrationAvailability.Available
        }
    }

    companion object {
        private const val GOOGLE_WEB_CLIENT_ID = "burnmate.google.web_client_id"
        private const val GOOGLE_FIT_ENABLED = "burnmate.google.fit_project_enabled"

        fun fromContext(context: Context): GoogleIntegrationConfiguration {
            val applicationInfo = context.packageManager.getApplicationInfo(
                context.packageName,
                PackageManager.GET_META_DATA
            )
            val metadata = applicationInfo.metaData
            return GoogleIntegrationConfiguration(
                webClientId = metadata?.getString(GOOGLE_WEB_CLIENT_ID),
                fitProjectEnabled = metadata?.getBoolean(GOOGLE_FIT_ENABLED, false) ?: false
            )
        }
    }
}
