package org.kalpeshbkundanani.burnmate.settings.export

interface AppExportCoordinator {
    suspend fun export(): Result<AppExportSnapshot>
}
