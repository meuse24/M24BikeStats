package info.meuse24.m24bikestats.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import info.meuse24.m24bikestats.presentation.apitest.ApiTestViewModel
import info.meuse24.m24bikestats.presentation.apitest.ApiTestScreen
import info.meuse24.m24bikestats.presentation.login.LoginScreen
import info.meuse24.m24bikestats.presentation.login.LoginStatus
import info.meuse24.m24bikestats.presentation.login.LoginViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    // Activity-scoped: überlebt die Navigation zwischen Screens
    val loginViewModel: LoginViewModel = koinViewModel()

    val startDestination = if (loginViewModel.status is LoginStatus.Authenticated)
        "api_test" else "login"

    NavHost(navController = navController, startDestination = startDestination) {

        composable("login") {
            LoginScreen(
                status = loginViewModel.status,
                onBuildAuthIntent = loginViewModel::buildAuthIntent,
                onAuthResult = loginViewModel::handleAuthResult,
                onAuthenticated = {
                    navController.navigate("api_test") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("api_test") {
            val apiTestViewModel: ApiTestViewModel = koinViewModel()
            val uiState by apiTestViewModel.uiState.collectAsStateWithLifecycle()
            ApiTestScreen(
                uiState = uiState,
                onSelectEndpoint = apiTestViewModel::selectEndpoint,
                onFetch = apiTestViewModel::fetch,
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
