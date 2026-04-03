package info.meuse24.m24bikestats.presentation.dashboard

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import info.meuse24.m24bikestats.R

@Composable
fun ActivitiesScreen(
    uiState: ActivitiesUiState,
    onActivitySearchQueryChanged: (String) -> Unit,
    onActivityDateRangeFilterChanged: (ActivityDateRangeFilter) -> Unit,
    onActivitySortOptionChanged: (ActivitySortOption) -> Unit,
    onActivityClick: (String) -> Unit,
    onActivityMapClick: (String) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val shouldLoadMore by remember(
        uiState.activities,
        uiState.canLoadMoreActivities,
        uiState.isLoadingMoreActivities,
        uiState.isRefreshing,
    ) {
        derivedStateOf {
            if (
                uiState.activities.isEmpty() ||
                !uiState.canLoadMoreActivities ||
                uiState.isLoadingMoreActivities ||
                uiState.isRefreshing
            ) {
                return@derivedStateOf false
            }

            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
            lastVisibleIndex >= uiState.activities.size + 1
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            onLoadMore()
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            HeroCard(
                eyebrow = stringResource(R.string.activities_hero_eyebrow),
                title = stringResource(R.string.activities_hero_title, uiState.visibleActivityCount, uiState.loadedActivityCount),
                subtitle = stringResource(R.string.activities_hero_subtitle),
            ) {
                SummaryChipRow(
                    summary = listOf(
                        stringResource(R.string.activities_metric_visible) to uiState.visibleActivityCount.toString(),
                        stringResource(R.string.activities_metric_loaded) to uiState.loadedActivityCount.toString(),
                        stringResource(R.string.activities_metric_total) to uiState.activityTotalCount.toString(),
                        stringResource(R.string.activities_metric_status) to if (uiState.isRefreshing) {
                            stringResource(R.string.activities_status_refreshing)
                        } else {
                            stringResource(R.string.activities_status_ready)
                        },
                    ),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    itemContent = { label, value ->
                        CompactMetricPill(label = label, value = value)
                    },
                )
            }
        }

        item {
            ActivityFilterSection(
                searchQuery = uiState.activitySearchQuery,
                onSearchQueryChanged = onActivitySearchQueryChanged,
                selectedDateRange = uiState.activityDateRangeFilter,
                selectedSortOption = uiState.activitySortOption,
                onDateRangeSelected = onActivityDateRangeFilterChanged,
                onSortOptionSelected = onActivitySortOptionChanged,
            )
        }

        if (uiState.activities.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ) {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.activities_empty_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(R.string.activities_empty_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        items(uiState.activities, key = { it.id }) { activity ->
            ActivityCard(
                activity = activity,
                onClick = { onActivityClick(activity.id) },
                onMapClick = { onActivityMapClick(activity.id) },
                onShareClick = { shareActivitySummary(context, activity) },
            )
        }

        item {
            if (uiState.isLoadingMoreActivities) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator()
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.activities_loading_more_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = stringResource(R.string.activities_loading_more_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

internal fun shareActivitySummary(
    context: Context,
    activity: ActivityCardUiModel,
) {
    val shareText = buildList {
        add(activity.title)
        add(activity.dateLabel)
        add("${context.getString(R.string.dashboard_label_distance)}: ${activity.distanceLabel}")
        add("${context.getString(R.string.dashboard_label_duration)}: ${activity.durationLabel}")
        add("${context.getString(R.string.dashboard_card_speed)}: ${activity.speedLabel}")
        activity.powerLabel?.let { add("${context.getString(R.string.dashboard_card_power)}: $it") }
        activity.elevationLabel?.let { add("${context.getString(R.string.dashboard_label_elevation)}: $it") }
        activity.caloriesLabel?.let { add("${context.getString(R.string.dashboard_label_calories)}: $it") }
    }.joinToString(separator = "\n")

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, activity.title)
        putExtra(Intent.EXTRA_TEXT, shareText)
    }

    context.startActivity(
        Intent.createChooser(
            shareIntent,
            context.getString(R.string.dashboard_activity_share_chooser),
        )
    )
}
