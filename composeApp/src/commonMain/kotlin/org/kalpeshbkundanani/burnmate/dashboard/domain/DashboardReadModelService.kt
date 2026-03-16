package org.kalpeshbkundanani.burnmate.dashboard.domain

import kotlinx.datetime.LocalDate
import org.kalpeshbkundanani.burnmate.dashboard.model.DashboardSnapshot

interface DashboardReadModelService {
    fun getDashboardSnapshot(today: LocalDate): Result<DashboardSnapshot>
}
