package info.meuse24.m24bikestats.presentation.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import info.meuse24.m24bikestats.BuildConfig
import info.meuse24.m24bikestats.R
import info.meuse24.m24bikestats.presentation.apitest.ApiTestContent
import info.meuse24.m24bikestats.presentation.apitest.ApiTestViewModel
import info.meuse24.m24bikestats.presentation.dashboard.ActivitiesScreen
import info.meuse24.m24bikestats.presentation.dashboard.ActivityDetailScreen
import info.meuse24.m24bikestats.presentation.dashboard.BikeDetailScreen
import info.meuse24.m24bikestats.presentation.dashboard.BikeListScreen
import info.meuse24.m24bikestats.presentation.dashboard.DashboardUiState
import info.meuse24.m24bikestats.presentation.dashboard.DashboardViewModel
import info.meuse24.m24bikestats.presentation.dashboard.FunctionsScreen
import info.meuse24.m24bikestats.presentation.dashboard.HomeScreen
import info.meuse24.m24bikestats.presentation.dashboard.PdfExportUiModel
import info.meuse24.m24bikestats.presentation.dashboard.TrackScreen
import info.meuse24.m24bikestats.presentation.dashboard.copyPdfReportToUri
import info.meuse24.m24bikestats.presentation.dashboard.createPdfShareIntent
import info.meuse24.m24bikestats.presentation.dashboard.toActivitiesUiState
import info.meuse24.m24bikestats.presentation.dashboard.toActivityDetailScreenUiState
import info.meuse24.m24bikestats.presentation.dashboard.toBikeDetailScreenUiState
import info.meuse24.m24bikestats.presentation.dashboard.toBikeListUiState
import info.meuse24.m24bikestats.presentation.dashboard.toFunctionsUiState
import info.meuse24.m24bikestats.presentation.dashboard.toHomeUiState
import info.meuse24.m24bikestats.presentation.dashboard.toTrackUiState
import info.meuse24.m24bikestats.presentation.login.LoginScreen
import info.meuse24.m24bikestats.presentation.login.LoginStatus
import info.meuse24.m24bikestats.presentation.login.LoginViewModel
import info.meuse24.m24bikestats.presentation.navigation.model.DrawerDestination
import info.meuse24.m24bikestats.presentation.navigation.model.MainDestination
import info.meuse24.m24bikestats.presentation.map.MapSummaryScreen
import info.meuse24.m24bikestats.presentation.map.MapSummaryViewModel
import info.meuse24.m24bikestats.presentation.statistics.StatisticsScreen
import info.meuse24.m24bikestats.presentation.statistics.StatisticsViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

private const val ROOT_LOGIN_ROUTE = "login"
private const val ROOT_MAIN_ROUTE = "main"
internal const val MAP_ROUTE = "map"

