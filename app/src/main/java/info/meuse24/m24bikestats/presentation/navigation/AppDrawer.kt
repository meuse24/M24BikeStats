package info.meuse24.m24bikestats.presentation.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import info.meuse24.m24bikestats.R
import info.meuse24.m24bikestats.presentation.navigation.model.DrawerDestination

@Composable
fun AppDrawer(
    currentRoute: String?,
    showExplanationTexts: Boolean,
    onHomeClicked: () -> Unit,
    onDestinationClicked: (DrawerDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalDrawerSheet(modifier = modifier.fillMaxHeight()) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 24.dp)) {
            Text(
                text = stringResource(R.string.drawer_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
                    .clickable(onClick = onHomeClicked)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (showExplanationTexts) {
                Text(
                    text = stringResource(R.string.drawer_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            DrawerDestination.availableEntries().forEach { destination ->
                NavigationDrawerItem(
                    label = { Text(stringResource(destination.labelRes)) },
                    selected = destination.route != null && currentRoute == destination.route,
                    onClick = { onDestinationClicked(destination) },
                    icon = {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = stringResource(destination.labelRes),
                        )
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                )
            }
        }
    }
}
