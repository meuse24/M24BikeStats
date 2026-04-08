# CLAUDE.md

Arbeitsnotizen für Agenten in diesem Repository.

## Projekt

Single-Module Android-App mit Jetpack Compose, Room, Koin und AppAuth.

- Package: `info.meuse24.m24bikestats`
- `minSdk`: 29
- `targetSdk`: 36
- Kotlin: `2.2.10`
- AGP: `9.1.0`

## Standard-Checks

```bash
./gradlew test
./gradlew lint
./gradlew build
./gradlew assembleRelease
```

Optional:

```bash
./gradlew assembleDebug
./gradlew connectedAndroidTest
```

## Architektur

```text
domain/
  model/       Bosch- und App-Modelle
  repository/  Interfaces
  usecase/     fachliche Anwendungsfälle

api/
  BoschEndpoint   (API-Test-/Diagnose-Endpunktkatalog)

data/
  remote/      Bosch API Zugriff
  local/       Room, Mapper, Cache-State
  repository/  Repository-Implementierungen

auth/
  AuthManager
  AuthFlowCoordinator
  OAuthConfig
  OidcAccountInfo
  OidcCertificateInfo

background/
  BackgroundSyncScheduler
  BackgroundSyncSettingsObserver

presentation/
  login/
  apitest/
  dashboard/
  statistics/
  map/
  navigation/

shared/
  TokenInfoFormat
  weitere app-weite Hilfsfunktionen

di/
  AppModule
```

Regeln:

- `domain` bleibt Android-frei
- `data` hängt nur an `domain`
- `presentation` hängt an `domain` und `auth`
- `auth` kapselt Android-/AppAuth-spezifische Login- und OIDC-Helfer
- ViewModels exponieren `StateFlow`
- Screens sind stateless und bekommen `UiState` plus Callbacks
- Dashboard-UI ist nach Verantwortungen getrennt:
  - `DashboardScreen.kt` = Shell
  - `DashboardOverviewComponents.kt` = Listen/Karten/Filter
  - `DashboardDetailScreens.kt` = Activity-/Bike-Details
  - `DashboardTrackScreen.kt` = Track-Vollbild + Export
  - `DashboardSharedUi.kt` = gemeinsame Compose-Bausteine
- Statistik-UI liegt separat in `presentation/statistics/`:
  - `StatisticsScreen.kt` = Compose-Screen (stateless)
  - `StatisticsViewModel.kt` = Grouping-State, Period-Selektion, `combine`-Pipeline
- `StatisticsUiModelMapper.kt` = `ActivityStatisticsOverview` → lokalisierte `StatisticsUiState`, Android-frei
- `StatisticsUiState.kt` = Datenmodelle `StatisticsUiState`, `PeriodStats`, `StatisticsHighlights`, `StatisticsGrouping` plus testbare Formatierungs-Helfer
- Statistik-Aggregation liegt fachlich in `domain/usecase/GetStatisticsUseCase.kt` und liefert `ActivityStatisticsOverview` inkl. Highlights und abgedecktem Zeitraum; `StatisticsUiModelMapper` mappt dieses Aggregat in lokalisierte UI-Daten
- Weltkarte liegt in `presentation/map/`:
  - `MapSummaryScreen.kt` = MapLibre-Composable, GeoJSON-CircleLayer, zoom-adaptive Tap-Erkennung
  - `MapSummaryViewModel.kt` = `GetActivityMapPointsUseCase`-Flow, Camera-State-Persistenz
  - `MapSummaryUiState.kt` = Datenhülle für Kartenpunkte und Ladezustand
  - `ActivityMapPointGeoJsonMapper.kt` = `List<ActivityMapPoint>` → GeoJSON-FeatureCollection-String

## Navigation

