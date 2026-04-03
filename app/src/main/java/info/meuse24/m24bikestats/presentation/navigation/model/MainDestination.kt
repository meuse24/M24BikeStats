package info.meuse24.m24bikestats.presentation.navigation.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.ElectricBike
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.ImageVector

enum class MainDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    HOME(
        route = "home",
        label = "Home",
        icon = Icons.Default.Home,
    ),
    ACTIVITIES(
        route = "activities",
        label = "Aktivitäten",
        icon = Icons.AutoMirrored.Filled.DirectionsBike,
    ),
    BIKE(
        route = "bike_list",
        label = "Bike",
        icon = Icons.Default.ElectricBike,
    ),
    FUNCTIONS(
        route = "functions",
        label = "Funktionen",
        icon = Icons.Default.FileDownload,
    ),
}
