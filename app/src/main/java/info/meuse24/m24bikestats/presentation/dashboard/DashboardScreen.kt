package info.meuse24.m24bikestats.presentation.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import info.meuse24.m24bikestats.R

private val dashboardTabs = listOf(
    R.string.nav_activities,
    R.string.nav_bike,
    R.string.nav_functions,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onRefresh: () -> Unit,
    onLoadMoreActivities: () -> Unit,
    onActivitySearchQueryChanged: (String) -> Unit,
    onActivityDateRangeFilterChanged: (ActivityDateRangeFilter) -> Unit,
    onActivitySortOptionChanged: (ActivitySortOption) -> Unit,
    onExportActivitiesCsv: () -> Unit,
    onExportActivityDetailsCsv: () -> Unit,
    onActivitiesCsvExportHandled: () -> Unit,
    onActivityDetailsCsvExportHandled: () -> Unit,
    onNavigateToActivityDetail: (String) -> Unit,
    onNavigateToBikeDetail: (String) -> Unit,
    onLogout: () -> Unit,
    onErrorShown: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            onErrorShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.app_name))
                        Text(
                            text = stringResource(dashboardTabs[selectedTabIndex]),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    if (selectedTabIndex != 2) {
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.cd_refresh))
                        }
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = stringResource(R.string.cd_logout))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (uiState.isInitialLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    ScrollableTabRow(selectedTabIndex = selectedTabIndex) {
                        dashboardTabs.forEachIndexed { index, titleRes ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = { Text(stringResource(titleRes)) },
                            )
                        }
                    }

                    when (selectedTabIndex) {
                        0 -> ActivitiesScreen(
                            uiState = uiState.toActivitiesUiState(),
                            onActivitySearchQueryChanged = onActivitySearchQueryChanged,
                            onActivityDateRangeFilterChanged = onActivityDateRangeFilterChanged,
                            onActivitySortOptionChanged = onActivitySortOptionChanged,
                            onActivityClick = onNavigateToActivityDetail,
                            onActivityMapClick = {},
                            onLoadMore = onLoadMoreActivities,
                        )

                        1 -> BikeListScreen(
                            uiState = uiState.toBikeListUiState(),
                            onBikeClick = onNavigateToBikeDetail,
                        )

                        else -> FunctionsScreen(
                            uiState = uiState.toFunctionsUiState(),
                            onExportActivitiesCsv = onExportActivitiesCsv,
                            onExportActivityDetailsCsv = onExportActivityDetailsCsv,
                            onCancelActivitiesCsvExport = {},
                            onCancelActivityDetailsCsvExport = {},
                            onActivitiesCsvExportHandled = onActivitiesCsvExportHandled,
                            onActivityDetailsCsvExportHandled = onActivityDetailsCsvExportHandled,
                        )
                    }
                }
            }
        }
    }
}
