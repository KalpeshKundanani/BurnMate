package org.kalpeshbkundanani.burnmate.integration.mapping

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.kalpeshbkundanani.burnmate.integration.model.ImportedActivitySample

class DefaultBurnImportMapperTest {

    @Test
    fun `t06 activity samples map deterministically to burn samples`() {
        val mapper = DefaultBurnImportMapper()

        val mapped = mapper.map(
            listOf(
                ImportedActivitySample(
                    date = LocalDate(2026, 3, 17),
                    stepCount = 2000,
                    activeCalories = null
                ),
                ImportedActivitySample(
                    date = LocalDate(2026, 3, 16),
                    stepCount = 1000,
                    activeCalories = 320
                ),
                ImportedActivitySample(
                    date = LocalDate(2026, 3, 18),
                    stepCount = 0,
                    activeCalories = 0
                )
            )
        )

        assertEquals(
            listOf(LocalDate(2026, 3, 16), LocalDate(2026, 3, 17)),
            mapped.map { it.date }
        )
        assertEquals(listOf("googlefit:2026-03-16:burn", "googlefit:2026-03-17:burn"), mapped.map { it.entryId })
        assertEquals(listOf(320, 80), mapped.map { it.burnCalories })
        assertEquals(
            listOf(
                Instant.parse("2026-03-16T12:00:00Z"),
                Instant.parse("2026-03-17T12:00:00Z")
            ),
            mapped.map { it.createdAt }
        )
    }
}
