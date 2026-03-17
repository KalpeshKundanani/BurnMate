package org.kalpeshbkundanani.burnmate.integration.mapping

import org.kalpeshbkundanani.burnmate.integration.model.ImportedActivitySample
import org.kalpeshbkundanani.burnmate.integration.model.ImportedBurnSample

interface BurnImportMapper {
    fun map(samples: List<ImportedActivitySample>): List<ImportedBurnSample>
}
