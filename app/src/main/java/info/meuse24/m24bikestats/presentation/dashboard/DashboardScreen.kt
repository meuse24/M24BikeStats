package info.meuse24.m24bikestats.presentation.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onRefresh: () -> Unit,
    onLoadMoreActivities: () -> Unit,
    onNavigateToApiTest: () -> Unit,
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
                title = { Text("M24 Bike Stats") },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Aktualisieren")
                    }
                    IconButton(onClick = onNavigateToApiTest) {
                        Text("API")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Abmelden")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    ScrollableTabRow(selectedTabIndex = selectedTabIndex) {
                        listOf("Aktivitäten", "Bike").forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = { Text(title) }
                            )
                        }
                    }

                    when (selectedTabIndex) {
                        0 -> ActivitiesOverview(
                            uiState = uiState,
                            activities = uiState.activities,
                            onActivityClick = onNavigateToActivityDetail,
                            onLoadMore = onLoadMoreActivities,
                            isRefreshing = uiState.isRefreshing,
                        )
                        else -> BikesOverview(
                            bikes = uiState.bikes,
                            onBikeClick = onNavigateToBikeDetail,
                            isRefreshing = uiState.isRefreshing,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityDetailScreen(
    activity: ActivityDetailUiModel?,
    onNavigateBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(activity?.title ?: "Aktivität") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        }
    ) { padding ->
        if (activity == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text("Aktivität nicht gefunden")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(activity.title, style = MaterialTheme.typography.headlineSmall)
                }
                item {
                    DetailSectionCard(
                        section = DetailSectionUiModel(
                            title = "Kurzüberblick",
                            rows = activity.summary,
                        )
                    )
                }
                items(activity.sections) { section ->
                    DetailSectionCard(section)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BikeDetailScreen(
    uiState: DashboardUiState,
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
                title = { Text(uiState.selectedBikeDetail?.title ?: "Bike") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        }
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
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        uiState.selectedBikeDetail.subtitle?.let {
                            Text(it, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    items(uiState.selectedBikeDetail.sections) { section ->
                        DetailSectionCard(section)
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
                    Text("Bike-Details nicht verfügbar")
                }
            }
        }
    }
}

@Composable
private fun ActivitiesOverview(
    uiState: DashboardUiState,
    activities: List<ActivityCardUiModel>,
    onActivityClick: (String) -> Unit,
    onLoadMore: () -> Unit,
    isRefreshing: Boolean,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Aktivitäten", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "${uiState.loadedActivityCount} von ${uiState.activityTotalCount}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (isRefreshing) CircularProgressIndicator()
            }
        }
        items(activities, key = { it.id }) { activity ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onActivityClick(activity.id) }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(activity.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(activity.dateLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Distanz: ${activity.distanceLabel}")
                    Text("Dauer: ${activity.durationLabel}")
                    Text("Geschwindigkeit: ${activity.speedLabel}")
                    activity.powerLabel?.let { Text("Leistung: $it") }
                    activity.elevationLabel?.let { Text("Höhenmeter: $it") }
                    activity.caloriesLabel?.let { Text("Kalorien: $it") }
                }
            }
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
                        Text("Mehr Aktivitäten laden")
                    }
                }
            }
        }
    }
}

@Composable
private fun BikesOverview(
    bikes: List<BikeCardUiModel>,
    onBikeClick: (String) -> Unit,
    isRefreshing: Boolean,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Bike", style = MaterialTheme.typography.headlineSmall)
                if (isRefreshing) CircularProgressIndicator()
            }
        }
        items(bikes, key = { it.id }) { bike ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onBikeClick(bike.id) }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(bike.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    bike.subtitle?.let {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    bike.odometerLabel?.let { Text("Kilometerstand: $it") }
                    bike.assistSpeedLabel?.let { Text("Max. Unterstützung: $it") }
                    bike.batterySummary?.let { Text("Batterie: $it") }
                }
            }
        }
    }
}

@Composable
private fun DetailSectionCard(section: DetailSectionUiModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(section.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            section.rows.forEach { (label, value) ->
                DetailRow(label = label, value = value)
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
