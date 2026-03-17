package org.kalpeshbkundanani.burnmate.integration.fit

import kotlinx.datetime.LocalDate
import org.kalpeshbkundanani.burnmate.integration.model.GoogleAccountSession
import org.kalpeshbkundanani.burnmate.integration.model.GoogleIntegrationAvailability
import org.kalpeshbkundanani.burnmate.integration.model.ImportedActivitySample

interface GoogleFitService {
    fun availability(): GoogleIntegrationAvailability
    suspend fun readDailyActivity(
        session: GoogleAccountSession,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<List<ImportedActivitySample>>

    suspend fun disconnect(session: GoogleAccountSession): Result<Unit>
}
