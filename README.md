# M24BikeStats

Android-App für Bosch eBike Smart System Fahrtdaten über das Bosch eBike Data Act Portal.

## Überblick

- OAuth2 + PKCE Login gegen Bosch SingleKey ID
- adaptives Compose-UI mit `home`, `activities`, `bike` und `functions`
- sekundäre Navigation für `setup`, `hilfe`, `info`, `api-test` und `logout`
- Home-Top-Bar mit App-Branding statt generischem Bereichstitel
- Room-Cache für Aktivitäten, Aktivitätsdetails und Bikes
- cache-first Listen- und Detailansichten mit gezieltem Hintergrund-Refresh
- mehrstufiger Cloud-Sync vom Home-Screen für Bikes, Aktivitäten und optional fehlende bzw. veraltete Aktivitätsdetails
- optionaler täglicher Hintergrund-Sync über WorkManager mit konfigurierbaren Netzwerkbedingungen
- CSV-Export für Aktivitäten, Aktivitätsdetails und Tracks
- CSV-Format mit Presets `Automatisch`, `Excel/Deutsch` und `Standard/International`
- cache-only Exporte, damit keine zusätzlichen Cloud-Abfragen während des Exports nötig sind
- GPX- und Track-Share-Funktionen
- robuster API-Test-Share als Datei statt großer Binder-Texttransaktion
- MapLibre/OpenFreeMap-Kartenansicht und Profilcharts für Tracks
- Bereinigung und Kompression redundanter Detailpunkte für Karte, GPX und Profile
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
- `Setup`: zusätzlich Detail-Sync-Modus `nur fehlende` oder `fehlende + veraltete`
- `Setup`: zusätzlich Hintergrund-Sync `deaktiviert`, `täglich per WLAN` oder `täglich in jedem Netz`
- `Hilfe` / `Info` / `API-Test`: Sekundärziele im Drawer oder Overflow

## Daten und Exporte

- Aktivitäten werden über `limit`/`offset` paginiert geladen.
- Aktivitätsdetails kommen über `/activity/smart-system/v1/activities/{activityId}/details`.
- Bikes kommen über `/bike-profile/smart-system/v1/bikes` und `/bikes/{bikeId}`.
- Der separate `/track`-Endpunkt liefert aktuell `404`; Track, GPX und Profile basieren deshalb auf `/details`.
- Detailpunkte mit `0/0`-Koordinaten oder redundanten aufeinanderfolgenden Duplikaten werden vor Karten-/GPX-Nutzung bereinigt.
- CSV-Exporte nutzen den persistenten Setup-Wert für das Exportformat.
- `Automatisch` leitet aus den Dezimalkonventionen des Geräts ein passendes CSV-Preset ab.
- `Excel/Deutsch` nutzt Semikolon, Dezimalkomma und deutsches Datumsformat.
- `Standard/International` nutzt Komma, Dezimalpunkt und ISO-nahes Datumsformat.
- Der optionale Hintergrund-Sync plant genau einen eindeutigen periodischen WorkManager-Job und übernimmt dabei den im Setup gewählten Detail-Sync-Modus.
- Aktivitäten- und Detail-CSV exportieren nur Daten, die bereits in Room vorhanden sind.
- Der Home-Sync zeigt Fortschritt und kann abgebrochen werden.
- Der Home-Sync kann datensparsam nur fehlende Aktivitätsdetails laden oder optional veraltete Details mitaktualisieren.
- Die Home-Übersicht zeigt zusätzlich die Anzahl gecachter Detaildatensätze und GPS-Punkte.
- Bike-Status nutzt zusätzlich Walk Assist, Einschaltzeit und Assist-Reichweiten aus den Bike-Details.

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
- `presentation/dashboard/DashboardScreen.kt`: nur noch Dashboard-Shell mit Tabs, Snackbar und Screen-Auswahl
- `presentation/dashboard/DashboardOverviewComponents.kt`: Karten-, Listen- und Filter-Komponenten für Aktivitäten und Bikes
- `presentation/dashboard/DashboardDetailScreens.kt`: Aktivitäts- und Bike-Detailscreens inkl. Share-/Detail-Sektionen
- `presentation/dashboard/DashboardTrackScreen.kt`: Track-Vollbild, Karten-/Canvas-Helfer und Exportdialog
- `presentation/dashboard/DashboardSharedUi.kt`: wiederverwendete Hero-/Metric-/Section-Komponenten
- `presentation/dashboard/DashboardStringResolver`: UI-Strings für ViewModels testbar auflösbar ohne Android-`Context` direkt im ViewModel

Mehr Projektdetails: [CLAUDE.md](CLAUDE.md)

## OAuth2-Konfiguration

| Feld | Wert |
|---|---|
| Client ID | `euda-2c8d2760-d459-40aa-adc9-6eb7a8b91bd7` |
| Redirect URI | `m24bikestats://oauth-callback` |
| Flow | Authorization Code + PKCE |

## Verifizierte Endpunkte

Stand: 4. April 2026, live mit echtem Smart-System-Token getestet.

| Endpoint | Status | Zweck |
|---|---|---|
| `GET /activity/smart-system/v1/activities?limit=20&offset=0` | `200` | Aktivitätenliste |
| `GET /activity/smart-system/v1/activities/{activityId}/details` | `200` | Aktivitätsdetails |
| `GET /bike-profile/smart-system/v1/bikes` | `200` | Bike-Liste |
| `GET /bike-profile/smart-system/v1/bikes/{bikeId}` | `200` | Bike-Detail |
| `GET /activity/smart-system/v1/activities/{activityId}/track` | `404` | aktuell nicht verfügbar, `/details` wird stattdessen genutzt |
| `GET .../userinfo` | `200` | OIDC Userinfo |
| `GET .../.well-known/openid-configuration` | `200` | OIDC Discovery |

## Testabdeckung

- Unit-Tests für Mapper, UseCases und ViewModels
- Navigation- und Routing-Tests
- Repository- und Cache-Tests
- Room- und Migrations-Tests auf Android
- GPX-/CSV-Exporttests
- API-Test-Share- und Detailpunkt-Mapping-Tests

## Lizenz

MIT
