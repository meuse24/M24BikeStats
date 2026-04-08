package info.meuse24.m24bikestats.presentation.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import info.meuse24.m24bikestats.BuildConfig
import info.meuse24.m24bikestats.R

private const val AUTHOR_WEBSITE = "https://meuse24.info"
private const val PROJECT_REPOSITORY = "https://github.com/meuse24/M24BikeStats"

@Composable
fun HelpScreen(
    showExplanationTexts: Boolean,
    modifier: Modifier = Modifier,
) {
    val sections = listOf(
        stringResource(R.string.help_login_label) to stringResource(R.string.help_login_text),
        stringResource(R.string.help_activities_label) to stringResource(R.string.help_activities_text),
        stringResource(R.string.help_bike_label) to stringResource(R.string.help_bike_text),
        stringResource(R.string.help_setup_label) to stringResource(R.string.help_setup_text),
        stringResource(R.string.help_functions_label) to stringResource(R.string.help_functions_text),
        stringResource(R.string.help_statistics_label) to stringResource(R.string.help_statistics_text),
        stringResource(R.string.help_diagnostics_label) to stringResource(R.string.help_diagnostics_text),
    )
    InfoListScreen(
        title = stringResource(R.string.help_title),
        subtitle = stringResource(R.string.help_subtitle),
        items = sections,
        showHeroCard = showExplanationTexts,
        modifier = modifier,
    )
}

@Composable
fun InfoScreen(
    showExplanationTexts: Boolean,
    modifier: Modifier = Modifier,
) {
    val projectInfo = listOf(
        InfoDetailRow(stringResource(R.string.info_app_label), stringResource(R.string.app_name)),
        InfoDetailRow(stringResource(R.string.info_author_label), stringResource(R.string.info_author_value)),
        InfoDetailRow(
            label = stringResource(R.string.info_website_label),
            value = AUTHOR_WEBSITE,
            link = AUTHOR_WEBSITE,
        ),
        InfoDetailRow(
            label = stringResource(R.string.info_repository_label),
            value = PROJECT_REPOSITORY,
            link = PROJECT_REPOSITORY,
        ),
        InfoDetailRow(stringResource(R.string.info_application_id_label), BuildConfig.APPLICATION_ID),
        InfoDetailRow(stringResource(R.string.info_copyright_label), stringResource(R.string.info_copyright_value)),
    )
    val privacy = listOf(
        InfoNarrativeItem(stringResource(R.string.info_privacy_auth_label), stringResource(R.string.info_privacy_auth_value)),
        InfoNarrativeItem(stringResource(R.string.info_privacy_storage_label), stringResource(R.string.info_privacy_storage_value)),
        InfoNarrativeItem(stringResource(R.string.info_privacy_backup_label), stringResource(R.string.info_privacy_backup_value)),
        InfoNarrativeItem(stringResource(R.string.info_privacy_sync_label), stringResource(R.string.info_privacy_sync_value)),
    )
    val legal = listOf(
        InfoNarrativeItem(stringResource(R.string.info_legal_status_label), stringResource(R.string.info_legal_status_value)),
        InfoNarrativeItem(stringResource(R.string.info_legal_support_label), stringResource(R.string.info_legal_support_value)),
        InfoNarrativeItem(stringResource(R.string.info_legal_data_act_label), stringResource(R.string.info_legal_data_act_value)),
    )
    val libraryGroups = listOf(
        InfoLibraryGroup(
            title = stringResource(R.string.info_library_group_android),
            entries = listOf(
                "Kotlin 2.2.10",
                "AGP 9.1.0",
                "Compose BOM 2025.05.00",
                "Material 3 Adaptive Navigation",
                "Navigation Compose 2.8.9",
                "Lifecycle Runtime / Compose 2.10.0",
                "Activity Compose 1.13.0",
                "AndroidX Window 1.4.0",
            ),
        ),
        InfoLibraryGroup(
            title = stringResource(R.string.info_library_group_data),
            entries = listOf(
                "Koin 4.0.2",
                "Room 2.8.4",
                "AppAuth 0.11.1",
                "OkHttp 4.12.0",
                "AndroidX Security Crypto 1.1.0-alpha06",
            ),
        ),
        InfoLibraryGroup(
            title = stringResource(R.string.info_library_group_visuals),
            entries = listOf(
                "MapLibre Compose 0.12.1",
                "Vico 2.3.6",
                "Android PdfDocument / StaticLayout",
            ),
        ),
    )
    val credits = listOf(
        InfoNarrativeItem("BOSCH", stringResource(R.string.info_credit_bosch)),
        InfoNarrativeItem("EU Data Act", stringResource(R.string.info_credit_data_act)),
        InfoNarrativeItem("OpenFreeMap", stringResource(R.string.info_credit_openfreemap)),
        InfoNarrativeItem(
            title = stringResource(R.string.info_credit_cli_tools_label),
            body = stringResource(R.string.info_credit_cli_tools),
            badges = listOf("OpenAI Codex", "Claude Code", "Gemini CLI"),
        ),
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            InfoHeroCard(
                title = stringResource(R.string.info_title),
                subtitle = stringResource(R.string.info_subtitle).takeIf { showExplanationTexts },
                badges = listOf(
                    "${stringResource(R.string.info_version_label)} ${BuildConfig.VERSION_NAME}",
                    "${stringResource(R.string.info_build_type_label)} ${BuildConfig.BUILD_TYPE}",
                    "${stringResource(R.string.info_license_label)} MIT",
                ),
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                InfoSectionHeader(stringResource(R.string.info_section_project))
                InfoDetailCard(rows = projectInfo)
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                InfoSectionHeader(stringResource(R.string.info_section_privacy))
                InfoNarrativeCard(items = privacy)
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                InfoSectionHeader(stringResource(R.string.info_section_legal))
                InfoNarrativeCard(items = legal)
            }
        }
        item {
            InfoSectionHeader(stringResource(R.string.info_section_libraries))
        }
        items(libraryGroups) { group ->
            InfoLibraryGroupCard(group = group)
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                InfoSectionHeader(stringResource(R.string.info_section_credits))
                InfoNarrativeCard(items = credits)
            }
        }
    }
}

