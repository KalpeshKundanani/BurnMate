package org.kalpeshbkundanani.burnmate.platform

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.kalpeshbkundanani.burnmate.settings.export.AppExportLauncher
import org.kalpeshbkundanani.burnmate.settings.export.AppExportSnapshot

class AndroidAppExportLauncher(
    private val context: Context
) : AppExportLauncher {

    override suspend fun launch(snapshot: AppExportSnapshot): Result<Unit> {
        return runCatching {
            val chooserIntent = Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "BurnMate Export")
                    putExtra(Intent.EXTRA_TEXT, snapshot.asExportText())
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
                "Export BurnMate data"
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooserIntent)
        }
    }
}

@Composable
fun rememberAndroidAppExportLauncher(context: Context): AppExportLauncher {
    return remember(context) { AndroidAppExportLauncher(context) }
}

private fun AppExportSnapshot.asExportText(): String {
    return buildString {
        appendLine("exportedAt=$exportedAt")
        appendLine("dailyTargetCalories=${preferences.dailyTargetCalories}")
        appendLine("integrationSummary=${integrationSummary ?: "none"}")
        appendLine("profile=${profile?.metrics}")
        appendLine("calorieEntries=${calorieEntries.size}")
        calorieEntries.forEach { entry ->
            appendLine("calorieEntry=${entry.id.value}|${entry.date.value}|${entry.amount.value}|${entry.createdAt}")
        }
        appendLine("weightEntries=${weightEntries.size}")
        weightEntries.forEach { entry ->
            appendLine("weightEntry=${entry.date}|${entry.weight.kg}|${entry.createdAt}")
        }
    }
}
