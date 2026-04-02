package info.meuse24.m24bikestats.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import info.meuse24.m24bikestats.presentation.apitest.ApiTestContent
import info.meuse24.m24bikestats.presentation.apitest.ApiTestUiState
import info.meuse24.m24bikestats.domain.model.BoschEndpoint

private val dashboardTabs = listOf("Aktivitäten", "Bike", "API-Test")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    apiTestUiState: ApiTestUiState,
    onRefresh: () -> Unit,
    onLoadMoreActivities: () -> Unit,
    onSelectApiEndpoint: (BoschEndpoint) -> Unit,
    onFetchApiEndpoint: () -> Unit,
    onRunAllApiEndpoints: () -> Unit,
    onClearApiOutput: () -> Unit,
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
                        Text("M24 Bike Stats")
                        Text(
                            text = dashboardTabs[selectedTabIndex],
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    if (selectedTabIndex != 2) {
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Default.Refresh, contentDescription = "Aktualisieren")
                        }
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Abmelden")
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
                        dashboardTabs.forEachIndexed { index, title ->
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
                        )
                        1 -> BikesOverview(
                            bikes = uiState.bikes,
                            isRefreshing = uiState.isRefreshing,
                            onBikeClick = onNavigateToBikeDetail,
                        )
                        else -> ApiTestContent(
                            uiState = apiTestUiState,
                            onSelectEndpoint = onSelectApiEndpoint,
                            onFetch = onFetchApiEndpoint,
                            onRunAll = onRunAllApiEndpoints,
                            onClear = onClearApiOutput,
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
    uiState: DashboardUiState,
    onLoadActivity: (String) -> Unit,
    activityId: String,
    onNavigateBack: () -> Unit,
) {
    LaunchedEffect(activityId) {
        onLoadActivity(activityId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.selectedActivityDetail?.title ?: "Aktivität") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        }
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
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        HeroCard(
                            eyebrow = "Aktivität",
                            title = activity.title,
                            subtitle = activity.subtitle ?: "Detaildaten aus Bosch Smart System",
                        ) {
                            SummaryChipRow(activity.summary)
                        }
                    }
                    items(activity.sections) { section ->
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
                    Text("Aktivität nicht gefunden")
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
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        HeroCard(
                            eyebrow = "Smart System Bike",
                            title = uiState.selectedBikeDetail.title,
                            subtitle = uiState.selectedBikeDetail.subtitle ?: "Komponenten und Batteriesystem",
                        )
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
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            HeroCard(
                eyebrow = "Tourenübersicht",
                title = "${uiState.loadedActivityCount} von ${uiState.activityTotalCount} Aktivitäten geladen",
                subtitle = "Die App liest aktuell die bestätigten Summary-Daten aus Bosch Smart System.",
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        MetricPill(
                            label = "Geladen",
                            value = uiState.loadedActivityCount.toString(),
                        )
                    }
                    item {
                        MetricPill(
                            label = "Gesamt",
                            value = uiState.activityTotalCount.toString(),
                        )
                    }
                    item {
                        MetricPill(
                            label = "Status",
                            value = if (uiState.isRefreshing) "Aktualisiert..." else "Bereit",
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
    isRefreshing: Boolean,
    onBikeClick: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            HeroCard(
                eyebrow = "Bike-Profil",
                title = if (bikes.isEmpty()) "Kein Bike gefunden" else "${bikes.size} Bike${if (bikes.size == 1) "" else "s"} verfügbar",
                subtitle = if (isRefreshing) "Bike-Daten werden aktualisiert." else "Komponenten-, Assist- und Batterieinformationen aus Bosch Smart System.",
            )
        }

        items(bikes, key = { it.id }) { bike ->
            BikeOverviewCard(
                bike = bike,
                onClick = { onBikeClick(bike.id) },
            )
        }
    }
}

@Composable
private fun HeroCard(
    eyebrow: String,
    title: String,
    subtitle: String,
    content: @Composable (() -> Unit)? = null,
) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            colorScheme.primaryContainer,
                            colorScheme.secondaryContainer,
                        )
                    )
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = eyebrow,
                style = MaterialTheme.typography.labelLarge,
                color = colorScheme.onPrimaryContainer,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
            )
            content?.invoke()
        }
    }
}

@Composable
private fun ActivityCard(
    activity: ActivityCardUiModel,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activity.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = activity.dateLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StatusBadge(activity.distanceLabel)
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { MetricPill("Dauer", activity.durationLabel) }
                item { MetricPill("Tempo", activity.speedLabel) }
                activity.powerLabel?.let { item { MetricPill("Leistung", it) } }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OptionalRow("Höhenmeter", activity.elevationLabel)
                OptionalRow("Kalorien", activity.caloriesLabel)
            }
        }
    }
}

@Composable
private fun BikeOverviewCard(
    bike: BikeCardUiModel,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
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
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                bike.odometerLabel?.let { item { MetricPill("Kilometer", it) } }
                bike.assistSpeedLabel?.let { item { MetricPill("Assist", it) } }
            }
            SectionSurface {
                OptionalRow("Batterie", bike.batterySummary)
                Text(
                    text = "Tippe für Komponenten-, Batterie- und Head-Unit-Details.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DetailSectionCard(section: DetailSectionUiModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                section.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(10.dp))
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun SummaryChipRow(summary: List<Pair<String, String>>) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(summary) { (label, value) ->
            MetricPill(label = label, value = value)
        }
    }
}

@Composable
private fun MetricPill(
    label: String,
    value: String,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
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

@Composable
private fun StatusBadge(value: String) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun SectionSurface(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun OptionalRow(
    label: String,
    value: String?,
) {
    if (value == null) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}
