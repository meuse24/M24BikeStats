package info.meuse24.m24bikestats.presentation.navigation

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalUriHandler
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
    val appInfo = listOf(
        "App" to "M24 Bike Stats",
        "Version" to BuildConfig.VERSION_NAME,
        "Build-Typ" to BuildConfig.BUILD_TYPE,
        "Application ID" to BuildConfig.APPLICATION_ID,
        "Lizenz" to "MIT",
        "Copyright" to "(c) 2026 meuse24, Author: Guenther Meusburger",
        "Repository" to "https://github.com/meuse24/M24BikeStats",
    )
    val libraries = listOf(
        "Kotlin" to "2.2.10",
        "Android Gradle Plugin" to "9.1.0",
        "Jetpack Compose BOM" to "2025.05.00",
        "Material 3 Adaptive Navigation Suite" to "Compose BOM",
        "Navigation Compose" to "2.8.9",
        "Lifecycle Runtime / Compose" to "2.10.0",
        "Activity Compose" to "1.13.0",
        "Koin" to "4.0.2",
        "Room" to "2.8.4",
        "AppAuth" to "0.11.1",
        "OkHttp" to "4.12.0",
        "AndroidX Security Crypto" to "1.1.0-alpha06",
        "AndroidX Window" to "1.4.0",
        "MapLibre Compose" to "0.12.1",
    )
    val credits = listOf(
        "BOSCH" to "Bosch eBike Systems liefert das Smart System, die zugehoerigen Cloud-Schnittstellen und damit die technische Grundlage fuer die in dieser App angezeigten Fahr- und Bike-Daten.",
        "EU Data Act" to "Die EU-Datenverordnung, Regulation (EU) 2023/2854, definiert harmonisierte Regeln fuer fairen Zugang zu und faire Nutzung von Produkt- und Servicedaten und bildet damit den regulatorischen Rahmen fuer solche Datenzugriffe.",
        "OpenAI Codex" to "Codex von OpenAI ist ein agentischer Software-Engineering-Assistent, der Codebasen lesen, aendern, testen und Aufgaben in isolierten Umgebungen ausfuehren kann.",
        "Anthropic Claude Code" to "Claude Code von Anthropic ist ein agentisches Coding-System fuer Terminal, IDE und Web, das projektweit planen, Dateien aendern, Tests ausfuehren und Entwicklungswerkzeuge direkt nutzen kann.",
        "Google Gemini CLI" to "Gemini CLI von Google ist ein quelloffener KI-Agent fuer das Terminal, der Gemini direkt in die Kommandozeile bringt und fuer Coding, Recherche, Automatisierung und Problemloesung ausgelegt ist.",
    )
    InfoSectionScreen(
        title = "Info",
        subtitle = "Versions-, Lizenz-, Bibliotheks- und Credit-Informationen zur App.",
        sections = listOf(
            "Projekt" to appInfo,
            "Bibliotheken" to libraries,
            "Credits" to credits,
        ),
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
    InfoSectionScreen(
        title = title,
        subtitle = subtitle,
        sections = listOf("" to items),
        modifier = modifier,
    )
}

@Composable
private fun InfoSectionScreen(
    title: String,
    subtitle: String,
    sections: List<Pair<String, List<Pair<String, String>>>>,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
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
        sections.forEach { (sectionTitle, items) ->
            if (sectionTitle.isNotBlank()) {
                item {
                    Text(
                        text = sectionTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                    )
                }
            }
            items(items) { (label, value) ->
                val isUrl = value.startsWith("https://") || value.startsWith("http://")
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
                            color = if (isUrl) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            modifier = if (isUrl) {
                                Modifier.clickable { uriHandler.openUri(value) }
                            } else {
                                Modifier
                            },
                        )
                    }
                }
            }
        }
    }
}
