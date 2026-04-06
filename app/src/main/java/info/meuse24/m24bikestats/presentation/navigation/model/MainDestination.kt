package info.meuse24.m24bikestats.presentation.navigation.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ElectricBike
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.annotation.StringRes
import info.meuse24.m24bikestats.R
import androidx.compose.ui.graphics.vector.ImageVector

enum class MainDestination(
    val route: String,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    HOME(
        route = "home",
        labelRes = R.string.nav_home,
        icon = Icons.Default.Home,
    ),
    ACTIVITIES(
        route = "activities",
        labelRes = R.string.nav_activities,
        icon = Icons.AutoMirrored.Filled.DirectionsBike,
    ),
    BIKE(
        route = "bike_list",
        labelRes = R.string.nav_bike,
        icon = Icons.Default.ElectricBike,
    ),
    STATISTICS(
        route = "statistics",
        labelRes = R.string.nav_statistics,
        icon = Icons.Default.BarChart,
    ),
    MAP(
        route = "map",
        labelRes = R.string.nav_map,
        icon = Icons.Default.Map,
    ),
    ;

    companion object {
        fun fromRoute(route: String?): MainDestination? =
            when {
                route == null -> null
                route == HOME.route -> HOME
                route == ACTIVITIES.route || route.startsWith("activity/") -> ACTIVITIES
                route == BIKE.route || route.startsWith("bike/") -> BIKE
                route == STATISTICS.route -> STATISTICS
                route == MAP.route -> MAP
                else -> null
            }
    }
}
