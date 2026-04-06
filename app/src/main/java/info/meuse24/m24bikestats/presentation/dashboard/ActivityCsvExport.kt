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
    return createSharedCsvUri(
        context = context,
        fileName = export.fileName,
        csvContent = export.csvContent,
        fallbackName = "bosch-activities.csv",
    )
}

fun createActivityDetailsCsvUri(
    context: Context,
    export: ActivityDetailsCsvExportUiModel,
): Uri {
    return createSharedCsvUri(
        context = context,
        fileName = export.fileName,
        csvContent = export.csvContent,
        fallbackName = "bosch-activity-details.csv",
    )
}

fun createPdfReportUri(
    context: Context,
    export: PdfExportUiModel,
): Uri =
    FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        File(export.filePath),
    )

private fun createSharedCsvUri(
    context: Context,
    fileName: String,
    csvContent: String,
    fallbackName: String,
): Uri {
    val exportDir = File(context.cacheDir, "shared_exports").apply { mkdirs() }
    val safeName = fileName
        .lowercase(Locale.US)
        .replace(Regex("[^a-z0-9._-]+"), "-")
        .trim('-')
        .ifBlank { fallbackName }
    val file = File(exportDir, safeName)
    file.writeText(csvContent)
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )
}
