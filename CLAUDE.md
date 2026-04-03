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
- Vollsync vom Home-Screen für Room gegen Bosch-Cloud
- CSV-Export für Aktivitäten, Details und Tracks
- CSV-Trennzeichen persistent konfigurierbar
- Track-Screen mit MapLibre/OpenFreeMap, GPX und CSV
- aktive EN/DE-Lokalisierung für Navigation, Setup, Home, Funktionen und die sichtbaren Detail-/Track-Flows

## Bosch API

Bestätigte Endpunkte:

```text
GET /activity/smart-system/v1/activities?limit=20&offset=0
GET /activity/smart-system/v1/activities/{activityId}/details
GET /bike-profile/smart-system/v1/bikes
GET /bike-profile/smart-system/v1/bikes/{bikeId}
GET /activity/smart-system/v1/activities/{activityId}/track   -> aktuell 404
GET https://p9.authz.bosch.com/.../userinfo
GET https://p9.authz.bosch.com/.../.well-known/openid-configuration
```

## Dependency Injection

`AppModule` bindet unter anderem:

- `AuthManager` als `AuthRepository` und `LoginRepository`
- `BoschApiClient` als `BoschApiDataSource`
- `BoschRepositoryImpl`
- `BoschSmartSystemRepositoryImpl`
- `AppSettingsRepositoryImpl`
- alle UseCases
- `DashboardStringResolver` für testbare ViewModel-Lokalisierung
- `LoginViewModel`, `ApiTestViewModel`, `DashboardViewModel`

## Hinweise für Änderungen

- Für Navigation nur `AppNavigation` und `MainShell` als zentrale Stellen ändern
- Für Exportverhalten immer alle CSV-Pfade mitdenken: Aktivitäten, Detail-CSV, Track-CSV
- Bei Cache-/Sync-Änderungen auf Room-State und Paging-Verhalten achten
- Bei Home- oder Drawer-Änderungen `Home`-Navigation und Restore-State explizit prüfen
- Keine Android-`Context`-Abhängigkeit direkt ins ViewModel ziehen, wenn ein kleiner Resolver/Provider reicht
- Bei Lokalisierung nur aktive Nutzertexte anfassen; technische Literale wie MIME-Types, Routen oder JSON-Keys bleiben unberührt

## Testfokus

- UseCases mit Fakes
- Dashboard-ViewModel
- Routing/Navigations-Mapping
- Repository/Cache-Verhalten
- GPX-/CSV-Exportpfade

## Offene Baustellen

- Bosch `track`-Endpoint weiter verifizieren
- Logging für produktive Nutzung schlank halten
