# Plan: Adaptive App-Navigation und Menüstruktur implementieren

## Fortschritt
- [x] Phase 1: Fundament & Dependencies
- [x] Phase 2: Main-Shell & Drawer
- [x] Phase 3: Screen-Refactoring
- [ ] Phase 4: Navigation verdrahten
- [ ] Phase 5: Validierung & Tests

### Letztes Ergebnis
- 2026-04-03: `HomeScreen`, `ActivitiesScreen`, `BikeListScreen` und `FunctionsScreen` angelegt. `DashboardScreen` nutzt diese neuen Bereichs-Screens bereits als Alt-Host.
- Validierung nach Phase 3: `./gradlew test`, `./gradlew lint`, `./gradlew build` erfolgreich.
- 2026-04-03: `MainShell`, `AppDrawer` sowie Hilfe-/Info-Screens ergänzt. Compact nutzt einen `ModalNavigationDrawer`, größere Breiten ein Overflow-Menü für sekundäre Ziele.
- Validierung nach Phase 2: `./gradlew test`, `./gradlew lint`, `./gradlew build` erfolgreich.
- 2026-04-03: Compose BOM auf `2025.05.00` aktualisiert, Adaptive-Navigation- und `androidx.window`-Dependencies ergänzt, `MainDestination` und `DrawerDestination` angelegt.
- Validierung nach Phase 1: `./gradlew test`, `./gradlew lint`, `./gradlew build` erfolgreich.

## Zielsetzung
Die App soll von einer einfachen Tab-Navigation auf eine moderne, adaptive Navigationsstruktur umgestellt werden. Dies verbessert die Bedienbarkeit auf unterschiedlichen Geräten (Phone, Tablet, Foldable) und schafft eine klare Trennung zwischen fachlichen Hauptbereichen und systemnahen Funktionen.

## UI/UX-Konzept
Gemäß offizieller Android-Referenz für adaptive Apps wird eine Kombination aus **NavigationSuiteScaffold** und **ModalNavigationDrawer** implementiert.

### 1. Primärnavigation (Sichtbar)
Die wichtigsten fachlichen Bereiche sind direkt über die adaptive Navigation erreichbar:
- **Home**: Eine neue Übersicht mit den wichtigsten Kennzahlen und Schnellzugriffen.
- **Aktivitäten**: Die bekannte Liste der Touren (vorheriger Dashboard-Tab).
- **Bike**: Informationen zum eBike (vorheriger Dashboard-Tab).
- **Funktionen**: CSV-Export und Datenverwaltung (vorheriger Dashboard-Tab).

Diese Ziele erscheinen auf Phones in einer `NavigationBar` (unten) und auf größeren Bildschirmen automatisch in einer `NavigationRail` oder einem `NavigationDrawer` (seitlich). `NavigationSuiteScaffold` wählt die passende Variante automatisch anhand der `WindowAdaptiveInfo`.

### 2. Sekundärnavigation (Hamburger-Menü)
Seltene Aktionen und Info-Bereiche werden in einem `ModalNavigationDrawer` ausgelagert:
- **Hilfe**: Anleitungen und Support.
- **Info**: App-Version, Lizenzen, Mitwirkende.
- **Logout**: Beenden der Sitzung (aus der Top-Bar dorthin verschoben).

> **Wichtig – Drawer-Konflikt auf großen Bildschirmen:**  
> `NavigationSuiteScaffold` rendert auf Tablets und Foldables automatisch einen permanenten seitlichen `NavigationDrawer`. Ein zusätzlicher `ModalNavigationDrawer` als äußerer Wrapper würde auf diesen Geräten zu zwei überlagernden Drawer-Ebenen führen.  
> Lösung: Der `ModalNavigationDrawer` (Hamburger) wird **nur auf Phone-Klasse** (WindowWidthSizeClass.Compact) gerendert. Auf größeren Bildschirmen werden Hilfe/Info/Logout stattdessen in einem Overflow-Menü der `TopAppBar` oder direkt in `NavigationDrawerItem`s im permanenten Drawer untergebracht.

### 3. Login
Der Login-Screen bleibt eine eigenständige, unauthentifizierte Root-Destination außerhalb der Haupt-Shell.

