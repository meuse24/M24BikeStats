# M24BikeStats

Android-App für Bosch eBike Smart System Fahrtdaten über das Bosch eBike Data Act Portal.

## Überblick

- OAuth2 + PKCE Login gegen Bosch SingleKey ID
- adaptives Compose-UI mit `home`, `activities`, `account`, `statistics` und `functions`
- sekundäre Navigation für `setup`, `hilfe`, `info`, `api-test` und `logout`
- Home-Top-Bar mit App-Branding statt generischem Bereichstitel
- Room-Cache für Aktivitäten, Aktivitätsdetails und Bikes
- cache-first Listen- und Detailansichten mit gezieltem Hintergrund-Refresh
- mehrstufiger Cloud-Sync vom Home-Screen für Bikes, Aktivitäten und optional fehlende bzw. veraltete Aktivitätsdetails
- optionaler täglicher Hintergrund-Sync über WorkManager mit konfigurierbaren Netzwerkbedingungen
- CSV-Export für Aktivitäten, Aktivitätsdetails und Tracks
- PDF-Zusammenfassungsbericht als cache-only Export mit Nutzerkonto, Bikes, Aktivitätsübersicht, Highlights sowie Wochen-, Monats- und Jahresdiagrammen
- CSV-Format mit Presets `Automatisch`, `Excel/Deutsch` und `Standard/International`
- cache-only Exporte, damit keine zusätzlichen Cloud-Abfragen während des Exports nötig sind
- GPX- und Track-Share-Funktionen
- robuster API-Test-Share als Datei statt großer Binder-Texttransaktion
- API-Test kann Ergebnisse zusätzlich direkt nach `Downloads/M24BikeStats` speichern
- Statistikscreen mit interaktivem Vico-Kombidiagramm: Distanzbalken mit Tourenzahl-Labels, Fahrtzeit-Linie, Wochen-/Monats-/Jahresaggregation, Durchschnittslinien für Distanz und Fahrtzeit, zusätzliche Durchschnitts-Tiles pro Tour, aufklappbarer Period-Detail-Card sowie einer read-only Sektion `Highlights & Rhythmus`
- MapLibre/OpenFreeMap-Kartenansicht mit roter Route, kompaktem Attribution-Overlay und klar getrennten Start-/Zielmarkern
- Weltkarte mit allen gecachten Touren als anklickbare Kreise; Tap navigiert zur Aktivitätsdetailseite; Kameraposition bleibt beim Zurücknavigieren erhalten; GPS-Zentrum per Tour = am weitesten vom Startpunkt entfernte Koordinate
- Profilcharts für Tracks
- Bereinigung und Kompression redundanter Detailpunkte für Karte, GPX und Profile
- Kontodetails zeigen zusätzlich Bosch-`USERINFO`, OIDC-Discovery und das aktuell passende OIDC-Signaturzertifikat aus der JWKS-Antwort
- Kontodetails zeigen zusätzlich Bike-Pass-, Service-Book- und Registrierungsdaten, sofern Bosch dafür Daten liefert
- aktive UI-Texte in Englisch und Deutsch lokalisiert
- Release-Build nutzt R8-Minify + Resource-Shrinking
- Android Auto-Backup und Device-Transfer-Backup sind deaktiviert, damit keine sensiblen Bosch-Daten aus App-Speicher oder Tokens unkontrolliert exportiert werden
- Cleartext-Traffic ist explizit deaktiviert
- API-Test und sonstige Diagnosepfade bleiben in Debug-Builds sichtbar und werden für Release-Builds aus der Endnutzer-Navigation entfernt

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
./gradlew assembleRelease
```

## Navigation

- `Home`: Übersicht, letzter Cloud-Abgleich, letzte Tour, Bike-Status, letzte Exporte
- `Home` zeigt in der Shell-Top-Bar den App-Titel `M24 Bike Stats`, wobei `M24` hervorgehoben ist
- `Aktivitäten`: paginierte Aktivitätenliste mit Suche, Datumsfilter und Sortierung; enthält Button zur Weltkarte
- `Weltkarte`: alle Touren als Kreise auf OpenFreeMap-Karte; Tap auf Kreis öffnet Aktivitätsdetail
- `Konto`: Bike-Liste plus Konto-/OIDC-Details
- `Funktionen`: CSV-Exporte und PDF-Zusammenfassungsbericht
- `Statistiken`: Wochen-/Monats-/Jahresaggregation aller gecachten Aktivitäten mit Vico-Kombidiagramm, Durchschnittslinien und Summary-Tiles für Gesamt- und Durchschnittswerte pro Tour; darunter `Highlights & Rhythmus` mit Bestleistungen, distanzstärkstem Zeitraum, effektiver Reisegeschwindigkeit, Wochentagsverteilung und Wochenfrequenz
- `Setup`: App-Einstellungen wie CSV-Format-Presets
- `Setup`: zusätzlich Detail-Sync-Modus `nur fehlende` oder `fehlende + veraltete`
- `Setup`: zusätzlich Hintergrund-Sync `deaktiviert`, `täglich per WLAN` oder `täglich in jedem Netz`
- `Hilfe` / `Info` / `API-Test`: Sekundärziele im Drawer oder Overflow
- `API-Test` ist nur in Debug-Builds als Diagnoseziel verfügbar

## Daten und Exporte

- Aktivitäten werden über `limit`/`offset` paginiert geladen.
- Aktivitätsdetails kommen über `/activity/smart-system/v1/activities/{activityId}/details`.
- Bikes kommen über `/bike-profile/smart-system/v1/bikes` und `/bikes/{bikeId}`.
- Zusätzliche Bike-Metadaten kommen über `/bike-pass/smart-system/v1/bike-passes?bikeId=...`, `/service-book/smart-system/v1/service-records?bikeId=...` und `/bike-registration/smart-system/v1/registrations`.
- Kontodetails ergänzen diese Bike-Daten um `/userinfo`, `/.well-known/openid-configuration` und `/protocol/openid-connect/certs`.
- Leere Antworten bei Bike Pass, Service Book oder Registrierungen sind fachlich möglich; `Service book = 0` ist daher kein technischer Fehler.
- Schlägt einer dieser Zusatz-Calls temporär fehl, bleibt der vorhandene Cache erhalten und wird nicht durch leere Daten ersetzt.
- Der separate `/track`-Endpunkt liefert aktuell `404`; Track, GPX und Profile basieren deshalb auf `/details`.
- Detailpunkte mit `0/0`-Koordinaten oder redundanten aufeinanderfolgenden Duplikaten werden vor Karten-/GPX-Nutzung bereinigt.
- Die Track-Karte blendet die Attribution kompakt direkt in der Karte ein: `© OSM • OFM • MapLibre`.
- Start- und Endpunkt sind fest farblich getrennt markiert: Start grün, Ziel lila, Route rot.
- CSV-Exporte nutzen den persistenten Setup-Wert für das Exportformat.
- `Automatisch` leitet aus den Dezimalkonventionen des Geräts ein passendes CSV-Preset ab.
- `Excel/Deutsch` nutzt Semikolon, Dezimalkomma und deutsches Datumsformat.
- `Standard/International` nutzt Komma, Dezimalpunkt und ISO-nahes Datumsformat.
- Der optionale Hintergrund-Sync plant genau einen eindeutigen periodischen WorkManager-Job und übernimmt dabei den im Setup gewählten Detail-Sync-Modus.
- Aktivitäten- und Detail-CSV exportieren nur Daten, die bereits in Room vorhanden sind.
- Der PDF-Bericht nutzt ebenfalls nur bereits lokal gecachte Daten und ergänzt sie um OIDC-UserInfo-/Discovery-Metadaten.
- Der PDF-Bericht enthält Highlights sowie Wochen-, Monats- und Jahresdiagramme mit jeweils passendem distanzstärkstem Zeitraum.
- Der Home-Sync zeigt Fortschritt und kann abgebrochen werden.
- Der Home-Sync kann datensparsam nur fehlende Aktivitätsdetails laden oder optional veraltete Details mitaktualisieren.
- Die Home-Übersicht zeigt zusätzlich die Anzahl gecachter Detaildatensätze und GPS-Punkte.
- Bike-Status nutzt zusätzlich Walk Assist, Einschaltzeit und Assist-Reichweiten aus den Bike-Details.

## Architektur

```text
domain/        Interfaces, Modelle, UseCases
api/           Bosch-Endpoint-Katalog für API-Test/Diagnose
data/          API- und Repository-Implementierungen, Room-Zugriff
auth/          OAuth2/AppAuth, Token-Verwaltung, OIDC-Helfer
background/    WorkManager-Scheduling und Settings-Beobachtung
presentation/  Compose-Screens, Navigation, ViewModels
shared/        gemeinsam genutzte Formatierungs-/Hilfscodecs
support/       Diagnose- und API-Test-Helfer
ui/            Theme und app-weite UI-Grundbausteine
di/            Koin-Modul
```

Ergänzungen:

- `presentation/navigation`: Root- und Shell-Navigation, adaptive Top-Bar/Drawer-Logik
- `presentation/dashboard`: Home, Aktivitäten, Konto, Funktionen sowie Detail- und Track-Screens
- `data/export`: CSV-/PDF-Export-Helfer, PDF-Layout-Builder und Android-gebundene Report-Dateigenerierung
- `domain/model/PdfReportData.kt`: Android-freies Aggregat für den PDF-Bericht
- `domain/usecase/ExportPdfSummaryReportUseCase.kt`: baut den Bericht aus Cache-, Bike- und OIDC-Daten auf
- `presentation/statistics`: `StatisticsScreen`, `StatisticsViewModel`, `StatisticsUiModelMapper`, `StatisticsUiState` inkl. formatierter Statistik-UI-Helfer, Wochen-/Monats-/Jahresaggregation, Durchschnittskennzahlen und read-only Highlights-/Rhythmus-Metriken aus gecachten `BoschActivity`-Daten
- `presentation/dashboard/DashboardScreen.kt`: nur noch Dashboard-Shell mit Tabs, Snackbar und Screen-Auswahl
- `presentation/dashboard/DashboardOverviewComponents.kt`: Karten-, Listen- und Filter-Komponenten für Aktivitäten und Bikes
- `presentation/dashboard/DashboardDetailScreens.kt`: Aktivitäts- und Bike-Detailscreens inkl. Share-/Detail-Sektionen
- `presentation/dashboard/DashboardTrackScreen.kt`: Track-Vollbild, Karten-/Canvas-Helfer und Exportdialog
- `presentation/dashboard/DashboardSharedUi.kt`: wiederverwendete Hero-/Metric-/Section-Komponenten
- `presentation/dashboard/DashboardStringResolver`: UI-Strings für ViewModels testbar auflösbar ohne Android-`Context` direkt im ViewModel
- `presentation/login/LoginStringResolver`: sichtbare Login-Statusmeldungen bleiben ebenfalls resource-basiert und testbar ohne Android-`Context` direkt im ViewModel
- `api/`: Endpoint-Definitionen für API-Test-/Diagnose-Flows; `BoschApiRequest`/`FetchBoschDataUseCase` liegen in `domain/`
- `auth/AuthFlowCoordinator`: Android-spezifischer Login-/Logout-Intent-Flow außerhalb der Präsentationsschicht
- `auth/OidcAccountInfo`: produktive OIDC-UserInfo-/Discovery-Logik für Kontodetails
- `auth/OidcCertificateInfo`: produktive OIDC-JWKS-/Zertifikatslogik für Kontodetails

## Lokalisierung

- Aktive Nutzertexte liegen in `app/src/main/res/values/strings.xml` und `app/src/main/res/values-de/strings.xml`.
- Sichtbare Compose-Texte in Navigation, Setup, Dashboard-Listen und Detail-Flows sollen über `stringResource(...)` kommen.
- ViewModel-seitige Nutzertexte laufen über kleine Resolver wie `DashboardStringResolver` und `LoginStringResolver`.
- Der PDF-Generator nutzt denselben Ansatz über `PdfStringResolver`, damit sichtbare Report-Texte testbar und lokalisiert bleiben.
- Technische Literale wie MIME-Types, Routen, JSON-Keys oder Dateinamen bleiben bewusst im Code.

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
| `GET /bike-pass/smart-system/v1/bike-passes?bikeId={bikeId}` | `200` | Bike-Pass und Theft-Logs |
| `GET /service-book/smart-system/v1/service-records?bikeId={bikeId}` | `200` | Servicehistorie pro Bike, kann leer sein |
| `GET /bike-registration/smart-system/v1/registrations` | `200` | Bike- und Komponentenregistrierungen |
| `GET /activity/smart-system/v1/activities/{activityId}/track` | `404` | aktuell nicht verfügbar, `/details` wird stattdessen genutzt |
| `GET .../userinfo` | `200` | OIDC Userinfo für den aktuell angemeldeten Bosch-Account |
| `GET .../.well-known/openid-configuration` | `200` | OIDC Discovery-Metadaten für Kontodetails |
| `GET .../protocol/openid-connect/certs` | `200` | OIDC JWKS / Signaturzertifikate |

## Testabdeckung

- Unit-Tests für Mapper, UseCases und ViewModels (inkl. Statistik-Mapper und -ViewModel)
- Navigation- und Routing-Tests
- Repository- und Cache-Tests
- Room- und Migrations-Tests auf Android
- GPX-/CSV-Exporttests
- API-Test-Share- und Detailpunkt-Mapping-Tests
- Karten-Tests: `ActivityCenterCalculatorTest` (Algorithmus, Kosinus-Korrektur), `ActivityMapPointGeoJsonMapperTest` (GeoJSON-Format)

## Lizenz

MIT
