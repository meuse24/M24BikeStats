package info.meuse24.m24bikestats.presentation.dashboard

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.Locale

fun createActivitiesCsvUri(
    context: Context,
    export: ActivitiesCsvExportUiModel,
): Uri {
    val exportDir = File(context.cacheDir, "shared_exports").apply { mkdirs() }
    val safeName = export.fileName
        .lowercase(Locale.US)
        .replace(Regex("[^a-z0-9._-]+"), "-")
        .trim('-')
        .ifBlank { "bosch-activities.csv" }
    val file = File(exportDir, safeName)
    file.writeText(export.csvContent)
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )
}
