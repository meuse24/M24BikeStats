package info.meuse24.m24bikestats.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import info.meuse24.m24bikestats.R

@Composable
fun BikeListScreen(
    uiState: BikeListUiState,
    onBikeClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            HeroCard(
                eyebrow = stringResource(R.string.bike_list_hero_eyebrow),
                title = if (uiState.bikes.isEmpty()) {
                    stringResource(R.string.bike_list_empty_title)
                } else {
                    stringResource(
                        if (uiState.bikes.size == 1) R.string.bike_list_count_title else R.string.bike_list_count_title_plural,
                        uiState.bikes.size,
                    )
                },
                subtitle = if (uiState.isRefreshing) {
                    stringResource(R.string.bike_list_refreshing_subtitle)
                } else if (uiState.showExplanationTexts) {
                    stringResource(R.string.bike_list_default_subtitle)
                } else {
                    null
                },
            )
        }

        items(uiState.bikes, key = { it.id }) { bike ->
            BikeOverviewCard(
                bike = bike,
                hasOidcCertificateInfo = uiState.hasOidcCertificateInfo,
                onClick = { onBikeClick(bike.id) },
                onShareClick = { shareBikeDetail(context, bike) },
            )
        }
    }
}
