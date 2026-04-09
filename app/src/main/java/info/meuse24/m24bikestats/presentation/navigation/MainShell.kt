package info.meuse24.m24bikestats.presentation.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.window.core.layout.WindowSizeClass
import info.meuse24.m24bikestats.R
import info.meuse24.m24bikestats.presentation.navigation.model.DrawerDestination
import info.meuse24.m24bikestats.presentation.navigation.model.MainDestination
import kotlinx.coroutines.launch

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3AdaptiveApi::class,
)
@Composable
fun MainShell(
    currentMainDestination: MainDestination?,
    currentRoute: String?,
    topBarTitle: String,
    showExplanationTexts: Boolean,
    onMainDestinationSelected: (MainDestination) -> Unit,
    onDrawerDestinationSelected: (DrawerDestination) -> Unit,
    onNavigateToOverview: () -> Unit,
    modifier: Modifier = Modifier,
    showTopBar: Boolean = true,
    snackbarHostState: SnackbarHostState? = null,
    topBarActions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val appName = stringResource(R.string.app_name)
    val appNamePrefix = appName.substringBefore(' ')
    val appNameSuffix = appName.removePrefix(appNamePrefix).trimStart()
    val isDrawerRoute = isDrawerDestinationRoute(currentRoute)
    val isCompact = !adaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(
        WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND,
    )
    val isMapRoute = currentRoute.isMapRoute()
    val canOpenDrawer = isCompact && showTopBar && !isMapRoute
    val canNavigateToOverview = showTopBar && currentRoute.canNavigateToOverview()
    val showHomeBrandTitle = currentRoute == MainDestination.HOME.route
    val showOverflowDestinations = !isCompact && !isMapRoute
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    var overflowExpanded by remember { mutableStateOf(false) }

    val shellContent: @Composable () -> Unit = {
        NavigationSuiteScaffold(
            modifier = modifier.fillMaxSize(),
            navigationSuiteItems = {
                MainDestination.entries.forEach { destination ->
                    item(
                        icon = {
                            val destinationLabel = stringResource(destination.labelRes)
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destinationLabel,
                            )
                        },
                        label = if (showExplanationTexts) {
                            { Text(stringResource(destination.labelRes)) }
                        } else {
                            null
                        },
                        selected = currentMainDestination == destination,
                        onClick = { onMainDestinationSelected(destination) },
                    )
                }
            },
        ) {
            Scaffold(
                topBar = {
                    if (showTopBar) {
                        TopAppBar(
                            title = {
                                if (showHomeBrandTitle) {
                                    Text(
                                        buildAnnotatedString {
                                            withStyle(
                                                SpanStyle(
                                                    color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold,
                                                ),
                                            ) {
                                                append(appNamePrefix)
                                            }
                                            if (appNameSuffix.isNotBlank()) {
                                                append(" ")
                                                append(appNameSuffix)
                                            }
                                        },
                                    )
                                } else {
                                    Text(topBarTitle)
                                }
                            },
                            navigationIcon = {
                                when {
                                    canOpenDrawer -> {
                                        IconButton(
                                            onClick = {
                                                coroutineScope.launch {
                                                    drawerState.open()
                                                }
                                            },
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.MenuOpen,
                                                contentDescription = stringResource(R.string.cd_open_menu),
                                            )
                                        }
                                    }

                                    canNavigateToOverview -> {
                                        IconButton(onClick = onNavigateToOverview) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = stringResource(R.string.cd_navigate_overview),
                                            )
                                        }
                                    }
                                }
                            },
                            actions = {
                                topBarActions()
                                if (showOverflowDestinations) {
                                    IconButton(onClick = { overflowExpanded = true }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.MenuOpen,
                                            contentDescription = stringResource(R.string.cd_more_destinations),
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = overflowExpanded,
                                        onDismissRequest = { overflowExpanded = false },
                                    ) {
                                        DrawerDestination.availableEntries().forEach { destination ->
                                            DropdownMenuItem(
                                                text = { Text(stringResource(destination.labelRes)) },
                                                onClick = {
                                                    overflowExpanded = false
                                                    onDrawerDestinationSelected(destination)
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = destination.icon,
                                                        contentDescription = stringResource(destination.labelRes),
                                                    )
                                                },
                                            )
                                        }
                                    }
                                }
                            },
                        )
                    }
                },
                snackbarHost = {
                    snackbarHostState?.let { SnackbarHost(hostState = it) }
                },
            ) { innerPadding ->
                content(innerPadding)
            }
        }
    }

    if (canOpenDrawer) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = currentRoute != MAP_ROUTE,
            drawerContent = {
                AppDrawer(
                    currentRoute = currentRoute,
                    showExplanationTexts = showExplanationTexts,
                    onHomeClicked = {
                        coroutineScope.launch {
                            drawerState.close()
                        }
                        onNavigateToOverview()
                    },
                    onDestinationClicked = { destination ->
                        coroutineScope.launch {
                            drawerState.close()
                        }
                        onDrawerDestinationSelected(destination)
                    },
                )
            },
        ) {
            shellContent()
        }
    } else {
        shellContent()
    }
}

private fun isDrawerDestinationRoute(route: String?): Boolean =
    DrawerDestination.availableEntries(includeDebugTools = true).any { destination ->
        destination.route != null &&
            (route == destination.route || route?.startsWith("${destination.route}/") == true)
    }

internal fun String?.canNavigateToOverview(): Boolean = when (this) {
    null,
    MainDestination.HOME.route,
    DrawerDestination.EXPORT.route,
    DrawerDestination.SETUP.route,
    DrawerDestination.HELP.route,
    DrawerDestination.INFO.route,
    DrawerDestination.API_TEST.route,
    -> false

    else -> true
}
