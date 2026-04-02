# M24BikeStats

Android-App zum Abrufen und Anzeigen von Fahrtdaten aus dem **Bosch eBike Data Act Portal**.

## Features

- OAuth2 + PKCE Authentifizierung gegen das Bosch eBike Portal (kein Client Secret erforderlich)
- Sicherer Token-Speicher via Android Keystore (`EncryptedSharedPreferences`)
- API-Test-Screen: Dropdown mit verfügbaren Endpunkten, rohe JSON-Ausgabe

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

## Lizenz

MIT