### 4. ApiTestScreen
Der bestehende `ApiTestScreen` (`presentation/apitest/`) wird als Debug-Einstieg im Drawer (unter Hilfe/Info) oder als ausgeblendete Route beibehalten. Er soll nicht aus der Navigationsstruktur entfernt werden, solange er noch als Diagnose-Werkzeug genutzt wird.

---

## Technische Umsetzung

### Vorbereitung: Dependencies
Hinzufügen in `gradle/libs.versions.toml` und `app/build.gradle.kts`:

```toml
# libs.versions.toml
composeBom = "2025.05.00"   # Upgrade von 2024.09.00 – adaptive-navigation-suite stabil ab 2024.12
adaptiveNavigationSuite = { group = "androidx.compose.material3", name = "material3-adaptive-navigation-suite" }
windowCore = { group = "androidx.window", name = "window", version = "1.4.0" }
```

```kotlin
// app/build.gradle.kts
implementation(libs.adaptiveNavigationSuite)
implementation(libs.windowCore)  // für WindowAdaptiveInfo / WindowSizeClass
```

> **Hinweis:** `material3-adaptive-navigation-suite` und `androidx.window` werden über die BOM versioniert.  
> `androidx.window` kommt zwar transitiv, sollte aber explizit deklariert werden, da `currentWindowAdaptiveInfo()` direkt genutzt wird.

---

### Schritt 1: Navigations-Modell definieren
Neues Package `presentation/navigation/model/`:

```kotlin
// MainDestination.kt
enum class MainDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    HOME("home", "Home", Icons.Default.Home),
    ACTIVITIES("activities", "Aktivitäten", Icons.Default.DirectionsBike),
    BIKE("bike_list", "Bike", Icons.Default.ElectricBike),
    FUNCTIONS("functions", "Funktionen", Icons.Default.FileDownload),
}

// DrawerDestination.kt
enum class DrawerDestination(val label: String, val icon: ImageVector) {
    HELP("Hilfe", Icons.Default.HelpOutline),
    INFO("Info", Icons.Default.Info),
    LOGOUT("Logout", Icons.AutoMirrored.Filled.ExitToApp),
}
```

---

### Schritt 2: `MainShell` implementieren
Neue Datei `presentation/navigation/MainShell.kt`:

```kotlin
@Composable
fun MainShell(
    onLogout: () -> Unit,
    content: @Composable (NavController) -> Unit,
) {
    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val isCompact = windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val drawerContent: @Composable ColumnScope.() -> Unit = {
        AppDrawer(
            onDestinationClicked = { dest ->
                scope.launch { drawerState.close() }
                when (dest) {
                    DrawerDestination.LOGOUT -> onLogout()
                    DrawerDestination.HELP   -> navController.navigate("help")
                    DrawerDestination.INFO   -> navController.navigate("info")
                }
            }
        )
    }

    // ModalNavigationDrawer nur auf Compact (Phone); auf Rail/Drawer-Geräten entfällt er
    val shell: @Composable () -> Unit = {
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                MainDestination.entries.forEach { dest ->
                    item(
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) },
                        selected = currentRoute == dest.route,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        ) {
            content(navController)
        }
    }

    if (isCompact) {
        ModalNavigationDrawer(drawerState = drawerState, drawerContent = drawerContent) {
            shell()
        }
    } else {
        shell()
        // Hilfe/Info/Logout auf größeren Geräten über TopAppBar-OverflowMenu
    }
}
```

> **`drawerState.close()` ist eine suspend-Funktion** – darf nicht direkt in einen Lambda ohne Coroutine-Scope aufgerufen werden. Immer mit `scope.launch { drawerState.close() }`.

---

### Schritt 3: Zerlegung des `DashboardScreen`
Der bisherige monolithische `DashboardScreen` (Tab-basiert) wird aufgeteilt:

| Datei (neu) | Inhalt |
|---|---|
| `HomeScreen.kt` | Letzte Tour, Odometer, Export-Status, Schnellzugriffe |
| `ActivitiesScreen.kt` | Aktivitätenliste inkl. Filter, Suche, Paginierung |
| `BikeListScreen.kt` | Bike-Übersicht (vorher `BikesOverview`) |
| `FunctionsScreen.kt` | CSV-Export und Datenverwaltung |

