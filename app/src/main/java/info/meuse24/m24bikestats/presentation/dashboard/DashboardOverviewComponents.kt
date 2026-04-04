package info.meuse24.m24bikestats.presentation.dashboard

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import info.meuse24.m24bikestats.R

@Composable
internal fun ActivitiesOverview(
    uiState: DashboardUiState,
    activities: List<ActivityCardUiModel>,
    onActivitySearchQueryChanged: (String) -> Unit,
    onActivityDateRangeFilterChanged: (ActivityDateRangeFilter) -> Unit,
    onActivitySortOptionChanged: (ActivitySortOption) -> Unit,
    onActivityClick: (String) -> Unit,
    onLoadMore: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            HeroCard(
                eyebrow = stringResource(R.string.activities_hero_eyebrow),
                title = stringResource(R.string.activities_hero_title, uiState.visibleActivityCount, uiState.loadedActivityCount),
                subtitle = stringResource(R.string.activities_hero_subtitle),
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { MetricPill(label = stringResource(R.string.activities_metric_visible), value = uiState.visibleActivityCount.toString()) }
                    item { MetricPill(label = stringResource(R.string.activities_metric_loaded), value = uiState.loadedActivityCount.toString()) }
                    item { MetricPill(label = stringResource(R.string.activities_metric_total), value = uiState.activityTotalCount.toString()) }
                    item {
                        MetricPill(
                            label = stringResource(R.string.activities_metric_status),
                            value = if (uiState.isRefreshing) {
                                stringResource(R.string.activities_status_refreshing)
                            } else {
                                stringResource(R.string.activities_status_ready)
                            },
                        )
                    }
                }
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

        if (activities.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                ) {
                    Column(
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

        items(activities, key = { it.id }) { activity ->
            ActivityCard(
                activity = activity,
                onClick = { onActivityClick(activity.id) },
            )
        }

        item {
            when {
                uiState.isLoadingMoreActivities -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.canLoadMoreActivities -> {
                    OutlinedButton(
                        onClick = onLoadMore,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.activities_load_more))
                    }
                }
            }
        }
    }
}

@Composable
internal fun ActivityFilterSection(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    selectedDateRange: ActivityDateRangeFilter,
    selectedSortOption: ActivitySortOption,
    onDateRangeSelected: (ActivityDateRangeFilter) -> Unit,
    onSortOptionSelected: (ActivitySortOption) -> Unit,
) {
    var isSearchVisible by rememberSaveable { mutableStateOf(searchQuery.isNotBlank()) }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            isSearchVisible = true
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.filter_section_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                FilledTonalIconButton(
                    onClick = {
                        if (isSearchVisible && searchQuery.isBlank()) {
                            isSearchVisible = false
                        } else {
                            isSearchVisible = true
                        }
                    },
                ) {
                    Icon(
                        imageVector = if (isSearchVisible && searchQuery.isBlank()) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = if (isSearchVisible && searchQuery.isBlank()) {
                            stringResource(R.string.filter_search_clear)
                        } else {
                            stringResource(R.string.filter_search_label)
                        },
                    )
                }
            }

            if (isSearchVisible || searchQuery.isNotBlank()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChanged,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.filter_search_placeholder)) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = if (searchQuery.isNotBlank()) {
                        {
                            IconButton(onClick = { onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.filter_search_clear))
                            }
                        }
                    } else {
                        null
                    },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CompactFilterDropdown(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.filter_date_range),
                    selectedLabel = stringResource(selectedDateRange.labelRes),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                        )
                    },
                ) { dismiss ->
                    ActivityDateRangeFilter.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(stringResource(option.labelRes)) },
                            onClick = {
                                onDateRangeSelected(option)
                                dismiss()
                            },
                        )
                    }
                }

                CompactFilterDropdown(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.filter_sorting),
                    selectedLabel = stringResource(selectedSortOption.labelRes),
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = null,
                        )
                    },
                ) { dismiss ->
                    ActivitySortOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(stringResource(option.labelRes)) },
                            onClick = {
                                onSortOptionSelected(option)
                                dismiss()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactFilterDropdown(
    label: String,
    selectedLabel: String,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    content: @Composable ((dismiss: () -> Unit) -> Unit),
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        ) {
            icon()
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Text(
                    text = selectedLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            content { expanded = false }
        }
    }
}

