package info.meuse24.m24bikestats.presentation.dashboard

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import info.meuse24.m24bikestats.R
import info.meuse24.m24bikestats.presentation.theme.DesignTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityDetailScreen(
    uiState: ActivityDetailScreenUiState,
    onLoadActivity: (String) -> Unit,
    activityId: String,
    onNavigateToTrack: (String) -> Unit,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val selectedActivity = uiState.selectedActivityDetail?.takeIf { uiState.selectedActivityId == activityId }

    LaunchedEffect(activityId) {
        onLoadActivity(activityId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selectedActivity?.title ?: stringResource(R.string.activity_detail_fallback_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.activity_detail_back))
                    }
                },
                actions = {
                    selectedActivity?.let { activity ->
                        IconButton(onClick = { shareActivityDetail(context, activity) }) {
                            Icon(Icons.Default.Share, contentDescription = stringResource(R.string.activity_detail_share))
                        }
                    }
                },
            )
        },
    ) { padding ->
        when {
            uiState.isActivityDetailLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.selectedActivityId == activityId && uiState.selectedActivityDetail != null -> {
                val activity = uiState.selectedActivityDetail
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(
                        horizontal = DesignTokens.ScreenHorizontalPadding,
                        vertical = DesignTokens.ScreenVerticalPadding,
                    ),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.SectionSpacing),
                ) {
                    if (uiState.isActivityDetailRefreshing) {
                        item {
                            DashboardPageContainer {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                    items(activity.sections) { section ->
                        DashboardPageContainer {
                            DetailSectionCard(
                                section = section,
                                activity = activity,
                                onNavigateToTrack = onNavigateToTrack,
                                showExplanationTexts = uiState.showExplanationTexts,
                            )
                        }
                    }
                }
            }

            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(R.string.activity_detail_not_found))
                }
            }
        }
    }
}

private fun shareActivityDetail(
    context: Context,
    activity: ActivityDetailUiModel,
) {
    val shareText = buildString {
        appendLine(activity.title)
        activity.subtitle?.takeIf { it.isNotBlank() }?.let { appendLine(it) }
        activity.sections.forEach { section ->
            appendLine()
            appendLine(section.title)
            section.rows.forEach { (label, value) ->
                appendLine("$label: $value")
            }
        }
    }.trim()

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, activity.title)
        putExtra(Intent.EXTRA_TEXT, shareText)
    }

    context.startActivity(
        Intent.createChooser(
            shareIntent,
            context.getString(R.string.activity_detail_share_chooser),
        ),
    )
}

internal fun shareBikeDetail(
    context: Context,
    bike: BikeCardUiModel,
) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, bike.title)
        putExtra(Intent.EXTRA_TEXT, bike.shareText)
    }

    context.startActivity(
        Intent.createChooser(
            shareIntent,
            context.getString(R.string.dashboard_bike_share_chooser),
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BikeDetailScreen(
    uiState: BikeDetailScreenUiState,
    onLoadBike: (String) -> Unit,
    bikeId: String,
    onNavigateBack: () -> Unit,
) {
    LaunchedEffect(bikeId) {
        onLoadBike(bikeId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.selectedBikeDetail?.title ?: stringResource(R.string.bike_detail_fallback_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.activity_detail_back))
                    }
                },
            )
        },
    ) { padding ->
        when {
            uiState.isBikeDetailLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.selectedBikeId == bikeId && uiState.selectedBikeDetail != null -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(
                        horizontal = DesignTokens.ScreenHorizontalPadding,
                        vertical = DesignTokens.ScreenVerticalPadding,
                    ),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.SectionSpacing),
                ) {
                    if (uiState.isBikeDetailRefreshing) {
                        item {
                            DashboardPageContainer {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                    item {
                        DashboardPageContainer {
                            val bikeDetailSubtitle = uiState.selectedBikeDetail.subtitle
                                ?: stringResource(R.string.bike_detail_subtitle).takeIf {
                                    uiState.showExplanationTexts
                                }
                            HeroCard(
                                eyebrow = stringResource(R.string.bike_detail_eyebrow),
                                title = uiState.selectedBikeDetail.title,
                                subtitle = bikeDetailSubtitle,
                            )
                        }
                    }
                    items(uiState.selectedBikeDetail.sections) { section ->
                        DashboardPageContainer {
                            DetailSectionCard(
                                section = section,
                                showExplanationTexts = uiState.showExplanationTexts,
                            )
                        }
                    }
                }
            }

            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(R.string.bike_detail_not_available))
                }
            }
        }
    }
}

@Composable
private fun DetailSectionCard(
    section: DetailSectionUiModel,
    activity: ActivityDetailUiModel? = null,
    onNavigateToTrack: (String) -> Unit = {},
    showExplanationTexts: Boolean = true,
) {
    var showExportDialog by rememberSaveable(activity?.id, section.title) { mutableStateOf(false) }
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                section.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            section.indicator?.let { indicator ->
                SectionIndicator(
                    indicator = indicator,
                    showExplanationTexts = showExplanationTexts,
                )
            }
            section.rows.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowItems.forEach { (label, value) ->
                        DetailValueTile(
                            label = label,
                            value = value,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        )
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            if (section.actions.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    section.actions.forEach { action ->
                        ActivityCardActionButton(
                            label = action.label,
                            icon = when (action.type) {
                                DetailSectionActionType.SHARE -> Icons.Default.Share
                                DetailSectionActionType.MAP -> Icons.Default.Map
                            },
                            onClick = {
                                when (action.type) {
                                    DetailSectionActionType.SHARE -> {
                                        if (activity != null) showExportDialog = true
                                    }

                                    DetailSectionActionType.MAP -> {
                                        activity?.let { onNavigateToTrack(it.id) }
                                    }
                                }
                            },
                            showLabel = showExplanationTexts,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
    if (showExportDialog && activity != null) {
        TrackExportDialog(
            activity = activity,
            onDismiss = { showExportDialog = false },
            onShare = { shareTrackGpx(context, activity) },
            onCopyGpx = {
                copyTrackGpxToClipboard(context, activity)
                Toast.makeText(context, context.getString(R.string.track_gpx_copied), Toast.LENGTH_SHORT).show()
            },
        )
    }
}

@Composable
private fun SectionIndicator(
    indicator: DetailSectionIndicatorUiModel,
    showExplanationTexts: Boolean,
) {
    val progressColor = when (indicator.tone) {
        DetailSectionIndicatorTone.POSITIVE -> MaterialTheme.colorScheme.primary
        DetailSectionIndicatorTone.INFORMATIVE -> MaterialTheme.colorScheme.tertiary
        DetailSectionIndicatorTone.WARNING -> MaterialTheme.colorScheme.secondary
        DetailSectionIndicatorTone.DANGER -> MaterialTheme.colorScheme.error
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = indicator.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = indicator.value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            LinearProgressIndicator(
                progress = { indicator.progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = progressColor,
                trackColor = progressColor.copy(alpha = 0.18f),
            )
            indicator.supportingText?.takeIf { showExplanationTexts }?.let { supportingText ->
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DetailValueTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
