package info.meuse24.m24bikestats.presentation.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import info.meuse24.m24bikestats.BuildConfig

@Composable
fun HelpScreen(modifier: Modifier = Modifier) {
    val sections = listOf(
        "Login" to "Die Anmeldung nutzt Bosch SingleKey ID und den Bosch eBike Data Act Zugriff. Nach erfolgreichem Login landet die App in der Hauptnavigation.",
        "Aktivitäten" to "Die Tourenliste lädt lokal gecachte Aktivitäten zuerst und aktualisiert danach bei Bedarf im Hintergrund. Filter, Sortierung und Suche wirken direkt auf die geladene Liste.",
        "Bike" to "Der Bike-Bereich zeigt Bosch Smart System Komponenten, Batterien und Detailinformationen ebenfalls cache-first aus Room.",
        "Funktionen" to "CSV-Exporte greifen auf den sichtbaren Aktivitätssatz oder den kompletten Abruf zurück und öffnen anschließend das Android-Share-Sheet.",
        "Diagnose" to "Der API-Test-Screen bleibt als Diagnosewerkzeug verfügbar und ist bewusst nicht Teil der primären Navigation.",
    )
    InfoListScreen(
        title = "Hilfe",
        subtitle = "Kurzübersicht zu Navigation, Datenfluss und Diagnose.",
        items = sections,
        modifier = modifier,
    )
}

@Composable
fun InfoScreen(modifier: Modifier = Modifier) {
    val sections = listOf(
        "App" to "M24 Bike Stats",
        "Version" to BuildConfig.VERSION_NAME,
        "Build-Typ" to BuildConfig.BUILD_TYPE,
        "Application ID" to BuildConfig.APPLICATION_ID,
        "Lizenz" to "MIT",
        "Stack" to "Jetpack Compose, Navigation Compose, Koin, Room, AppAuth, MapLibre",
    )
    InfoListScreen(
        title = "Info",
        subtitle = "Versions- und Projektinformationen zur App.",
        items = sections,
        modifier = modifier,
    )
}

@Composable
private fun InfoListScreen(
    title: String,
    subtitle: String,
    items: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                    )
                }
            }
        }
        items(items) { (label, value) ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}