@Composable
fun AppNavigation() {
    val rootNavController = rememberNavController()
    val loginViewModel: LoginViewModel = koinViewModel()
    val loginStatus by loginViewModel.status.collectAsStateWithLifecycle()
    val isAuthenticated = loginStatus is LoginStatus.Authenticated
    val logoutLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        loginViewModel.handleLogoutResult(it.resultCode, it.data)
        rootNavController.navigate(ROOT_LOGIN_ROUTE) {
            popUpTo(rootNavController.graph.id) { inclusive = true }
        }
    }

    val startDestination = if (isAuthenticated) ROOT_MAIN_ROUTE else ROOT_LOGIN_ROUTE

    NavHost(
        navController = rootNavController,
        startDestination = startDestination,
    ) {
        composable(ROOT_LOGIN_ROUTE) {
            LoginScreen(
                status = loginStatus,
                onBuildAuthIntent = loginViewModel::buildAuthIntent,
                onAuthResult = loginViewModel::handleAuthResult,
                onAuthenticated = {
                    rootNavController.navigate(ROOT_MAIN_ROUTE) {
                        popUpTo(ROOT_LOGIN_ROUTE) { inclusive = true }
                    }
                },
            )
        }

        composable(ROOT_MAIN_ROUTE) {
            val context = LocalContext.current
            val shellNavController = rememberNavController()
            val shellBackStackEntry by shellNavController.currentBackStackEntryAsState()
            val currentRoute = shellBackStackEntry?.destination?.route
            val currentMainDestination = currentRoute.toMainDestination()
            val topBarTitle = currentRoute.toTopBarTitle()
            val showTopBar = currentRoute.shouldShowShellTopBar()
            val snackbarHostState = remember { SnackbarHostState() }
            val coroutineScope = rememberCoroutineScope()
            val dashboardViewModel: DashboardViewModel = koinViewModel()
            val dashboardUiState by dashboardViewModel.uiState.collectAsStateWithLifecycle()
            var pendingPdfDialogExport by remember { mutableStateOf<PdfExportUiModel?>(null) }
            var pendingPdfSaveExport by remember { mutableStateOf<PdfExportUiModel?>(null) }
            val savePdfLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("application/pdf"),
            ) { uri ->
                val export = pendingPdfSaveExport
                pendingPdfSaveExport = null
                pendingPdfDialogExport = null
                if (uri == null || export == null) return@rememberLauncherForActivityResult

                val result = copyPdfReportToUri(context, export, uri)
                result.exceptionOrNull()?.let { error ->
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            error.message ?: context.getString(R.string.dashboard_error_pdf_export),
                        )
                    }
                }
            }

            LaunchedEffect(dashboardUiState.error) {
                dashboardUiState.error?.let { message ->
                    snackbarHostState.showSnackbar(message)
                    dashboardViewModel.clearError()
                }
            }

            LaunchedEffect(dashboardUiState.pendingPdfExport) {
                dashboardUiState.pendingPdfExport?.let { export ->
                    pendingPdfDialogExport = export
                    dashboardViewModel.onPdfExportHandled()
                }
            }

            MainShell(
                currentMainDestination = currentMainDestination,
                currentRoute = currentRoute,
                topBarTitle = topBarTitle,
                onNavigateToOverview = {
                    shellNavController.navigate(MainDestination.HOME.route) {
                        popUpTo(shellNavController.graph.findStartDestination().id)
                        launchSingleTop = true
                    }
                },
                showTopBar = showTopBar,
                snackbarHostState = snackbarHostState,
                onMainDestinationSelected = { destination ->
                    if (destination == MainDestination.HOME) {
                        shellNavController.navigate(MainDestination.HOME.route) {
                            popUpTo(shellNavController.graph.findStartDestination().id)
                            launchSingleTop = true
                        }
                    } else {
                        shellNavController.navigate(destination.route) {
                            popUpTo(shellNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                onDrawerDestinationSelected = { destination ->
                    when (destination) {
                        DrawerDestination.LOGOUT -> {
                            val logoutIntent = loginViewModel.buildLogoutIntent()
                            if (logoutIntent != null) {
                                logoutLauncher.launch(logoutIntent)
                            } else {
                                loginViewModel.logoutLocally()
                                rootNavController.navigate(ROOT_LOGIN_ROUTE) {
                                    popUpTo(rootNavController.graph.id) { inclusive = true }
                                }
                            }
                        }

                        else -> {
                            shellNavController.navigate(destination.route!!) {
                                launchSingleTop = true
                            }
                        }
                    }
                },
                topBarActions = {
                    if (currentRoute.shouldShowPdfExportAction()) {
                        IconButton(
                            onClick = dashboardViewModel::exportPdfSummaryReport,
                            enabled = dashboardUiState.canStartPdfExport(),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = stringResource(R.string.cd_export_pdf_summary),
                            )
                        }
                    } else if (currentRoute.shouldShowRefreshAction()) {
                        IconButton(onClick = dashboardViewModel::refresh) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.cd_refresh),
                            )
                        }
                    }
                },
            ) { innerPadding ->
                NavHost(
                    navController = shellNavController,
                    startDestination = MainDestination.HOME.route,
                ) {
                    composable(MainDestination.HOME.route) {
                        HomeScreen(
                            uiState = dashboardUiState.toHomeUiState(),
                            onSyncCloudData = dashboardViewModel::syncCloudData,
                            onCancelSyncCloudData = dashboardViewModel::cancelCloudSync,
                            onNavigateToActivityDetail = { activityId ->
                                shellNavController.navigate("activity/$activityId")
                            },
                            onNavigateToActivityTrack = { activityId ->
                                shellNavController.navigate("activity/$activityId/track")
                            },
                            modifier = androidx.compose.ui.Modifier.padding(innerPadding),
                        )
                    }

                    composable(MainDestination.ACTIVITIES.route) {
                        ActivitiesScreen(
                            uiState = dashboardUiState.toActivitiesUiState(),
                            onActivitySearchQueryChanged = dashboardViewModel::updateActivitySearchQuery,
                            onActivityDateRangeFilterChanged = dashboardViewModel::updateActivityDateRangeFilter,
                            onActivitySortOptionChanged = dashboardViewModel::updateActivitySortOption,
                            onActivityClick = { activityId ->
                                shellNavController.navigate("activity/$activityId")
                            },
                            onActivityMapClick = { activityId ->
                                shellNavController.navigate("activity/$activityId/track")
                            },
                            onNavigateToMap = {
                                shellNavController.navigate(MAP_ROUTE) {
                                    launchSingleTop = true
                                }
                            },
                            onLoadMore = dashboardViewModel::loadMoreActivities,
                            modifier = androidx.compose.ui.Modifier.padding(innerPadding),
                        )
                    }

                    composable(MainDestination.BIKE.route) {
                        BikeListScreen(
                            uiState = dashboardUiState.toBikeListUiState(),
                            onBikeClick = { bikeId ->
                                shellNavController.navigate("bike/$bikeId")
                            },
                            modifier = androidx.compose.ui.Modifier.padding(innerPadding),
                        )
                    }

                    composable(MainDestination.STATISTICS.route) {
                        val statisticsViewModel: StatisticsViewModel = koinViewModel()
                        val statisticsUiState by statisticsViewModel.uiState.collectAsStateWithLifecycle()
                        StatisticsScreen(
                            uiState = statisticsUiState,
                            onGroupingSelected = statisticsViewModel::updateGrouping,
                            onPeriodSelected = statisticsViewModel::toggleSelectedPeriod,
                            modifier = androidx.compose.ui.Modifier.padding(innerPadding),
                        )
                    }

                    composable(MAP_ROUTE) {
                        val vm: MapSummaryViewModel = koinViewModel()
                        val uiState by vm.uiState.collectAsStateWithLifecycle()
                        val savedCameraPosition by vm.savedCameraPosition.collectAsStateWithLifecycle()
                        MapSummaryScreen(
                            uiState = uiState,
                            savedCameraPosition = savedCameraPosition,
                            onCameraPositionChanged = vm::saveCameraPosition,
                            onActivityClick = { activityId ->
                                shellNavController.navigate("activity/$activityId")
                            },
                            modifier = androidx.compose.ui.Modifier.padding(innerPadding),
                        )
                    }

                    composable(DrawerDestination.EXPORT.route!!) {
                        FunctionsScreen(
                            uiState = dashboardUiState.toFunctionsUiState(),
                            onExportActivitiesCsv = dashboardViewModel::exportAllActivitiesCsv,
                            onExportActivityDetailsCsv = dashboardViewModel::exportVisibleActivityDetailsCsv,
                            onExportPdf = dashboardViewModel::exportPdfSummaryReport,
                            onCancelActivitiesCsvExport = dashboardViewModel::cancelActivitiesCsvExport,
                            onCancelActivityDetailsCsvExport = dashboardViewModel::cancelActivityDetailsCsvExport,
                            onCancelPdfExport = dashboardViewModel::cancelPdfExport,
                            onActivitiesCsvExportHandled = dashboardViewModel::onActivitiesCsvExportHandled,
                            onActivityDetailsCsvExportHandled = dashboardViewModel::onActivityDetailsCsvExportHandled,
                            onPdfExportHandled = dashboardViewModel::onPdfExportHandled,
                            modifier = androidx.compose.ui.Modifier.padding(innerPadding),
                        )
                    }

                    composable(DrawerDestination.HELP.route!!) {
                        HelpScreen(modifier = androidx.compose.ui.Modifier.padding(innerPadding))
                    }

                    composable(DrawerDestination.SETUP.route!!) {
                        SetupScreen(
                            csvExportFormat = dashboardUiState.csvExportFormat,
                            cloudSyncDetailMode = dashboardUiState.cloudSyncDetailMode,
                            backgroundSyncMode = dashboardUiState.backgroundSyncMode,
                            onCsvExportFormatSelected = dashboardViewModel::updateCsvExportFormat,
                            onCloudSyncDetailModeSelected = dashboardViewModel::updateCloudSyncDetailMode,
                            onBackgroundSyncModeSelected = dashboardViewModel::updateBackgroundSyncMode,
                            modifier = androidx.compose.ui.Modifier.padding(innerPadding),
                        )
                    }

                    composable(DrawerDestination.INFO.route!!) {
                        InfoScreen(modifier = androidx.compose.ui.Modifier.padding(innerPadding))
                    }

                    if (BuildConfig.DEBUG) {
                        composable(DrawerDestination.API_TEST.route!!) {
                            val apiTestViewModel: ApiTestViewModel = koinViewModel()
                            val apiTestUiState by apiTestViewModel.uiState.collectAsStateWithLifecycle()
                            ApiTestContent(
                                uiState = apiTestUiState,
                                onSelectEndpoint = apiTestViewModel::selectEndpoint,
                                onFetch = apiTestViewModel::fetch,
                                onRunAll = apiTestViewModel::runAllEndpoints,
                                onClear = apiTestViewModel::clear,
                                modifier = androidx.compose.ui.Modifier.padding(innerPadding),
                            )
                        }
                    }

                    composable(
                        route = "activity/{activityId}",
                        arguments = listOf(navArgument("activityId") { type = NavType.StringType }),
                    ) { backStackEntry ->
                        val activityId = backStackEntry.arguments?.getString("activityId").orEmpty()
                        ActivityDetailScreen(
                            uiState = dashboardUiState.toActivityDetailScreenUiState(),
                            onLoadActivity = dashboardViewModel::loadActivityDetail,
                            onRefreshActivity = dashboardViewModel::refreshActivityDetail,
                            activityId = activityId,
                            onNavigateToTrack = { shellNavController.navigate("activity/$activityId/track") },
                            onNavigateBack = { shellNavController.popBackStack() },
                        )
                    }

                    composable(
                        route = "activity/{activityId}/track",
                        arguments = listOf(navArgument("activityId") { type = NavType.StringType }),
                    ) { backStackEntry ->
                        val activityId = backStackEntry.arguments?.getString("activityId").orEmpty()
                        TrackScreen(
                            uiState = dashboardUiState.toTrackUiState(),
                            onLoadActivity = dashboardViewModel::loadActivityDetail,
                            onRefreshActivity = dashboardViewModel::refreshActivityDetail,
                            activityId = activityId,
                            onNavigateBack = { shellNavController.popBackStack() },
                        )
                    }

                    composable(
                        route = "bike/{bikeId}",
                        arguments = listOf(navArgument("bikeId") { type = NavType.StringType }),
                    ) { backStackEntry ->
                        val bikeId = backStackEntry.arguments?.getString("bikeId").orEmpty()
                        BikeDetailScreen(
                            uiState = dashboardUiState.toBikeDetailScreenUiState(),
                            onLoadBike = dashboardViewModel::loadBikeDetail,
                            onRefreshBike = dashboardViewModel::refreshBikeDetail,
                            bikeId = bikeId,
                            onNavigateBack = { shellNavController.popBackStack() },
                        )
                    }
                }
            }

            pendingPdfDialogExport?.let { export ->
                AlertDialog(
                    onDismissRequest = { pendingPdfDialogExport = null },
                    title = { Text(stringResource(R.string.home_pdf_dialog_title)) },
                    text = { Text(stringResource(R.string.home_pdf_dialog_text, export.fileName)) },
                    confirmButton = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            TextButton(
                                onClick = {
                                    pendingPdfSaveExport = export
                                    savePdfLauncher.launch(export.fileName)
                                },
                            ) {
                                Text(stringResource(R.string.home_pdf_action_save))
                            }
                            TextButton(
                                onClick = {
                                    context.startActivity(
                                        android.content.Intent.createChooser(
                                            createPdfShareIntent(context, export),
                                            context.getString(R.string.functions_share_chooser_pdf),
                                        ),
                                    )
                                    pendingPdfDialogExport = null
                                },
                            ) {
                                Text(stringResource(R.string.home_pdf_action_share))
                            }
                            TextButton(onClick = { pendingPdfDialogExport = null }) {
                                Text(stringResource(R.string.common_cancel))
                            }
                        }
                    },
                )
            }
        }
    }
}