private data class InfoDetailRow(
    val label: String,
    val value: String,
    val link: String? = null,
)

private data class InfoNarrativeItem(
    val title: String,
    val body: String,
    val badges: List<String> = emptyList(),
)

private data class InfoLibraryGroup(
    val title: String,
    val entries: List<String>,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InfoHeroCard(
    title: String,
    subtitle: String?,
    badges: List<String>,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(R.string.info_section_project),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                badges.forEach { badge ->
                    InfoBadge(
                        text = badge,
                        containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

@Composable
private fun InfoDetailCard(rows: List<InfoDetailRow>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            rows.forEachIndexed { index, row ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = row.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    InfoValueText(value = row.value, link = row.link)
                }
                if (index < rows.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InfoNarrativeCard(items: List<InfoNarrativeItem>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items.forEachIndexed { index, item ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = item.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (item.badges.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            item.badges.forEach { badge ->
                                InfoBadge(
                                    text = badge,
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                        }
                    }
                }
                if (index < items.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InfoLibraryGroupCard(group: InfoLibraryGroup) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = group.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                group.entries.forEach { entry ->
                    InfoBadge(
                        text = entry,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoValueText(
    value: String,
    link: String? = null,
) {
    val uriHandler = LocalUriHandler.current
    val isLink = !link.isNullOrBlank()
    Text(
        text = value,
        style = MaterialTheme.typography.bodyLarge,
        color = if (isLink) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        modifier = if (isLink) {
            Modifier.clickable { uriHandler.openUri(link ?: value) }
        } else {
            Modifier
        },
    )
}

@Composable
private fun InfoBadge(
    text: String,
    containerColor: Color,
    contentColor: Color,
) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.large,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun InfoListScreen(
    title: String,
    subtitle: String,
    items: List<Pair<String, String>>,
    showHeroCard: Boolean = true,
    modifier: Modifier = Modifier,
) {
    InfoSectionScreen(
        title = title,
        subtitle = subtitle,
        sections = listOf("" to items),
        showHeroCard = showHeroCard,
        modifier = modifier,
    )
}

@Composable
private fun InfoSectionScreen(
    title: String,
    subtitle: String,
    sections: List<Pair<String, List<Pair<String, String>>>>,
    showHeroCard: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (showHeroCard) {
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
        }
        sections.forEach { (sectionTitle, sectionItems) ->
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
            items(sectionItems) { (label, value) ->
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
