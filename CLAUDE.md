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
```

Optional:

```bash
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew connectedAndroidTest
```

## Architektur

```text
domain/
  model/       Bosch- und App-Modelle
  repository/  Interfaces
  usecase/     fachliche Anwendungsfälle

data/
  remote/      Bosch API Zugriff
  local/       Room, Mapper, Cache-State
  repository/  Repository-Implementierungen

auth/
  AuthManager
  LoginRepository
  OAuthConfig

presentation/
  login/
  apitest/
  dashboard/
  navigation/

di/
  AppModule
```

Regeln:

- `domain` bleibt Android-frei
- `data` hängt nur an `domain`
- `presentation` hängt an `domain` und `auth`
- ViewModels exponieren `StateFlow`
- Screens sind stateless und bekommen `UiState` plus Callbacks
- Dashboard-UI ist nach Verantwortungen getrennt:
  - `DashboardScreen.kt` = Shell
  - `DashboardOverviewComponents.kt` = Listen/Karten/Filter
  - `DashboardDetailScreens.kt` = Activity-/Bike-Details
  - `DashboardTrackScreen.kt` = Track-Vollbild + Export
  - `DashboardSharedUi.kt` = gemeinsame Compose-Bausteine

## Navigation

- Root-Level: `login` und `main`
- Authentifizierter Bereich läuft in `MainShell`
- Primärnavigation: `home`, `activities`, `bike_list`, `functions`
- Sekundärnavigation: `setup`, `help`, `info`, `api_test`, `logout`
- Compact: `ModalNavigationDrawer`
- größere Breiten: Overflow-Menü in der `TopAppBar`
- Home zeigt in der Shell-Top-Bar den Brand-Titel `M24 Bike Stats`; `M24` ist visuell hervorgehoben
- Support-Screens laufen in der Shell und bleiben damit über Hamburger oder Overview-Navigation erreichbar

## Wichtige Features

- OAuth2 Authorization Code + PKCE
- Token-Speicherung via `EncryptedSharedPreferences`
- cache-first Aktivitäten-, Detail- und Bike-Flows
- mehrstufiger Home-Cloud-Sync mit Bikes, Aktivitäten und konfigurierbarem Detail-Refresh
- optionaler täglicher Hintergrund-Sync über WorkManager mit einstellbarer Netzbedingung
- CSV-Export für Aktivitäten, Details und Tracks
- CSV-Format mit Presets `Automatisch`, `Excel/Deutsch`, `Standard/International`
- Exporte sind cache-only und haben Cancel-Aktionen
- Track-Screen mit MapLibre/OpenFreeMap, GPX und CSV
- Aktivitätsdetailpunkte werden vor Karten-/GPX-Nutzung bereinigt und komprimiert
- API-Test teilt große Ergebnisse als Datei über `FileProvider`
- aktive EN/DE-Lokalisierung für Navigation, Setup, Home, Funktionen und die sichtbaren Detail-/Track-Flows

## Bosch API

Bestätigte Endpunkte:

```text
GET /activity/smart-system/v1/activities?limit=20&offset=0
GET /activity/smart-system/v1/activities/{activityId}/details
GET /bike-profile/smart-system/v1/bikes
GET /bike-profile/smart-system/v1/bikes/{bikeId}
GET /activity/smart-system/v1/activities/{activityId}/track   -> aktuell 404, nicht produktiv nutzen
GET https://p9.authz.bosch.com/.../userinfo
GET https://p9.authz.bosch.com/.../.well-known/openid-configuration
```

## Dependency Injection

`AppModule` bindet unter anderem:

- `AuthManager` als `AuthRepository` und `LoginRepository`
- `BackgroundSyncScheduler` und `BackgroundSyncSettingsObserver`
- `BoschApiClient` als `BoschApiDataSource`
- `BoschRepositoryImpl`
- `BoschSmartSystemRepositoryImpl`
- `AppSettingsRepositoryImpl`
- alle UseCases
- `DashboardStringResolver` für testbare ViewModel-Lokalisierung
- `LoginViewModel`, `ApiTestViewModel`, `DashboardViewModel`

## Hinweise für Änderungen

- Für Navigation nur `AppNavigation` und `MainShell` als zentrale Stellen ändern
- Bei Dashboard-UI-Änderungen zuerst prüfen, in welche der Dashboard-Dateien die Änderung fachlich gehört; neue große Blöcke nicht wieder in `DashboardScreen.kt` zurückziehen
- Für Exportverhalten immer alle CSV-Pfade mitdenken: Aktivitäten, Detail-CSV, Track-CSV
- Bei Cache-/Sync-Änderungen auf Room-State, Detail-Sync-Modus und Paging-Verhalten achten
- Beim Cloud-Sync unterscheiden zwischen Summary-Cache und Detail-Cache; Details dürfen datensparsam nur fehlend oder optional fehlend+veraltet geladen werden
- Änderungen an `AppSettings` immer gegen drei Pfade prüfen: Setup-UI, Hintergrund-Scheduler und Dashboard-State
- Periodischen Hintergrund-Sync nur über `BackgroundSyncScheduler` ändern; keine parallelen WorkManager-Namen oder konkurrierenden Schedules einführen
- Bei Aktivitätsdetails keine rohe Punktliste blind verwenden; Track/Profile laufen über den bereinigten Mapper-Pfad
- Bei Home- oder Drawer-Änderungen `Home`-Navigation und Restore-State explizit prüfen
- Keine Android-`Context`-Abhängigkeit direkt ins ViewModel ziehen, wenn ein kleiner Resolver/Provider reicht
- Bei Lokalisierung nur aktive Nutzertexte anfassen; technische Literale wie MIME-Types, Routen oder JSON-Keys bleiben unberührt
- Testartefakte mit echten Nutzerdaten wie `bosch-api-test-run-all.txt` nicht mitcommitten

## Testfokus

- UseCases mit Fakes
- Dashboard-ViewModel
- Dashboard-UI-Mapper
- Routing/Navigations-Mapping
- Repository/Cache-Verhalten
- GPX-/CSV-Exportpfade
- API-Test-Sharing

## Offene Baustellen

- alternative Bosch-Endpunkte gezielt nur im API-Test evaluieren
- Logging für produktive Nutzung schlank halten
