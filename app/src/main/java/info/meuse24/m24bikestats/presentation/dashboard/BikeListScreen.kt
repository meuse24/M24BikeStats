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
import info.meuse24.m24bikestats.R
import info.meuse24.m24bikestats.presentation.theme.DesignTokens

@Composable
fun BikeListScreen(
    uiState: BikeListUiState,
    onBikeClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = DesignTokens.ScreenHorizontalPadding,
            vertical = DesignTokens.ScreenVerticalPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SectionSpacing),
    ) {
        item {
            DashboardPageContainer {
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
        }

        items(uiState.bikes, key = { it.id }) { bike ->
            DashboardPageContainer {
                BikeOverviewCard(
                    bike = bike,
                    hasOidcCertificateInfo = uiState.hasOidcCertificateInfo,
                    onClick = { onBikeClick(bike.id) },
                    onShareClick = { shareBikeDetail(context, bike) },
                    showActionLabels = uiState.showExplanationTexts,
                )
            }
        }
    }
}
