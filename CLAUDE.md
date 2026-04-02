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
  model/         ← BoschEndpoint (Enum)
  repository/    ← AuthRepository, BoschRepository (Interfaces)
  usecase/       ← FetchBoschDataUseCase

data/            ← implementiert Domain-Interfaces
  remote/        ← BoschApiClient (OkHttp, class)
  repository/    ← BoschRepositoryImpl

auth/            ← OAuth2-Brücke (plattformspezifisch)
  AuthManager    ← implementiert LoginRepository (extends AuthRepository)
  LoginRepository← Interface mit Android-Intent-Methoden
  OAuthConfig    ← Endpunkte, Client-ID, Scopes

presentation/    ← stateless Screens, ViewModels, Navigation
  login/         ← LoginViewModel, LoginScreen, LoginStatus
  apitest/       ← ApiTestViewModel (StateFlow), ApiTestScreen, ApiTestUiState
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
5. Navigation → `ApiTestScreen`

### Compose-Muster

- Screens sind **stateless**: nehmen nur `UiState`-Datenklassen + Callbacks
- `AppNavigation` ist die einzige Stelle, die ViewModels kennt
- `StateFlow` + `collectAsStateWithLifecycle()` für reaktive UI-Updates
- `LaunchedEffect` für Navigations-Seiteneffekte

### Credentials

```
# secrets.properties (nicht im Repo, in .gitignore)
BOSCH_CLIENT_ID=euda-2c8d2760-d459-40aa-adc9-6eb7a8b91bd7
BOSCH_CLIENT_SECRET=  ← nicht benötigt (public client)
```

Client-ID ist in `OAuthConfig.kt` hart kodiert (kein Geheimnis bei public clients).

### Offene Punkte

- Genaue Scope-Werte aus `https://portal.bosch-ebike.com/data-act/app#/introduction` ermitteln
- Bosch API-Endpunkte in `BoschEndpoint.kt` nach erstem erfolgreichen Login validieren
- Token-Refresh-Logik implementieren (Refresh Token ist in `AuthManager` gespeichert)