- Root-Level: `login` und `main`
- Authentifizierter Bereich läuft in `MainShell`
- Primärnavigation: `home`, `activities`, `bike_list` als Konto-Ansicht, `statistics` und `functions`
- Sekundärnavigation: `setup`, `help`, `info`, `api_test`, `logout`
- Zusätzliche Shell-Route `map` (kein Primär-Nav-Eintrag): Weltkarte erreichbar über Button im Aktivitäten-Screen
- Compact: `ModalNavigationDrawer` mit `gesturesEnabled = false` auf der Karten-Route
- größere Breiten: Overflow-Menü in der `TopAppBar`
- Home zeigt in der Shell-Top-Bar den Brand-Titel `M24 Bike Stats`; `M24` ist visuell hervorgehoben
- Support-Screens laufen in der Shell und bleiben damit über Hamburger oder Overview-Navigation erreichbar
- `MAP_ROUTE` ist `internal` in `AppNavigation.kt` und wird von `MainShell.kt` referenziert

## Wichtige Features

- OAuth2 Authorization Code + PKCE
- Token-Speicherung via `EncryptedSharedPreferences`
- cache-first Aktivitäten-, Detail- und Bike-Flows
- mehrstufiger Home-Cloud-Sync mit Bikes, Aktivitäten und konfigurierbarem Detail-Refresh
- optionaler täglicher Hintergrund-Sync über WorkManager mit einstellbarer Netzbedingung
- CSV-Export für Aktivitäten, Details und Tracks
- PDF-Zusammenfassungsbericht mit `PdfDocument`/`Canvas`, cache-only und mit teilbarem FileProvider-Export; enthält Highlights sowie Wochen-, Monats- und Jahresdiagramme
- CSV-Format mit Presets `Automatisch`, `Excel/Deutsch`, `Standard/International`
- Anzeigemodus mit `Automatisch`, `Hell`, `Dunkel`; wird aus `AppSettings` gelesen und direkt im Root-Theme angewendet
- Exporte sind cache-only und haben Cancel-Aktionen
- Track-Screen mit MapLibre/OpenFreeMap, GPX und CSV
- Weltkarte (route `map`) zeigt alle gecachten Touren als Kreise auf einer MapLibre/OpenFreeMap-Karte; Tap auf einen Kreis navigiert zur Aktivitätsdetailseite; Camera-State bleibt beim Zurücknavigieren erhalten
- GPS-Zentrum pro Tour: jene Koordinate aus `/details`, die am weitesten vom Startpunkt entfernt liegt (semantisch: Ziel- oder Wendepunkt); berechnet via `ActivityCenterCalculator`, gespeichert in `ActivityEntity.centerLatitude/Longitude`; fehlende Werte werden durch `ComputeActivityCentersWorker` (WorkManager, einmalig beim App-Start) nachberechnet; `upsertAllPreservingCenter` schützt gecachte Werte bei jedem Cloud-Sync
- Aktivitätsdetailpunkte werden vor Karten-/GPX-Nutzung bereinigt und komprimiert
- API-Test teilt große Ergebnisse als Datei über `FileProvider`
- API-Test kann Ergebnisse zusätzlich nach `Downloads/M24BikeStats` speichern
- Kontodetails zeigen Bosch-`USERINFO`, OIDC-Discovery und OIDC-Signaturzertifikats-Metadaten
- Kontodetails ergänzen Bike-Profil jetzt auch um `oemId`, `serviceDue`, `connectModule`, ABS-Komponenten, Bike Pass, Service Book und Registrierungen; starten mit `Konto & Profil`, zeigen nur das unterstützte System und hängen ausführliche OIDC-Karten unten an
- Statistikscreen mit Vico-Kombidiagramm (Balken Distanz + Linie Fahrtzeit), Wochen-/Monats-/Jahresaggregation, interaktiver Period-Selektion, Summary-Tiles für Gesamt- und Durchschnittswerte pro Tour sowie Durchschnittslinien für Distanz und Fahrtzeit; zeigt zusätzlich den abgedeckten Statistikzeitraum und startet horizontal beim neuesten Abschnitt; darunter `Highlights & Rhythmus` als read-only Sektion für Bestleistungen, distanzstärksten Zeitraum, effektive Reisegeschwindigkeit, Wochentagsverteilung und Wochenfrequenz; Tourenzahl als Data-Label auf dem Balken über Vico `ExtraStore`
- Setup nutzt kompakte Dropdown-Auswahl statt langer Radio-Listen; Änderungen an sichtbaren Optionen deshalb bevorzugt dort zentral halten
- `SupportScreens.kt` enthält jetzt einen dedizierten, gruppierten Info-Screen mit Projekt-/Privacy-/Legal-Karten, kompakten Bibliotheksgruppen und zusammengefassten CLI-Tool-Credits
- aktive EN/DE-Lokalisierung für Navigation, Setup, Home, Funktionen, Statistiken und die sichtbaren Detail-/Track-Flows
- Release-Build läuft mit `isMinifyEnabled = true` und `isShrinkResources = true`
- Android-Backups sind deaktiviert und die App erlaubt keinen Cleartext-Traffic
- `API_TEST` bleibt ein Debug-/Diagnosewerkzeug und wird in Release-Builds nicht in die Endnutzer-Navigation aufgenommen

