# M24BikeStats

Android-App zum Abrufen und Anzeigen von Fahrtdaten aus dem **Bosch eBike Data Act Portal**.

## Features

- OAuth2 + PKCE Authentifizierung gegen das Bosch eBike Portal (kein Client Secret erforderlich)
- Sicherer Token-Speicher via Android Keystore (`EncryptedSharedPreferences`)
- Automatischer Access-Token-Refresh über den Bosch-OIDC-Token-Endpunkt
- Best-Effort-Logout über den Bosch-OIDC-End-Session-Endpunkt
- Fachliches Dashboard mit Aktivitätenübersicht, Aktivitätsdetail und Bike-Ansicht
- Lokaler Room-Cache für Aktivitäten, Aktivitätsdetails und Bikes
- Explizite Room-Migrationen für die bekannten Cache-Schema-Versionen ab v2
- Cache-first Detail- und Bike-Screens mit Hintergrund-Refresh aus Room
- Cache-first Listen-, Detail- und Bike-Flows mit `observe + refreshIfStale`
- Aktivitäten-Paginierung auf Basis von `limit`/`offset`
- Funktionen-Tab mit CSV-Export aller Aktivitäten
- Funktionen-Tab mit CSV-Export aller Aktivitäten und sichtbarer Aktivitätsdetails
- Aktivitätenliste mit Datumsfilter und Sortierung
- Aktivitätenliste mit zusätzlicher Textsuche
- Trackansicht mit vollständigem Verlauf auf Basis der bestätigten GPS-Punkte
- Interaktive Kartenkachel-Ansicht mit MapLibre und OpenFreeMap
- Fullscreen-Track-Screen mit Kartenansicht, Share-, GPX-, CSV- und Autofit-Aktionen
- Höhen-, Leistungs- und Geschwindigkeitsprofil auf Basis der Bosch-Detailpunkte
- Direkter GPX-Export über das Android-Share-Sheet
- Track-CSV-Export direkt im fullscreen Track-Screen
- Erweiterte Testabdeckung für Mapper, Repository, Room-Migrationen, ViewModel und Exporte

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

Die Login-Seite verwendet einen kurzen Hinweis auf Bosch SingleKey ID.
Die Anmeldung erfolgt in `M24 Bike Stats` mit demselben Bosch-Login-Prinzip wie bei der eBike Flow App.

## Tech Stack

| Bibliothek | Zweck |
|------------|-------|
| Jetpack Compose | UI |
| Navigation Compose | Screen-Navigation |
| AppAuth Android | OAuth2/PKCE |
| Koin | Dependency Injection |
| OkHttp | HTTP-Client |
| EncryptedSharedPreferences | Sichere Token-Speicherung |
| Room | Lokaler Cache inkl. Migrationen |
| MapLibre + OpenFreeMap | Interaktive Kartenkacheln |

## Verifizierte Endpunkte

Stand: 2. April 2026, live mit echtem Smart-System-Token getestet.

### Bosch Smart System

| Endpoint | Status | Zweck |
|----------|--------|-------|
| `GET /activity/smart-system/v1/activities?limit=20&offset=0` | `200` | Aktivitätenliste mit Pagination und Summary-Feldern |
| `GET /bike-profile/smart-system/v1/bikes` | `200` | Liste der Bikes |
| `GET /activity/smart-system/v1/activities/{activityId}/details` | `200` | Detailpunkte einer Aktivität inkl. GPS-/Metrikdaten |
| `GET /bike-profile/smart-system/v1/bikes/{bikeId}` | `200` | Detailansicht eines Bikes |
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

Die App nutzt diese Werte inzwischen nicht nur im API-Test-Screen, sondern auch für ein fachliches Dashboard:
- paginierte Aktivitätenübersicht
- Aktivitätsdetailseite auf Basis der Summary-Daten
- `Mehr Aktivitäten laden` über `limit`/`offset`
- CSV-Export aller Aktivitäten über den Funktionen-Tab
- Detail-CSV-Export für den aktuell sichtbaren Aktivitätssatz über den Funktionen-Tab
- Datumsfilter für `Alle`, `30 Tage` und `12 Monate`
- Sortierung nach neuesten/ältesten Touren sowie Distanz und Dauer
- Textsuche über Titel, Datum und Distanzlabel
- persistente Zwischenspeicherung der geladenen Aktivitäten in Room
- automatische Hintergrund-Aktualisierung nur bei veraltetem Cache
- ViewModel- und Präsentationslogik für Filter, Sortierung und Suche sind separat testbar

### Aktivitätsdetails

Die Antwort auf `activities/{activityId}/details` enthält:
- `activityDetails[]`
- pro Detailpunkt u. a. `distance`, `altitude`, `speed`, `cadence`, `latitude`, `longitude`, `riderPower`

Die App nutzt diese Detaildaten jetzt für die Aktivitätsdetailseite:
- echte Detailpunkte statt nur Summary-Daten
- GPS-Punktanzahl und Start-/Zielkoordinaten
- zusätzliche Kennzahlen aus dem Detail-Track
- vollständige Trackansicht als interne Karten-/Polyline-Darstellung
- interaktive Kartenkacheln mit live gezeichneter Tracklinie
- automatische Ausrichtung auf die Track-Bounds sowie getrennte Start-/Zielmarker
- Linienprofile für Höhe, Fahrerleistung und Geschwindigkeit entlang der Strecke
- GPX-Datei direkt über das Android-Share-Sheet sowie GPX-Kopierfunktion
- CSV-Datei mit allen Detailpunkten inklusive GPS-, Distanz-, Höhen-, Geschwindigkeits-, Kadenz- und Leistungswerten
- Detail-CSV auch gesammelt für mehrere sichtbare Aktivitäten über den Funktionen-Tab
- direkter CSV-Export des aktuell geöffneten Tracks im fullscreen Track-Screen
- persistente Zwischenspeicherung der Detaildaten und Trackpunkte in Room
- Detailscreen zeigt lokale Daten sofort und aktualisiert sie bei Bedarf im Hintergrund

### Bike-Liste und Bike-Detail

Die Antwort auf `bikes` bzw. `bikes/{bikeId}` enthält:
- `id`, `createdAt`, `language`
- `driveUnit` mit z. B. `serialNumber`, `partNumber`, `productName`, `odometer`, `maximumAssistanceSpeed`, `activeAssistModes`, `powerOnTime`
- `remoteControl`
- `batteries[]` mit z. B. `deliveredWhOverLifetime`, `chargeCycles`
- `headUnit`
- `serviceDue`

Die App cached Bike-Liste und Bike-Details ebenfalls in Room, sodass der Bike-Tab lokale Daten sofort anzeigen und danach im Hintergrund aktualisieren kann.

## Teststand

Die aktuelle Testabdeckung umfasst:
- Unit-Tests für Mapper und Präsentationslogik
- UseCase-Tests mit Fakes
- ViewModel-Tests für Dashboard-Filter, Suche und Exporte
- Room- und Migrationstests auf Android
- Repository-Integrationstests für `remote -> db -> cache-flow`
- Android-Tests für GPX-/CSV-Exportpfade

## Lizenz

MIT
