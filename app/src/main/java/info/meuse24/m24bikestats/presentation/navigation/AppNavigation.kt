package info.meuse24.m24bikestats.presentation.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import info.meuse24.m24bikestats.presentation.dashboard.ActivityDetailScreen
import info.meuse24.m24bikestats.presentation.dashboard.BikeDetailScreen
import info.meuse24.m24bikestats.presentation.dashboard.DashboardScreen
import info.meuse24.m24bikestats.presentation.dashboard.DashboardViewModel
import info.meuse24.m24bikestats.presentation.dashboard.TrackScreen
import info.meuse24.m24bikestats.presentation.login.LoginScreen
import info.meuse24.m24bikestats.presentation.login.LoginStatus
import info.meuse24.m24bikestats.presentation.login.LoginViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    val loginViewModel: LoginViewModel = koinViewModel()
    val isAuthenticated = loginViewModel.status is LoginStatus.Authenticated
    val dashboardViewModel: DashboardViewModel? = if (isAuthenticated) koinViewModel() else null
    val logoutLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        loginViewModel.handleLogoutResult(it.resultCode, it.data)
        navController.navigate("login") {
            popUpTo("dashboard") { inclusive = true }
        }
    }

    val startDestination = if (isAuthenticated)
        "dashboard" else "login"

    NavHost(navController = navController, startDestination = startDestination) {

        composable("login") {
            LoginScreen(
                status = loginViewModel.status,
                onBuildAuthIntent = loginViewModel::buildAuthIntent,
                onAuthResult = loginViewModel::handleAuthResult,
                onAuthenticated = {
                    navController.navigate("dashboard") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("dashboard") {
            val uiState by dashboardViewModel!!.uiState.collectAsStateWithLifecycle()
            DashboardScreen(
                uiState = uiState,
                onRefresh = dashboardViewModel::refresh,
                onLoadMoreActivities = dashboardViewModel::loadMoreActivities,
                onExportActivitiesCsv = dashboardViewModel::exportAllActivitiesCsv,
                onActivitiesCsvExportHandled = dashboardViewModel::onActivitiesCsvExportHandled,
                onNavigateToActivityDetail = { activityId ->
                    navController.navigate("activity/$activityId")
                },
                onNavigateToBikeDetail = { bikeId ->
                    navController.navigate("bike/$bikeId")
                },
                onLogout = {
                    val logoutIntent = loginViewModel.buildLogoutIntent()
                    if (logoutIntent != null) {
                        logoutLauncher.launch(logoutIntent)
                    } else {
                        loginViewModel.logoutLocally()
                        navController.navigate("login") {
                            popUpTo("dashboard") { inclusive = true }
                        }
                    }
                },
                onErrorShown = dashboardViewModel::clearError,
            )
        }

        composable(
            route = "activity/{activityId}",
            arguments = listOf(navArgument("activityId") { type = NavType.StringType })
        ) { backStackEntry ->
            val activityId = backStackEntry.arguments?.getString("activityId").orEmpty()
            val uiState by dashboardViewModel!!.uiState.collectAsStateWithLifecycle()
            ActivityDetailScreen(
                uiState = uiState,
                onLoadActivity = dashboardViewModel::loadActivityDetail,
                activityId = activityId,
                onNavigateToTrack = { navController.navigate("activity/$activityId/track") },
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(
            route = "activity/{activityId}/track",
            arguments = listOf(navArgument("activityId") { type = NavType.StringType })
        ) { backStackEntry ->
            val activityId = backStackEntry.arguments?.getString("activityId").orEmpty()
            val uiState by dashboardViewModel!!.uiState.collectAsStateWithLifecycle()
            TrackScreen(
                uiState = uiState,
                onLoadActivity = dashboardViewModel::loadActivityDetail,
                activityId = activityId,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(
            route = "bike/{bikeId}",
            arguments = listOf(navArgument("bikeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bikeId = backStackEntry.arguments?.getString("bikeId").orEmpty()
            val uiState by dashboardViewModel!!.uiState.collectAsStateWithLifecycle()
            BikeDetailScreen(
                uiState = uiState,
                onLoadBike = dashboardViewModel::loadBikeDetail,
                bikeId = bikeId,
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
