package info.meuse24.m24bikestats.presentation.dashboard

import android.content.Context
import android.content.Intent
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

fun createPdfShareIntent(
    context: Context,
    export: PdfExportUiModel,
): Intent =
    Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, createPdfReportUri(context, export))
        putExtra(Intent.EXTRA_SUBJECT, export.fileName)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

fun copyPdfReportToUri(
    context: Context,
    export: PdfExportUiModel,
    targetUri: Uri,
): Result<Unit> = runCatching {
    val sourceFile = File(export.filePath)
    context.contentResolver.openOutputStream(targetUri)?.use { outputStream ->
        sourceFile.inputStream().use { inputStream ->
            inputStream.copyTo(outputStream)
        }
    } ?: error("Unable to open destination document")
}

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
