package org.kalpeshbkundanani.burnmate.integration.mapping

import kotlin.math.roundToInt
import kotlinx.datetime.Instant
import org.kalpeshbkundanani.burnmate.integration.model.ImportedActivitySample
import org.kalpeshbkundanani.burnmate.integration.model.ImportedBurnSample

class DefaultBurnImportMapper : BurnImportMapper {

    override fun map(samples: List<ImportedActivitySample>): List<ImportedBurnSample> {
        return samples
            .sortedBy { it.date }
            .mapNotNull { sample ->
                require(sample.activeCalories == null || sample.activeCalories >= 0) {
                    "INVALID_ACTIVE_CALORIES"
                }
                require(sample.stepCount == null || sample.stepCount >= 0) {
                    "INVALID_STEP_COUNT"
                }

                val burnCalories = when {
                    sample.activeCalories != null && sample.activeCalories > 0 -> sample.activeCalories
                    sample.stepCount != null && sample.stepCount > 0 -> (sample.stepCount * 0.04).roundToInt()
                    else -> null
                } ?: return@mapNotNull null

                ImportedBurnSample(
                    entryId = "googlefit:${sample.date}:burn",
                    date = sample.date,
                    burnCalories = burnCalories,
                    createdAt = Instant.parse("${sample.date}T12:00:00Z"),
                    source = sample.source
                )
            }
    }
}
