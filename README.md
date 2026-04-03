# M24BikeStats

Android-App für Bosch eBike Smart System Fahrtdaten über das Bosch eBike Data Act Portal.

## Überblick

- OAuth2 + PKCE Login gegen Bosch SingleKey ID
- adaptives Compose-UI mit `home`, `activities`, `bike` und `functions`
- sekundäre Navigation für `setup`, `hilfe`, `info`, `api-test` und `logout`
- Home-Top-Bar mit App-Branding statt generischem Bereichstitel
- Room-Cache für Aktivitäten, Aktivitätsdetails und Bikes
- cache-first Listen- und Detailansichten mit gezieltem Hintergrund-Refresh
- Vollsync vom Home-Screen, der alle Aktivitätsseiten und die Bike-Liste neu in Room einliest
- CSV-Export für Aktivitäten, Aktivitätsdetails und Tracks
- CSV-Format mit Presets `Automatisch`, `Excel/Deutsch` und `Standard/International`
- GPX- und Track-Share-Funktionen
- MapLibre/OpenFreeMap-Kartenansicht und Profilcharts für Tracks
- aktive UI-Texte in Englisch und Deutsch lokalisiert

## Voraussetzungen

- Android Studio Meerkat oder neuer
- Android SDK 29+
- Bosch eBike Portal Zugang: [portal.bosch-ebike.com](https://portal.bosch-ebike.com)

## Lokales Setup

1. Repository klonen:
   ```bash
   git clone https://github.com/meuse24/M24BikeStats.git
   ```
2. Projekt in Android Studio öffnen und Gradle Sync ausführen.
3. `local.properties` wird lokal von Android Studio gepflegt.
4. `secrets.properties` wird nicht benötigt, da die App als Public Client arbeitet.

## Build und Checks

```bash
./gradlew test
./gradlew lint
./gradlew build
```

## Navigation

- `Home`: Übersicht, letzter Cloud-Abgleich, letzte Tour, Bike-Status, letzte Exporte
- `Home` zeigt in der Shell-Top-Bar den App-Titel `M24 Bike Stats`, wobei `M24` hervorgehoben ist
- `Aktivitäten`: paginierte Aktivitätenliste mit Suche, Datumsfilter und Sortierung
- `Bike`: Bike-Liste und Bike-Details
- `Funktionen`: CSV-Exporte
- `Setup`: App-Einstellungen wie CSV-Format-Presets
- `Hilfe` / `Info` / `API-Test`: Sekundärziele im Drawer oder Overflow

## Daten und Exporte

- Aktivitäten werden über `limit`/`offset` paginiert geladen.
- Aktivitätsdetails kommen über `/activity/smart-system/v1/activities/{activityId}/details`.
- Bikes kommen über `/bike-profile/smart-system/v1/bikes` und `/bikes/{bikeId}`.
- CSV-Exporte nutzen den persistenten Setup-Wert für das Exportformat.
- `Automatisch` leitet aus den Dezimalkonventionen des Geräts ein passendes CSV-Preset ab.
- `Excel/Deutsch` nutzt Semikolon, Dezimalkomma und deutsches Datumsformat.
- `Standard/International` nutzt Komma, Dezimalpunkt und ISO-nahes Datumsformat.
- Der Home-Sync lädt die vollständige Aktivitätenliste seitenweise wie der CSV-Export, aber ohne Datei-Erzeugung.

## Architektur

```text
domain/        Interfaces, Modelle, UseCases
data/          API- und Repository-Implementierungen, Room-Zugriff
auth/          OAuth2/AppAuth und Token-Verwaltung
presentation/  Compose-Screens, Navigation, ViewModels
di/            Koin-Modul
```

Ergänzungen:

- `presentation/navigation`: Root- und Shell-Navigation, adaptive Top-Bar/Drawer-Logik
- `presentation/dashboard`: Home, Aktivitäten, Bike, Funktionen sowie Detail- und Track-Screens
- `presentation/dashboard/DashboardStringResolver`: UI-Strings für ViewModels testbar auflösbar ohne Android-`Context` direkt im ViewModel

Mehr Projektdetails: [CLAUDE.md](CLAUDE.md)

## OAuth2-Konfiguration

| Feld | Wert |
|---|---|
| Client ID | `euda-2c8d2760-d459-40aa-adc9-6eb7a8b91bd7` |
| Redirect URI | `m24bikestats://oauth-callback` |
| Flow | Authorization Code + PKCE |

## Verifizierte Endpunkte

Stand: 2. April 2026, live mit echtem Smart-System-Token getestet.

| Endpoint | Status | Zweck |
|---|---|---|
| `GET /activity/smart-system/v1/activities?limit=20&offset=0` | `200` | Aktivitätenliste |
| `GET /activity/smart-system/v1/activities/{activityId}/details` | `200` | Aktivitätsdetails |
| `GET /bike-profile/smart-system/v1/bikes` | `200` | Bike-Liste |
| `GET /bike-profile/smart-system/v1/bikes/{bikeId}` | `200` | Bike-Detail |
| `GET /activity/smart-system/v1/activities/{activityId}/track` | `404` | aktuell nicht bestätigt |
| `GET .../userinfo` | `200` | OIDC Userinfo |
| `GET .../.well-known/openid-configuration` | `200` | OIDC Discovery |

## Testabdeckung

- Unit-Tests für Mapper, UseCases und ViewModels
- Navigation- und Routing-Tests
- Repository- und Cache-Tests
- Room- und Migrations-Tests auf Android
- GPX-/CSV-Exporttests

## Lizenz

MIT
