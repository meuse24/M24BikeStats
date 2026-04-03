package info.meuse24.m24bikestats.presentation.navigation.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.graphics.vector.ImageVector

enum class DrawerDestination(
    val route: String?,
    val label: String,
    val icon: ImageVector,
) {
    SETUP(
        route = "setup",
        label = "Setup",
        icon = Icons.Default.Settings,
    ),
    HELP(
        route = "help",
        label = "Hilfe",
        icon = Icons.AutoMirrored.Filled.HelpOutline,
    ),
    INFO(
        route = "info",
        label = "Info",
        icon = Icons.Default.Info,
    ),
    API_TEST(
        route = "api_test",
        label = "API-Test",
        icon = Icons.Default.BugReport,
    ),
    LOGOUT(
        route = null,
        label = "Logout",
        icon = Icons.AutoMirrored.Filled.ExitToApp,
    ),
    ;

    val isNavigationDestination: Boolean
        get() = route != null
}
