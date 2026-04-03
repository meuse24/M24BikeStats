# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
# Debug-Build
./gradlew assembleDebug

# Release-Build
./gradlew assembleRelease

# Unit-Tests
./gradlew test

# Einzelnen Test ausführen
./gradlew :app:test --tests "info.meuse24.m24bikestats.ExampleUnitTest"

# Instrumented Tests (Gerät/Emulator erforderlich)
./gradlew connectedAndroidTest

# Lint
./gradlew lint
```

## Architektur

Single-Module Android-App mit **Clean Architecture** und **Jetpack Compose**.

**Package:** `info.meuse24.m24bikestats` | **minSdk:** 29 | **targetSdk:** 36 | **Kotlin:** 2.2.10

### Schichtenmodell

```
domain/          ← reines Kotlin, kein Android-Framework
  model/         ← BoschEndpoint (Enum mit baseUrl + path)
  repository/    ← AuthRepository, BoschRepository (Interfaces)
  usecase/       ← FetchBoschDataUseCase, Smart-System-UseCases

data/            ← implementiert Domain-Interfaces
  remote/        ← BoschApiClient (OkHttp, JWT-Decoder)
  repository/    ← BoschRepositoryImpl, BoschSmartSystemRepositoryImpl

auth/            ← OAuth2-Brücke (plattformspezifisch)
  AuthManager    ← implementiert LoginRepository (extends AuthRepository)
  LoginRepository← Interface mit Android-Intent-Methoden
  OAuthConfig    ← Endpunkte, Client-ID, Scopes

presentation/    ← stateless Screens, ViewModels, Navigation
  login/         ← LoginViewModel, LoginScreen, LoginStatus
  apitest/       ← ApiTestViewModel (StateFlow), ApiTestScreen, ApiTestUiState
  dashboard/     ← DashboardViewModel, DashboardScreen, Detail-Screens
  navigation/    ← AppNavigation (verbindet VMs mit Screens)

di/              ← AppModule (Koin)
M24BikeStatsApp  ← Application, startet Koin
```

### Abhängigkeitsregeln

```
presentation → domain, auth
data         → domain
auth         → domain (AuthRepository), Android SDK
domain       → (nichts)
```

### Dependency Injection

Koin (`AppModule`):
- `AuthManager` als Singleton; über `AuthRepository` und `LoginRepository` gebunden
- `BoschRepositoryImpl` als Singleton (über `BoschRepository`)
- `FetchBoschDataUseCase` als Factory
- ViewModels via `viewModelOf`

### Auth-Flow (OAuth2 + PKCE)

Öffentlicher Client – kein Client Secret. AppAuth generiert Code-Verifier automatisch.

1. `LoginViewModel.buildAuthIntent()` → `AppNavigation` → `LoginScreen` launcht Browser
2. Bosch-Login → Redirect auf `m24bikestats://oauth-callback`
3. `RedirectUriReceiverActivity` fängt Callback ab, liefert Result zurück
4. `LoginViewModel.handleAuthResult()` → Token-Austausch → Token in `EncryptedSharedPreferences`
5. Navigation → `dashboard`
6. Logout versucht zusätzlich einen OIDC-End-Session-Flow gegen Bosch, bevor die App lokal auf `login` zurückkehrt

### Compose-Muster

- Screens sind **stateless**: nehmen nur `UiState`-Datenklassen + Callbacks
- `AppNavigation` ist die einzige Stelle, die ViewModels kennt
- `StateFlow` + `collectAsStateWithLifecycle()` für reaktive UI-Updates
- `LaunchedEffect` für Navigations-Seiteneffekte

### Credentials

Client-ID ist in `OAuthConfig.kt` hart kodiert (kein Geheimnis bei public clients).
`secrets.properties` wird nicht benötigt.

---

## Bosch eBike API – bestätigte Erkenntnisse

### Token-Eigenschaften (aus JWT-Analyse)

| Claim | Wert | Bedeutung |
|---|---|---|
| `aud` | `api-bosch-ebike` | Token gilt für `api.bosch-ebike.com` |
| `scope` | `euda:read ...` | EU Data Act Lesezugriff – automatisch erteilt |
| `ebike-rider-id` | UUID | Rider-ID für Detailabfragen |
| Lebensdauer | 3600 s | Refresh Token vorhanden |

### API-Endpunkte (Quelle: open-ebike/open-ebike-backend)

**Base URL:** `https://api.bosch-ebike.com`

```
# Smart System / BES3 (Flow-App, live bestätigt am 2026-04-02)
GET /activity/smart-system/v1/activities?limit=20&offset=0     -> HTTP 200
GET /bike-profile/smart-system/v1/bikes                        -> HTTP 200
GET /activity/smart-system/v1/activities/{activityId}/details  -> HTTP 200
GET /bike-profile/smart-system/v1/bikes/{bikeId}               -> HTTP 200
GET /activity/smart-system/v1/activities/{activityId}/track    -> HTTP 404

# OIDC – live bestätigt HTTP 200
GET https://p9.authz.bosch.com/.../userinfo
GET https://p9.authz.bosch.com/.../.well-known/openid-configuration
```

`BoschEndpoint.kt` enthält alle Varianten inkl. TOKEN_INFO (lokaler JWT-Decoder) und OIDC_DISCOVERY.

### Verifizierte Datenformen

- `activities` liefert `pagination` und `activitySummaries[]` mit Fahrdaten, Leistungswerten und `bikeId`
- `activities/{activityId}/details` liefert `activityDetails[]` mit Distanz-, Höhen-, GPS-, Speed-, Kadenz- und Rider-Power-Punkten
- `bikes` und `bikes/{bikeId}` liefern Komponenten-, Batterie- und Drive-Unit-Informationen
- JWT-Claims enthalten `aud=api-bosch-ebike`, `scope=euda:read`, `bosch-id`, `ebike-rider-id`

### Aktueller UI-Stand

- Startziel nach Login ist `dashboard`
- Dashboard zeigt Aktivitäten, Bike und Funktionen in einer gemeinsamen Tab-Navigation
- Aktivitäten werden paginiert über `limit`/`offset` geladen
- Aktivitätenliste kommt cache-first aus Room und wird danach remote synchronisiert
- Aktivitätsdetails verwenden jetzt den bestätigten `/details`-Endpunkt
- Aktivitätsdetails und Trackpunkte werden in Room gecacht und cache-first geladen
- Aktivitätsdetails können den vollständigen Track als GPX teilen
- Es gibt einen eigenen Track-Screen mit Polyline aus allen bestätigten GPS-Punkten
- Der Track-Screen enthält zusätzlich eine MapLibre/OpenFreeMap-Kartenansicht mit Track-Overlay
- Der Track-Screen ist eine fullscreen Kartenansicht mit Top-Bar und Action-Bottom-Bar
- Der Track-Screen zeigt zusätzlich Linienprofile für Höhe, Fahrerleistung und Geschwindigkeit
- Bike-Liste und Bike-Details kommen cache-first aus Room
- Bike-Details kommen über `GET /bike-profile/smart-system/v1/bikes/{bikeId}`
- Der Funktionen-Tab enthält den CSV-Export aller Aktivitäten und nutzt zuerst den lokalen Aktivitäten-Cache
- Login-Hinweis erklärt Bosch SingleKey ID kurz aus Sicht der eigenen App

---

## Offene Punkte

- Alternative Pfade für Activity-Detail und Activity-Track recherchieren
- Log-Ausgaben für produktive Nutzung datensparsam machen
