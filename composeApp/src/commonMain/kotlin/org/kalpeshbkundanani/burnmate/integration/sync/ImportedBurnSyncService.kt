package org.kalpeshbkundanani.burnmate.integration.sync

import kotlinx.datetime.LocalDate
import org.kalpeshbkundanani.burnmate.integration.model.GoogleFitSyncSummary
import org.kalpeshbkundanani.burnmate.integration.model.ImportedBurnSample

interface ImportedBurnSyncService {
    fun sync(
        startDate: LocalDate,
        endDate: LocalDate,
        samples: List<ImportedBurnSample>
    ): Result<GoogleFitSyncSummary>
}