## Bosch API

Bestätigte Endpunkte:

```text
GET /activity/smart-system/v1/activities?limit=20&offset=0
GET /activity/smart-system/v1/activities/{activityId}/details
GET /bike-profile/smart-system/v1/bikes
GET /bike-profile/smart-system/v1/bikes/{bikeId}
GET /bike-pass/smart-system/v1/bike-passes?bikeId={bikeId}
GET /service-book/smart-system/v1/service-records?bikeId={bikeId}
GET /bike-registration/smart-system/v1/registrations
GET /activity/smart-system/v1/activities/{activityId}/track   -> aktuell 404, nicht produktiv nutzen
GET https://p9.authz.bosch.com/.../userinfo
GET https://p9.authz.bosch.com/.../.well-known/openid-configuration
GET https://p9.authz.bosch.com/.../protocol/openid-connect/certs
```

## Dependency Injection

`AppModule` bindet unter anderem:

- `AuthManager` als `AuthRepository` und `AuthFlowCoordinator`
- `BackgroundSyncScheduler` und `BackgroundSyncSettingsObserver`
- `BoschApiClient` als `BoschApiDataSource`
- `BoschRepositoryImpl` als `BoschApiRepository`
- `BoschSmartSystemRepositoryImpl`
- `LiveOidcUserInfoProvider`
- `LiveOidcDiscoveryInfoProvider`
- `LiveOidcCertificateInfoProvider`
- `AppSettingsRepositoryImpl`
- alle UseCases
- darunter `UpdateDisplayModeUseCase`
- darunter `FetchBoschDataUseCase` (domain/usecase)
- `DashboardStringResolver` für testbare ViewModel-Lokalisierung
- `LoginStringResolver` für testbare Login-Statusmeldungen ohne Android-`Context` im ViewModel
- `PdfReportFileExporter`, `PdfReportMetadataRepository`, `PdfStringResolver`
- `LoginViewModel`, `ApiTestViewModel`, `DashboardViewModel`
- `GetStatisticsUseCase`, `StatisticsUiModelMapper`, `StatisticsViewModel`
- `GetActivityMapPointsUseCase`, `MapSummaryViewModel`
- `ComputeActivityCentersWorker` (WorkManager, einmalig beim App-Start)

## Hinweise für Änderungen

