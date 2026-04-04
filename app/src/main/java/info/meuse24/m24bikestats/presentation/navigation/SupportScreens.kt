package info.meuse24.m24bikestats.presentation.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import info.meuse24.m24bikestats.BuildConfig
import info.meuse24.m24bikestats.R

@Composable
fun HelpScreen(modifier: Modifier = Modifier) {
    val sections = listOf(
        stringResource(R.string.help_login_label) to stringResource(R.string.help_login_text),
        stringResource(R.string.help_activities_label) to stringResource(R.string.help_activities_text),
        stringResource(R.string.help_bike_label) to stringResource(R.string.help_bike_text),
        stringResource(R.string.help_functions_label) to stringResource(R.string.help_functions_text),
        stringResource(R.string.help_diagnostics_label) to stringResource(R.string.help_diagnostics_text),
    )
    InfoListScreen(
        title = stringResource(R.string.help_title),
        subtitle = stringResource(R.string.help_subtitle),
        items = sections,
        modifier = modifier,
    )
}

@Composable
fun InfoScreen(modifier: Modifier = Modifier) {
    val appInfo = listOf(
        stringResource(R.string.info_app_label) to stringResource(R.string.app_name),
        stringResource(R.string.info_version_label) to BuildConfig.VERSION_NAME,
        stringResource(R.string.info_build_type_label) to BuildConfig.BUILD_TYPE,
        stringResource(R.string.info_application_id_label) to BuildConfig.APPLICATION_ID,
        stringResource(R.string.info_license_label) to "MIT",
        stringResource(R.string.info_copyright_label) to stringResource(R.string.info_copyright_value),
        stringResource(R.string.info_repository_label) to "https://github.com/meuse24/M24BikeStats",
    )
    val libraries = listOf(
        "Kotlin" to "2.2.10 • JetBrains",
        "Android Gradle Plugin" to "9.1.0 • Google Android team",
        "Jetpack Compose BOM" to "2025.05.00 • Google AndroidX team",
        "Material 3 Adaptive Navigation Suite" to "Compose BOM • Google AndroidX team",
        "Navigation Compose" to "2.8.9 • Google AndroidX team",
        "Lifecycle Runtime / Compose" to "2.10.0 • Google AndroidX team",
        "Activity Compose" to "1.13.0 • Google AndroidX team",
        "Koin" to "4.0.2 • Arnaud Giuliani, Kotzilla and contributors",
        "Room" to "2.8.4 • Google AndroidX team",
        "AppAuth" to "0.11.1 • OpenID Foundation",
        "OkHttp" to "4.12.0 • Square",
        "AndroidX Security Crypto" to "1.1.0-alpha06 • Google AndroidX team",
        "AndroidX Window" to "1.4.0 • Google AndroidX team",
        "MapLibre Compose" to "0.12.1 • MapLibre community",
    )
    val credits = listOf(
        "BOSCH" to stringResource(R.string.info_credit_bosch),
        "EU Data Act" to stringResource(R.string.info_credit_data_act),
        "OpenFreeMap" to stringResource(R.string.info_credit_openfreemap),
        "OpenAI Codex" to stringResource(R.string.info_credit_codex),
        "Anthropic Claude Code" to stringResource(R.string.info_credit_claude_code),
        "Google Gemini CLI" to stringResource(R.string.info_credit_gemini_cli),
    )
    InfoSectionScreen(
        title = stringResource(R.string.info_title),
        subtitle = stringResource(R.string.info_subtitle),
        sections = listOf(
            stringResource(R.string.info_section_project) to appInfo,
            stringResource(R.string.info_section_libraries) to libraries,
            stringResource(R.string.info_section_credits) to credits,
        ),
        modifier = modifier,
    )
}

@Composable
private fun InfoListScreen(
    title: String,
    subtitle: String,
    items: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
) {
    InfoSectionScreen(
        title = title,
        subtitle = subtitle,
        sections = listOf("" to items),
        modifier = modifier,
    )
}

@Composable
private fun InfoSectionScreen(
    title: String,
    subtitle: String,
    sections: List<Pair<String, List<Pair<String, String>>>>,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                    )
                }
            }
        }
        sections.forEach { (sectionTitle, items) ->
            if (sectionTitle.isNotBlank()) {
                item {
                    Text(
                        text = sectionTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                    )
                }
            }
            items(items) { (label, value) ->
                val isUrl = value.startsWith("https://") || value.startsWith("http://")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = value,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isUrl) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            modifier = if (isUrl) {
                                Modifier.clickable { uriHandler.openUri(value) }
                            } else {
                                Modifier
                            },
                        )
                    }
                }
            }
        }
    }
}