@Composable
internal fun BikesOverview(
    bikes: List<BikeCardUiModel>,
    isRefreshing: Boolean,
    onBikeClick: (String) -> Unit,
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            HeroCard(
                eyebrow = stringResource(R.string.bike_list_hero_eyebrow),
                title = if (bikes.isEmpty()) {
                    stringResource(R.string.bike_list_empty_title)
                } else {
                    stringResource(
                        if (bikes.size == 1) R.string.bike_list_count_title else R.string.bike_list_count_title_plural,
                        bikes.size,
                    )
                },
                subtitle = if (isRefreshing) {
                    stringResource(R.string.bike_list_refreshing_subtitle)
                } else {
                    stringResource(R.string.bike_list_default_subtitle)
                },
            )
        }

        items(bikes, key = { it.id }) { bike ->
            BikeOverviewCard(
                bike = bike,
                onClick = { onBikeClick(bike.id) },
                onShareClick = { shareBikeDetail(context, bike) },
            )
        }
    }
}

@Composable
internal fun ActivityCard(
    activity: ActivityCardUiModel,
    onClick: () -> Unit,
    onMapClick: (() -> Unit)? = null,
    onShareClick: (() -> Unit)? = null,
    primaryActionLabel: String? = null,
) {
    val detailActionLabel = primaryActionLabel ?: stringResource(R.string.dashboard_activity_detail_button)
    val metricSummary = buildList {
        add(stringResource(R.string.dashboard_card_speed) to activity.speedLabel.withLineBreakBeforeMax())
        activity.powerLabel?.let { add(stringResource(R.string.dashboard_card_power) to it.withLineBreakBeforeMax()) }
        activity.elevationLabel?.let { add(stringResource(R.string.dashboard_label_elevation) to it) }
        activity.caloriesLabel?.let { add(stringResource(R.string.dashboard_label_calories) to it) }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = activity.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = activity.dateLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusBadge(activity.durationLabel)
                    StatusBadge(activity.distanceLabel)
                }
            }

            ActivityMetricGrid(summary = metricSummary)

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ActivityCardActionButton(
                        label = detailActionLabel,
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                        onClick = onClick,
                        modifier = Modifier.weight(1f),
                    )
                    onMapClick?.let { navigateToMap ->
                        ActivityCardActionButton(
                            label = stringResource(R.string.dashboard_activity_map_button),
                            icon = Icons.Default.Map,
                            onClick = navigateToMap,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    onShareClick?.let { share ->
                        ActivityCardActionButton(
                            label = stringResource(R.string.dashboard_activity_share_button),
                            icon = Icons.Default.Share,
                            onClick = share,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityMetricGrid(summary: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        summary.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowItems.forEach { (label, value) ->
                    ActivityMetricTile(
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
    }
}

@Composable
private fun ActivityMetricTile(
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
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
internal fun ActivityCardActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun String.withLineBreakBeforeMax(): String =
    replace(" • max ", "\nmax ")
        .replace(", max ", "\nmax ")

@Composable
internal fun BikeOverviewCard(
    bike: BikeCardUiModel,
    onClick: () -> Unit,
    onShareClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = bike.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                bike.subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            SummaryChipRow(
                summary = buildList {
                    bike.odometerLabel?.let { add(stringResource(R.string.dashboard_card_odometer) to it) }
                    bike.assistSpeedLabel?.let { add(stringResource(R.string.dashboard_card_assist) to it) }
                    bike.walkAssistLabel?.let { add(stringResource(R.string.dashboard_card_walk_assist) to it) }
                },
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                itemContent = { label, value ->
                    CompactMetricPill(label = label, value = value)
                },
            )
            bike.batterySummary?.let { batterySummary ->
                BikeInfoSurface(
                    label = stringResource(R.string.dashboard_battery_fallback_title),
                    value = batterySummary,
                )
            }
            bike.powerOnSummary?.let { powerOnSummary ->
                BikeInfoSurface(
                    label = stringResource(R.string.dashboard_card_usage),
                    value = powerOnSummary,
                )
            }
            bike.assistModesSummary?.let { assistModesSummary ->
                BikeInfoSurface(
                    label = stringResource(R.string.dashboard_card_assist_ranges),
                    value = assistModesSummary,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BikeCardActionButton(
                    label = stringResource(R.string.dashboard_bike_share_button),
                    icon = Icons.Default.Share,
                    onClick = onShareClick,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
                BikeCardActionButton(
                    label = stringResource(R.string.dashboard_bike_detail_button),
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    onClick = onClick,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun BikeCardActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun BikeInfoSurface(
    label: String,
    value: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
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
