package info.meuse24.m24bikestats.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BikeListScreen(
    bikes: List<BikeCardUiModel>,
    isRefreshing: Boolean,
    onBikeClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            HeroCard(
                eyebrow = "Bike-Profil",
                title = if (bikes.isEmpty()) {
                    "Kein Bike gefunden"
                } else {
                    "${bikes.size} Bike${if (bikes.size == 1) "" else "s"} verfügbar"
                },
                subtitle = if (isRefreshing) {
                    "Bike-Daten werden aktualisiert."
                } else {
                    "Komponenten-, Assist- und Batterieinformationen aus Bosch Smart System."
                },
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
