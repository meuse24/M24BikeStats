package info.meuse24.m24bikestats.presentation.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import info.meuse24.m24bikestats.R
import info.meuse24.m24bikestats.domain.model.CsvExportFormat
import info.meuse24.m24bikestats.domain.model.DisplayMode
import info.meuse24.m24bikestats.domain.model.ExplanationTextsPromptTiming

@Composable
fun SetupScreen(
    displayMode: DisplayMode,
    csvExportFormat: CsvExportFormat,
    showExplanationTexts: Boolean,
    explanationTextsPromptTiming: ExplanationTextsPromptTiming,
    onDisplayModeSelected: (DisplayMode) -> Unit,
    onCsvExportFormatSelected: (CsvExportFormat) -> Unit,
    onShowExplanationTextsChanged: (Boolean) -> Unit,
    onExplanationTextsPromptTimingSelected: (ExplanationTextsPromptTiming) -> Unit,
    onResetExplanationTextsPrompt: () -> Unit,
    onResetAllData: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val displayModeOptions = DisplayMode.entries.map { it to stringResource(it.labelRes()) }
    val csvOptions = CsvExportFormat.entries.map { it to stringResource(it.labelRes()) }
    val explanationPromptOptions = ExplanationTextsPromptTiming.entries.map { it to stringResource(it.labelRes()) }
    var showResetDialog by rememberSaveable { mutableStateOf(false) }
    val explanationPromptSubtitleRes = when {
        !showExplanationTexts -> R.string.setup_explanation_prompt_subtitle_hidden
        explanationTextsPromptTiming == ExplanationTextsPromptTiming.NEVER ->
            R.string.setup_explanation_prompt_subtitle_disabled
        else -> R.string.setup_explanation_prompt_subtitle
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (showExplanationTexts) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.setup_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.92f),
                        modifier = Modifier.padding(18.dp),
                    )
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    SetupTogglePreference(
                        title = stringResource(R.string.setup_explanation_texts_title),
                        subtitle = stringResource(R.string.setup_explanation_texts_subtitle).takeIf {
                            showExplanationTexts
                        },
                        checked = showExplanationTexts,
                        onCheckedChange = onShowExplanationTextsChanged,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SetupDropdownPreference(
                            title = stringResource(R.string.setup_explanation_prompt_title),
                            subtitle = stringResource(explanationPromptSubtitleRes),
                            selectedLabel = stringResource(explanationTextsPromptTiming.labelRes()),
                            options = explanationPromptOptions,
                            onOptionSelected = onExplanationTextsPromptTimingSelected,
                        )
                        if (showExplanationTexts && explanationTextsPromptTiming != ExplanationTextsPromptTiming.NEVER) {
                            OutlinedButton(
                                onClick = onResetExplanationTextsPrompt,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                )
                                Text(
                                    text = stringResource(R.string.setup_explanation_prompt_reset_button),
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    SetupDropdownPreference(
                        title = stringResource(R.string.setup_display_mode_title),
                        subtitle = stringResource(R.string.setup_display_mode_subtitle).takeIf {
                            showExplanationTexts
                        },
                        selectedLabel = stringResource(displayMode.labelRes()),
                        options = displayModeOptions,
                        onOptionSelected = onDisplayModeSelected,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    SetupDropdownPreference(
                        title = stringResource(R.string.setup_csv_title),
                        subtitle = stringResource(R.string.setup_csv_subtitle).takeIf {
                            showExplanationTexts
                        },
                        selectedLabel = stringResource(csvExportFormat.labelRes()),
                        options = csvOptions,
                        onOptionSelected = onCsvExportFormatSelected,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.setup_reset_button),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error,
                        )
                        stringResource(R.string.setup_reset_description).takeIf { showExplanationTexts }?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        OutlinedButton(
                            onClick = { showResetDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                        ) {
                            Text(text = stringResource(R.string.setup_reset_button))
                        }
                    }
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.setup_reset_title)) },
            text = { Text(stringResource(R.string.setup_reset_text)) },
            confirmButton = {
                OutlinedButton(
                    onClick = {
                        showResetDialog = false
                        onResetAllData()
                    },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(R.string.setup_reset_confirm))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

@Composable
private fun <T> SetupDropdownPreference(
    title: String,
    subtitle: String?,
    selectedLabel: String,
    options: List<Pair<T, String>>,
    onOptionSelected: (T) -> Unit,
) {
    var expanded by rememberSaveable(title) { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Text(
                    text = selectedLabel,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                options.forEach { (value, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            expanded = false
                            onOptionSelected(value)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SetupTogglePreference(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}

private fun CsvExportFormat.labelRes(): Int = when (this) {
    CsvExportFormat.SYSTEM_DEFAULT -> R.string.csv_export_format_system_default_label
    CsvExportFormat.EXCEL_DE -> R.string.csv_export_format_excel_de_label
    CsvExportFormat.STANDARD_INTERNATIONAL -> R.string.csv_export_format_standard_international_label
}

private fun ExplanationTextsPromptTiming.labelRes(): Int = when (this) {
    ExplanationTextsPromptTiming.EARLY -> R.string.explanation_texts_prompt_timing_early_label
    ExplanationTextsPromptTiming.STANDARD -> R.string.explanation_texts_prompt_timing_standard_label
    ExplanationTextsPromptTiming.LATE -> R.string.explanation_texts_prompt_timing_late_label
    ExplanationTextsPromptTiming.NEVER -> R.string.explanation_texts_prompt_timing_never_label
}

private fun DisplayMode.labelRes(): Int = when (this) {
    DisplayMode.AUTOMATIC -> R.string.display_mode_automatic_label
    DisplayMode.LIGHT -> R.string.display_mode_light_label
    DisplayMode.DARK -> R.string.display_mode_dark_label
}
