package info.meuse24.m24bikestats.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import info.meuse24.m24bikestats.presentation.apitest.ApiTestViewModel
import info.meuse24.m24bikestats.presentation.apitest.ApiTestScreen
import info.meuse24.m24bikestats.presentation.dashboard.ActivityDetailScreen
import info.meuse24.m24bikestats.presentation.dashboard.BikeDetailScreen
import info.meuse24.m24bikestats.presentation.dashboard.DashboardScreen
import info.meuse24.m24bikestats.presentation.dashboard.DashboardViewModel
import info.meuse24.m24bikestats.presentation.login.LoginScreen
import info.meuse24.m24bikestats.presentation.login.LoginStatus
import info.meuse24.m24bikestats.presentation.login.LoginViewModel
import androidx.navigation.navArgument
import org.koin.androidx.compose.koinViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    // Activity-scoped: überlebt die Navigation zwischen Screens
    val loginViewModel: LoginViewModel = koinViewModel()
    val isAuthenticated = loginViewModel.status is LoginStatus.Authenticated
    val dashboardViewModel: DashboardViewModel? = if (isAuthenticated) koinViewModel() else null

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
                onNavigateToApiTest = { navController.navigate("api_test") },
                onNavigateToActivityDetail = { activityId ->
                    navController.navigate("activity/$activityId")
                },
                onNavigateToBikeDetail = { bikeId ->
                    navController.navigate("bike/$bikeId")
                },
                onLogout = {
                    loginViewModel.logout()
                    navController.navigate("login") {
                        popUpTo("dashboard") { inclusive = true }
                    }
                },
                onErrorShown = dashboardViewModel::clearError,
            )
        }

        composable(
            route = "activity/{activityId}",
            arguments = listOf(navArgument("activityId") { type = NavType.StringType })
        ) { backStackEntry ->
            val activityId = backStackEntry.arguments?.getString("activityId")
            ActivityDetailScreen(
                activity = activityId?.let(dashboardViewModel!!::getActivityDetail),
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

        composable("api_test") {
            val apiTestViewModel: ApiTestViewModel = koinViewModel()
            val uiState by apiTestViewModel.uiState.collectAsStateWithLifecycle()
            ApiTestScreen(
                uiState = uiState,
                onSelectEndpoint = apiTestViewModel::selectEndpoint,
                onFetch = apiTestViewModel::fetch,
                onRunAll = apiTestViewModel::runAllEndpoints,
                onClear = apiTestViewModel::clear,
                onLogout = {
                    loginViewModel.logout()
                    navController.navigate("login") {
                        popUpTo("api_test") { inclusive = true }
                    }
                }
            )
        }
    }
}