- Für Navigation nur `AppNavigation` und `MainShell` als zentrale Stellen ändern
- Bei Dashboard-UI-Änderungen zuerst prüfen, in welche der Dashboard-Dateien die Änderung fachlich gehört; neue große Blöcke nicht wieder in `DashboardScreen.kt` zurückziehen
- Statistik-Anpassungen bleiben in `presentation/statistics/`; Chart-Extras (Vico `ExtraStore`) nur für Chart-spezifische Zusatzdaten nutzen, read-only Highlights/Rhythmus dagegen direkt im `StatisticsUiState` halten
- Karten-Anpassungen bleiben in `presentation/map/`; `ActivityCenterCalculator` und `GpsPointProjection` gehören zur Datenschicht; keine zusätzliche Cloud-Abfrage nur für die Karte einführen
- PDF-/Report-Datenmodelle dürfen keine `presentation`-States wie `StatisticsUiState` direkt referenzieren; dafür eigene domain-taugliche Aggregate oder Mapper-Grenzen vorsehen
- Hilfe- und Info-Texte für Endnutzer bewusst einfach und verständlich formulieren; technische Details gehören in Doku, Diagnose oder den Info-Bereich nur dann, wenn sie dort wirklich nötig sind
- PDF-Export bleibt cache-only; keine zusätzlichen Bosch-Cloud-Abfragen nur für Berichtserzeugung einführen
- `PdfReportGenerator` ist Android-gebunden und liefert nur die Report-Datei; Aggregation und fachliche Kennzahlen gehören in `ExportPdfSummaryReportUseCase`
- Vico-`dataLabelValueFormatter` bekommt nur den geplotteten Y-Wert; bei potenziell doppelten Distanzwerten keine positionsabhängige Zuordnung per Rohwert annehmen
- Konto-Details bleiben im bestehenden Bike-/`bike_list`-Flow verankert; Route nicht ohne Navigation-Review umbenennen
- Für Exportverhalten immer alle CSV-Pfade mitdenken: Aktivitäten, Detail-CSV, Track-CSV
- Bei Cache-/Sync-Änderungen auf Room-State, Detail-Sync-Modus und Paging-Verhalten achten
- Zusatzdaten pro Bike wie Bike Pass, Service Book und Registrierungen dürfen bei temporären API-Fehlern nicht aus dem Cache verschwinden; leere erfolgreiche Antworten dürfen den Cache dagegen leeren
- Beim Cloud-Sync unterscheiden zwischen Summary-Cache und Detail-Cache; Details dürfen datensparsam nur fehlend oder optional fehlend+veraltet geladen werden
- Änderungen an `AppSettings` immer gegen die relevanten Verbraucher prüfen: Setup-UI, Dashboard-State, Root-Theme und bei Sync-Settings zusätzlich Hintergrund-Scheduler
- Periodischen Hintergrund-Sync nur über `BackgroundSyncScheduler` ändern; keine parallelen WorkManager-Namen oder konkurrierenden Schedules einführen
- Bei Aktivitätsdetails keine rohe Punktliste blind verwenden; Track/Profile laufen über den bereinigten Mapper-Pfad
- Bei Home- oder Drawer-Änderungen `Home`-Navigation und Restore-State explizit prüfen
- Keine Android-`Context`-Abhängigkeit direkt ins ViewModel ziehen, wenn ein kleiner Resolver/Provider reicht
- Bei Lokalisierung nur aktive Nutzertexte anfassen; technische Literale wie MIME-Types, Routen oder JSON-Keys bleiben unberührt
- Sichtbare Compose-Texte bevorzugt in `strings.xml`; nur Brands, URLs, Versionswerte und andere technische Konstanten inline lassen
- Testartefakte mit echten Nutzerdaten wie `bosch-api-test-run-all.txt` nicht mitcommitten
- Änderungen an Security-/Release-Flags immer zusätzlich mit `assembleRelease` prüfen, nicht nur mit Debug-Builds
- `android.disallowKotlinSourceSets=false` aktuell nicht entfernen, solange KSP sonst den Release-Build blockiert
- Wenn Diagnose- oder Entwicklerziele ergänzt werden, standardmäßig `debugOnly` halten und nicht versehentlich in Release-Navigation exponieren

## Testfokus

- UseCases mit Fakes
- Dashboard-ViewModel
- Dashboard-UI-Mapper
- Statistik-Mapper (`StatisticsUiModelMapperTest`): Gruppierung, Timezone-Grenzfälle, Highlights-/Rhythmus-Berechnung
- Statistik-ViewModel (`StatisticsViewModelTest`): Toggle, Grouping-Wechsel, Stale-Reference
- Statistik-UI-Helfer (`StatisticsUiStateFormattingTest`): formatierte Distanz/Stunden, `durationHours`
- Karten-Datenschicht (`ActivityCenterCalculatorTest`): leere Liste, Null-Koordinaten, Einzelpunkt, Kosinus-Korrektur
- Karten-GeoJSON-Mapper (`ActivityMapPointGeoJsonMapperTest`): Koordinatenreihenfolge, leere Liste
- Routing/Navigations-Mapping
- Repository/Cache-Verhalten
- GPX-/CSV-Exportpfade
- API-Test-Sharing

## Offene Baustellen

- alternative Bosch-Endpunkte gezielt nur im API-Test evaluieren
- Logging für produktive Nutzung schlank halten
