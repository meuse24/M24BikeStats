package info.meuse24.m24bikestats.presentation.apitest

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import info.meuse24.m24bikestats.domain.model.BoschEndpoint
import java.io.File
import java.util.Locale

fun createApiTestShareIntent(
    context: Context,
    endpoint: BoschEndpoint,
    content: String,
): Intent {
    val uri = createApiTestShareUri(context, endpoint, content)
    return Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, endpoint.label)
        putExtra(Intent.EXTRA_TEXT, endpoint.label)
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}

private fun createApiTestShareUri(
    context: Context,
    endpoint: BoschEndpoint,
    content: String,
): Uri {
    val exportDir = File(context.cacheDir, "shared_exports").apply { mkdirs() }
    val file = File(exportDir, apiTestShareFileName(endpoint, content))
    file.writeText(content)
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )
}

internal fun apiTestShareFileName(
    endpoint: BoschEndpoint,
    content: String,
): String {
    val baseName = if (content.startsWith("=== Bosch Endpoint Batch Test ===")) {
        "bosch-api-test-run-all"
    } else {
        "bosch-api-test-${sanitizeFileComponent(endpoint.name)}"
    }
    return "$baseName.txt"
}

private fun sanitizeFileComponent(value: String): String =
    value
        .lowercase(Locale.US)
        .replace(Regex("[^a-z0-9._-]+"), "-")
        .trim('-')
        .ifBlank { "endpoint" }