Das `DashboardViewModel` bleibt als Shared ViewModel für alle vier Screens erhalten (über `koinViewModel()` aus demselben Backstack-Entry). Langfristig kann es modularisiert werden.

> **`dashboardViewModel!!` null check entfernen:**  
> In der aktuellen `AppNavigation.kt` wird `dashboardViewModel` als nullable erstellt und dann mit `!!` erzwungen. In der neuen `MainShell` wird das ViewModel erst innerhalb der authentifizierten Shell instanziiert – der `!!`-Operator entfällt.

---

### Schritt 4: `HomeScreen` als Landing-Page
- **Inhalt**: Letzte Tour (Karte/Kurzinfo via `MapLibre`), Gesamt-Odometer des Bikes, Status des letzten CSV-Exports, Schnellstart-Buttons zu Aktivitäten und Bike.
- **Daten**: Liest aus vorhandenem `DashboardUiState` (letzte Aktivität, Bike-Liste, Export-Status).
- **Startdestination**: `home` ersetzt `dashboard` als `startDestination` nach dem Login.

---

### Schritt 5: Anpassung `AppNavigation.kt`
- Äußere Struktur: `login`-Route außerhalb der Shell; authentifizierter Bereich innerhalb der `MainShell`.
- Die Route `dashboard` wird durch `home`, `activities`, `bike_list`, `functions` ersetzt.
- Detail-Routen (`activity/{activityId}`, `activity/{activityId}/track`, `bike/{bikeId}`) bleiben unverändert und werden innerhalb des MainShell-NavHosts registriert.
- **`popUpTo` im Logout-Flow:** Aktuell verweist der Code auf `popUpTo("dashboard") { inclusive = true }`. Dies muss auf die neue Startdestination umgestellt werden:

  ```kotlin
  navController.navigate("login") {
      popUpTo(navController.graph.id) { inclusive = true }
  }
  ```

---

## Implementierungsphasen

### Phase 1: Fundament & Dependencies
1. Compose BOM auf `2025.05.00` aktualisieren.
2. `material3-adaptive-navigation-suite` und `androidx.window` in `libs.versions.toml` und `build.gradle.kts` eintragen.
3. `MainDestination` und `DrawerDestination` Enums anlegen.

### Phase 2: Main-Shell & Drawer
1. `MainShell` Komponente mit `NavigationSuiteScaffold` erstellen.
2. `AppDrawer` für Hilfe, Info und Logout implementieren.
3. `TopAppBar` anpassen: Hamburger-Icon (Compact) bzw. Overflow-Menü (Rail/Drawer) für sekundäre Aktionen.

### Phase 3: Screen-Refactoring
1. `ActivitiesScreen`, `BikeListScreen` und `FunctionsScreen` aus `DashboardScreen` extrahieren.
2. `HomeScreen` neu entwerfen.
3. `DashboardScreen.kt` entfernen bzw. vollständig in neue Screens überführen.

### Phase 4: Navigation verdrahten
1. `AppNavigation.kt` auf neue Struktur umbauen (Login außen, MainShell innen).
2. `popUpTo`-Referenzen auf `"dashboard"` durch Graph-ID-basierte Variante ersetzen.
3. `dashboardViewModel!!` durch saubere Instanziierung innerhalb der Shell ersetzen.

### Phase 5: Validierung & Tests
1. Adaptive Darstellung im Emulator prüfen (Compact / Medium / Expanded).
2. Back-Stack-Verhalten verifizieren: Zurück aus Detail → Liste, nicht → Home.
3. Logout-Flow aus Drawer und Overflow-Menü testen.
4. Bestehende ViewModel- und Navigationstests auf neue Routen anpassen.

---

## Vorteile der neuen Struktur
- **Skalierbarkeit**: Neue Bereiche können einfach hinzugefügt werden (Drawer oder Navigation Suite).
- **Adaptivität**: Die App nutzt den Platz auf Tablets und Foldables optimal aus.
- **Klarheit**: Fachliche Inhalte stehen im Fokus, administrative Aufgaben sind dezent im Menü platziert.
- **Zukunftssicherheit**: Einsatz moderner Material 3 Adaptive Komponenten gemäß Android-Empfehlung.
- **Robustheit**: Kein `!!`-Operator mehr für das ViewModel; Drawer-Konflikt auf großen Bildschirmen strukturell gelöst.
