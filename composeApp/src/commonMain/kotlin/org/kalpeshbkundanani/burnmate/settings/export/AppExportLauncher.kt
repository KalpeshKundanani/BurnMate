package org.kalpeshbkundanani.burnmate.settings.export

interface AppExportLauncher {
    suspend fun launch(snapshot: AppExportSnapshot): Result<Unit>
}

object NoOpAppExportLauncher : AppExportLauncher {
    override suspend fun launch(snapshot: AppExportSnapshot): Result<Unit> = Result.success(Unit)
}
