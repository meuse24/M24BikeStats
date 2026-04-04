package info.meuse24.m24bikestats.presentation.navigation.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import info.meuse24.m24bikestats.BuildConfig
import info.meuse24.m24bikestats.R

enum class DrawerDestination(
    val route: String?,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
    val debugOnly: Boolean = false,
) {
    SETUP(
        route = "setup",
        labelRes = R.string.nav_setup,
        icon = Icons.Default.Settings,
    ),
    HELP(
        route = "help",
        labelRes = R.string.nav_help,
        icon = Icons.AutoMirrored.Filled.HelpOutline,
    ),
    INFO(
        route = "info",
        labelRes = R.string.nav_info,
        icon = Icons.Default.Info,
    ),
    API_TEST(
        route = "api_test",
        labelRes = R.string.nav_api_test,
        icon = Icons.Default.BugReport,
        debugOnly = true,
    ),
    LOGOUT(
        route = null,
        labelRes = R.string.nav_logout,
        icon = Icons.AutoMirrored.Filled.ExitToApp,
    ),
    ;

    val isNavigationDestination: Boolean
        get() = route != null

    companion object {
        fun availableEntries(includeDebugTools: Boolean = BuildConfig.DEBUG): List<DrawerDestination> =
            entries.filter { !it.debugOnly || includeDebugTools }
    }
}