internal fun String?.toMainDestination(): MainDestination? =
    MainDestination.fromRoute(this)

internal fun String?.toTopBarTitleRes(): Int = when {
    this == MainDestination.ACTIVITIES.route -> MainDestination.ACTIVITIES.labelRes
    this == MainDestination.BIKE.route -> MainDestination.BIKE.labelRes
    this == MainDestination.STATISTICS.route -> MainDestination.STATISTICS.labelRes
    this == MAP_ROUTE -> R.string.nav_map
    this == DrawerDestination.EXPORT.route -> DrawerDestination.EXPORT.labelRes
    this == DrawerDestination.SETUP.route -> DrawerDestination.SETUP.labelRes
    this == DrawerDestination.HELP.route -> DrawerDestination.HELP.labelRes
    this == DrawerDestination.INFO.route -> DrawerDestination.INFO.labelRes
    this == DrawerDestination.API_TEST.route -> DrawerDestination.API_TEST.labelRes
    else -> MainDestination.HOME.labelRes
}

@Composable
internal fun String?.toTopBarTitle(): String = stringResource(toTopBarTitleRes())

internal fun String?.shouldShowShellTopBar(): Boolean = when {
    this?.startsWith("activity/") == true -> false
    this?.startsWith("bike/") == true -> false
    else -> true
}

internal fun String?.shouldShowRefreshAction(): Boolean = when (this) {
    MainDestination.ACTIVITIES.route,
    MainDestination.BIKE.route,
    MainDestination.STATISTICS.route,
    -> true

    else -> false
}

internal fun String?.shouldShowPdfExportAction(): Boolean =
    this == MainDestination.HOME.route

private fun DashboardUiState.canStartPdfExport(): Boolean =
    !isInitialLoading &&
        !isRefreshing &&
        !isSyncingCloudData &&
        !isExportingActivitiesCsv &&
        !isExportingActivityDetailsCsv &&
        !isExportingPdf
