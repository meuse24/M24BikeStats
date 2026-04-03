package info.meuse24.m24bikestats.presentation.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import info.meuse24.m24bikestats.presentation.navigation.model.DrawerDestination

@Composable
fun AppDrawer(
    onDestinationClicked: (DrawerDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalDrawerSheet(modifier = modifier.fillMaxHeight()) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 24.dp)) {
            Text(
                text = "M24 Bike Stats",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Text(
                text = "Hilfe, Info und Diagnosewerkzeuge",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            DrawerDestination.entries.forEach { destination ->
                NavigationDrawerItem(
                    label = { Text(destination.label) },
                    selected = false,
                    onClick = { onDestinationClicked(destination) },
                    icon = {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = destination.label,
                        )
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                )
            }
        }
    }
}
