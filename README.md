# M24BikeStats

Android-App zum Abrufen und Anzeigen von Fahrtdaten aus dem **Bosch eBike Data Act Portal**.

## Features

- OAuth2 + PKCE Authentifizierung gegen das Bosch eBike Portal (kein Client Secret erforderlich)
- Sicherer Token-Speicher via Android Keystore (`EncryptedSharedPreferences`)
- Automatischer Access-Token-Refresh über den Bosch-OIDC-Token-Endpunkt
- API-Test-Screen: Einzelaufrufe und Batch-Test aller bekannten Endpunkte mit Roh-JSON

## Voraussetzungen

- Android Studio Meerkat oder neuer
- Android SDK 29+
- Bosch eBike Portal Zugang: [portal.bosch-ebike.com](https://portal.bosch-ebike.com)
- Registrierte Client-Anwendung im Bosch Portal (Client ID vorhanden)

## Setup

1. Repo klonen:
   ```bash
   git clone https://github.com/meuse24/M24BikeStats.git
   ```

2. In Android Studio öffnen und Gradle sync durchführen.

3. Keine `secrets.properties` nötig – die App nutzt einen öffentlichen Client ohne Secret.

## Build

```bash
# Debug APK
./gradlew assembleDebug

# Tests
./gradlew test
```

## Architektur

Clean Architecture mit Jetpack Compose:

```
domain/        ← Interfaces, UseCase, Modelle (reines Kotlin)
data/          ← API-Client (OkHttp), Repository-Implementierungen
auth/          ← OAuth2-Flow via AppAuth, Token-Verwaltung
presentation/  ← Stateless Compose-Screens, ViewModels (StateFlow)
di/            ← Koin Dependency Injection
```

Detaillierte Beschreibung: [CLAUDE.md](CLAUDE.md)

## OAuth2-Konfiguration

| Feld        | Wert |
|-------------|------|
| Client ID   | `euda-2c8d2760-d459-40aa-adc9-6eb7a8b91bd7` |
| Redirect URI | `m24bikestats://oauth-callback` |
| Flow        | Authorization Code + PKCE (RFC 7636) |
| Endpunkte   | `p9.authz.bosch.com` (Keycloak, Realm `obc`) |

## Tech Stack

| Bibliothek | Zweck |
|------------|-------|
| Jetpack Compose | UI |
| Navigation Compose | Screen-Navigation |
| AppAuth Android | OAuth2/PKCE |
| Koin | Dependency Injection |
| OkHttp | HTTP-Client |
| EncryptedSharedPreferences | Sichere Token-Speicherung |

## Verifizierte Endpunkte

Stand: 2. April 2026, live mit echtem Smart-System-Token getestet.

### Bosch Smart System

| Endpoint | Status | Zweck |
|----------|--------|-------|
| `GET /activity/smart-system/v1/activities?limit=20&offset=0` | `200` | Aktivitätenliste mit Pagination und Summary-Feldern |
| `GET /bike-profile/smart-system/v1/bikes` | `200` | Liste der Bikes |
| `GET /bike-profile/smart-system/v1/bikes/{bikeId}` | `200` | Detailansicht eines Bikes |
| `GET /activity/smart-system/v1/activities/{activityId}` | `404` | Mit echter ID aktuell nicht bestätigt |
| `GET /activity/smart-system/v1/activities/{activityId}/track` | `404` | Mit echter ID aktuell nicht bestätigt |

### OIDC

| Endpoint | Status | Zweck |
|----------|--------|-------|
| `GET https://p9.authz.bosch.com/auth/realms/obc/protocol/openid-connect/userinfo` | `200` | Nutzerprofil zum Token |
| `GET https://p9.authz.bosch.com/auth/realms/obc/.well-known/openid-configuration` | `200` | Discovery-Dokument mit OIDC-/OAuth-Endpunkten |

## Bestätigte Datenstrukturen

### Aktivitätenliste

Die Antwort auf `activities` enthält:
- `pagination.total`, `pagination.offset`, `pagination.limit`
- `activitySummaries[]`
- pro Aktivität u. a. `id`, `startTime`, `endTime`, `timeZone`, `durationWithoutStops`, `title`, `bikeId`, `startOdometer`, `distance`, `speed`, `cadence`, `riderPower`, `elevation`, `caloriesBurned`

### Bike-Liste und Bike-Detail

Die Antwort auf `bikes` bzw. `bikes/{bikeId}` enthält:
- `id`, `createdAt`, `language`
- `driveUnit` mit z. B. `serialNumber`, `partNumber`, `productName`, `odometer`, `maximumAssistanceSpeed`, `activeAssistModes`, `powerOnTime`
- `remoteControl`
- `batteries[]` mit z. B. `deliveredWhOverLifetime`, `chargeCycles`
- `headUnit`
- `serviceDue`

## Lizenz

MIT
