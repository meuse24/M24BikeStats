# Plan: PDF-Zusammenfassungsbericht

**Stand:** 2026-04-06  
**Ziel:** Herunterladbarer PDF-Bericht mit übersichtlicher Zusammenfassung aller vorhandenen App-Daten (Nutzer, Konto, Fahrräder/Komponenten, Aktivitäten, Statistik inkl. Diagramm). Keine Einzelauflistung von Aktivitäten – nur aggregierte Kennzahlen.

---

## 1. Bibliothekswahl

**Empfehlung: Android native `android.graphics.pdf.PdfDocument` + `Canvas`**

| Kriterium | Bewertung |
|---|---|
| Keine Zusatz-Abhängigkeit | ✅ Im Android SDK seit API 19, minSdk 29 erfüllt |
| Lizenz | ✅ Kein AGPL/LGPL-Problem (iText 7 = AGPL, OpenPDF = LGPL) |
| Volle Canvas-Kontrolle | ✅ Exaktes Layout, Farben, Diagramme direkt zeichenbar |
| Vico-Chart im PDF | ✅ Balkendiagramm mit Canvas-Primitiven nativ replizierbar |
| Proguard-sicher | ✅ Kein zusätzliches Keep-Rule nötig |

**Begründung gegen externe Libs:**
- iText 7 Community → AGPL, erfordert Source-Offenlegung bei Verbreitung
- PdfBox-Android → Apache 2.0, aber instabiler Community-Port, kein offizielles Android-AAR
- Android-native Lösung ist Best Practice für reine Android-Apps ohne komplexe Office-Kompatibilität

**Für Text-Rendering auf Canvas wird `android.text.StaticLayout` verwendet** (korrekte Mehrzeilen-Umbrüche, RTL-sicher).

---

## 2. Inhalt und Seitenstruktur

Das PDF besteht aus **6 Abschnitten** auf ca. 4–6 DIN-A4-Seiten (595 × 842 pt bei 72 dpi):

### Seite 1 – Deckblatt
- App-Titel **M24 Bike Stats** (M24 visuell hervorgehoben, Primärfarbe)
- Untertitel: „Persönlicher Fahrtenbericht"
- Erstellt-Datum, Uhrzeit, Zeitzone
- Benutzer-E-Mail und Username (aus `OidcUserInfoUiModel`)
- Horizontaler Trenner
- Kurz-Übersicht als kompakte Kennzahlen-Zeile:  
  Gesamttouren · Gesamtdistanz · Gesamtfahrtzeit · Zeitraum (früheste → späteste Aktivität)

### Seite 2 – Konto & Profil
- **Abschnitt Nutzerkonto**: E-Mail, Username, Subject (OIDC Sub)
- **Abschnitt OAuth-Endpunkte**: Issuer, Token-Endpoint, UserInfo-Endpoint (aus `OidcDiscoveryInfoUiModel`)
- Alle Felder als zweispaltige Label/Wert-Tabelle dargestellt

### Seite 3 – Fahrräder & Komponenten
Für jedes `BoschBike`:
- **Antriebseinheit (DriveUnit)**: Produktname, Seriennummer, Gesamtkilometer (Odometer), Betriebsstunden (`totalPowerOnHours`), max. Unterstützungsgeschwindigkeit, aktive Assistenzmodi als Liste
- **Akku/Batterien**: Pro Batterie Produktname, Ladezyklen gesamt/im Sattel/außerhalb, gelieferte Wh über Lebenszeit
- **Fernbedienung & Head Unit**: Produktname, Seriennummer (falls vorhanden)
- Trennlinie zwischen Bikes

### Seite 4 – Aktivitäten-Übersicht
Aggregierte Gesamtstatistik über alle gecachten Aktivitäten:

| Kennzahl | Quelle |
|---|---|
| Anzahl Touren gesamt | `totalTours` |
| Gesamtdistanz (km) | `totalDistanceKm` |
| Gesamtfahrtzeit (h) | `totalDurationHours` |
| Ø Distanz/Tour (km) | `avgDistanceKm` |
| Ø Fahrtzeit/Tour (h) | `avgDurationHours` |
| Zeitraum (von – bis) | früheste/späteste `startTime` aus Aktivitätsliste |
| Ø Reisetempo (km/h) | `avgTravelSpeedKmh` aus Highlights |
| Gesamter Höhengewinn (m) | `totalElevationGainM` |
| Verbrauchte Kalorien gesamt | `totalCaloriesBurned` |

Darstellung als **3-spaltige Kennzahlen-Kacheln** (ähnlich den Summary-Tiles auf dem Statistik-Screen).

### Seite 5 – Statistik & Diagramm
- **Gruppierer**: Monatsweise (feste Wahl für PDF – sinnvollste Verdichtung)
- **Balkendiagramm Distanz**: Canvas-Balken für jeden `PeriodStats.distanceKm`, X-Achse = Monats-Label, Y-Achse = km (automatische Skalierung). Balken in Primärfarbe. Tourenzahl als Label über dem Balken.
- **Linie Fahrtzeit**: Zweite Y-Achse rechts, Linienpunkte über den Balken in Sekundärfarbe.
- **Durchschnittslinien**: Horizontale gestrichelte Linie für Ø-Distanz und Ø-Fahrtzeit.
- Legende unterhalb des Charts
- Darunter: Highlights-Kacheln (Bestleistungen)

| Highlight | Quelle |
|---|---|
| Längste Tour | `longestTourKm` |
| Max. Geschwindigkeit | `maxSpeedKmh` |
| Max. Fahrerleistung | `maxRiderPowerWatts` |
| Lieblings-Wochentag | `favoriteDayOfWeek` |

### Seite 6 – Rhythmus & Frequenz
- **Wochentagsverteilung**: Horizontales Balkendiagramm (Mo–So), Balkenbreite proportional zur Tour-Häufigkeit, Anzahl als Label
- **Wochenfrequenz-Histogramm**: „Wie viele Wochen hattest du X Touren?" als kompakte Tabelle (0 Touren / 1 Tour / 2 Touren / 3+ Touren)
- **Aktivitätsquote**: Prozent der Wochen mit mind. 1 Tour (`activeWeeksRatio`)
- Footer mit App-Name, Version und Generierungsdatum

---

## 3. Neue Dateien und Klassen

### 3.1 Domain Layer (Android-frei)

**`domain/model/PdfReportData.kt`**  
Aggregiertes Datenmodell, das alle für das PDF benötigten Informationen trägt. Wird vom UseCase befüllt und an den Generator übergeben.

```kotlin
data class PdfReportData(
    val generatedAt: Instant,
    val userInfo: OidcUserInfoUiModel?,
    val discoveryInfo: OidcDiscoveryInfoUiModel?,
    val bikes: List<BoschBike>,
    val statisticsState: StatisticsUiState,   // enthält periods, highlights, totals
    val earliestActivityDate: Instant?,
    val latestActivityDate: Instant?,
)
```

**`domain/usecase/ExportPdfSummaryReportUseCase.kt`**  
Orchestriert die Datenbeschaffung aus bestehenden Repositories. Gibt `Result<PdfReportData>` zurück.

```
Ablauf:
1. userInfoProvider.loadCurrentUserInfo()
2. discoveryInfoProvider.loadCurrentDiscovery()
3. bikesRepository.getCachedBikes()
4. getStatisticsUseCase() → Liste<BoschActivity>
5. statisticsMapper.map(activities, grouping = MONTH) → StatisticsUiState
6. Datum-Grenzen aus Aktivitätsliste ermitteln
7. PdfReportData(...) zusammenbauen → Result.success(...)
```

### 3.2 Data Layer

**`data/export/PdfReportGenerator.kt`**  
Context-gebundener Generator. Erzeugt ein `PdfDocument` aus einem `PdfReportData`-Objekt und schreibt es in eine Datei im Cache-Dir. Gibt `Uri` (FileProvider) zurück.

Unterklassen/Helfer (package-private):

- **`PdfPageBuilder.kt`** – Wrapper um `PdfDocument.Page` + `Canvas`. Kapselt:
  - `drawSectionHeader(text)` – Abschnittstitel mit Unterstrich
  - `drawLabelValueRow(label, value)` – Zweispaltige Zeile
  - `drawMetricTile(label, value, unit)` – Kachel mit großem Wert
  - `drawBarChart(periods, avgDistance, avgDuration)` – Balken + Linie auf Canvas
  - `drawHorizontalBarChart(data: Map<String, Int>)` – Wochentagsverteilung
  - `drawText(text, x, y, paint)` – StaticLayout-basierter Mehrzeilen-Text
  - Automatische Y-Cursor-Verwaltung (aktueller Zeichnungspunkt), Seitenüberlauf-Erkennung → neue Seite

- **`PdfColorScheme.kt`** – Farb-Konstanten (Primär, Sekundär, Grau, Weiß, Schwarz) als `Int`-Werte, unabhängig von Compose-Themes

- **`PdfTypography.kt`** – `Paint`-Objekte für Titelzeilen, Fließtext, Labels, Werte, kleine Texte (Größen, Bold, Color vorkonfiguriert)

### 3.3 Presentation Layer

**`presentation/dashboard/DashboardScreenStates.kt`** (bestehende Datei, erweitern)  
`FunctionsUiState` bekommt drei neue Felder:
```kotlin
val isExportingPdf: Boolean = false
val pendingPdfExport: PdfExportUiModel? = null    // fileName + Uri
val lastPdfExport: PdfExportSummaryUiModel? = null // fileName, exportedAtLabel
```

**`presentation/dashboard/DashboardUiModels.kt`** (bestehende Datei, erweitern)  
Zwei neue UI-Modelle:
```kotlin
data class PdfExportUiModel(val fileName: String, val uri: Uri)
data class PdfExportSummaryUiModel(val fileName: String, val exportedAtLabel: String)
```

**`presentation/dashboard/DashboardViewModel.kt`** (bestehende Datei, erweitern)  
Drei neue Methoden nach dem bestehenden CSV-Muster:
```kotlin
fun exportPdfSummaryReport()     // startet Coroutine, setzt isExportingPdf
fun onPdfExportHandled()         // löscht pendingPdfExport aus State
fun cancelPdfExport()            // bricht laufende Job-Coroutine ab
```

**`presentation/dashboard/FunctionsScreen.kt`** (bestehende Datei, erweitern)  
- Neues `LaunchedEffect` für `pendingPdfExport` → `Intent.ACTION_SEND` mit `type = "application/pdf"`
- Neue Signatur-Parameter: `onExportPdf`, `onCancelPdfExport`, `onPdfExportHandled`
- Neues `FunctionsExportCard`-Item für den PDF-Bericht unterhalb der CSV-Karten

**`DashboardScreen.kt`** (bestehende Datei, minimal erweitern)  
Nur Parameter-Durchreichung der neuen Callbacks an `FunctionsScreen`.

### 3.4 Dependency Injection

**`di/AppModule.kt`** (bestehende Datei, erweitern)  
```kotlin
single { PdfReportGenerator(androidContext()) }
factory {
    ExportPdfSummaryReportUseCase(
        userInfoProvider = get<OidcUserInfoProvider>(),
        discoveryInfoProvider = get<OidcDiscoveryInfoProvider>(),
        bikesRepository = get<BoschSmartSystemRepository>(),
        getStatisticsUseCase = get<GetStatisticsUseCase>(),
        statisticsMapper = get<StatisticsUiModelMapper>(),
    )
}
```
`PdfReportGenerator` und `ExportPdfSummaryReportUseCase` werden per Koin-Injection in den `DashboardViewModel` gereicht.

### 3.5 FileProvider

Der bestehende `shared_exports`-Cache-Path in `file_paths.xml` deckt `.pdf`-Dateien bereits ab. **Kein Änderungsbedarf** in AndroidManifest oder `file_paths.xml`.

---

## 4. Implementierungsreihenfolge

| # | Schritt | Dateien | Abhängigkeiten |
|---|---|---|---|
| 1 | Domain-Modell anlegen | `domain/model/PdfReportData.kt` | – |
| 2 | UseCase anlegen | `domain/usecase/ExportPdfSummaryReportUseCase.kt` | Schritt 1 |
| 3 | `PdfColorScheme` + `PdfTypography` | `data/export/PdfColorScheme.kt`, `PdfTypography.kt` | – |
| 4 | `PdfPageBuilder` | `data/export/PdfPageBuilder.kt` | Schritt 3 |
| 5 | `PdfReportGenerator` | `data/export/PdfReportGenerator.kt` | Schritte 1, 4 |
| 6 | UI-Modelle erweitern | `DashboardUiModels.kt`, `DashboardScreenStates.kt` | – |
| 7 | ViewModel erweitern | `DashboardViewModel.kt` | Schritte 2, 5, 6 |
| 8 | FunctionsScreen erweitern | `FunctionsScreen.kt` | Schritt 6 |
| 9 | DashboardScreen Callbacks | `DashboardScreen.kt` | Schritt 8 |
| 10 | DI verkabeln | `AppModule.kt` | Schritte 2, 5, 7 |
| 11 | String-Ressourcen | `strings.xml`, `strings-de.xml` | – |
| 12 | Tests: UseCase | `ExportPdfSummaryReportUseCaseTest.kt` | Schritte 1–2 |
| 13 | Tests: Generator | `PdfReportGeneratorTest.kt` | Schritte 1–5 |
| 14 | `assembleRelease` prüfen | – | alle Schritte |

---

## 5. Technische Entscheidungen im Detail

### 5.1 Canvas-Koordinatensystem

Seitengröße: **595 × 842 pt** (DIN A4 bei 72 dpi – PDF-Standard). Android `PdfDocument` verwendet Pixel, 72 dpi wird von allen PDF-Viewern korrekt als DIN A4 interpretiert.

Rand: `margin = 40 px` links/rechts. Schreibbereich: `x ∈ [40, 555]`, `y`-Cursor startet bei `60`, Seitenende-Schwelle bei `y > 800` → automatisch neue Seite.

### 5.2 Balken-/Liniendiagramm ohne Vico

Das Diagramm wird **direkt auf Canvas gezeichnet** – keine Compose-zu-Bitmap-Konvertierung. Compose-Snapshots erfordern `ComposeView` + `ViewTreeLifecycleOwner` im Hintergrund-Thread und sind fehleranfällig; alle benötigten Daten sind bereits in `StatisticsUiState.periods` vorhanden.

Zeichenreihenfolge:
1. Hintergrundgitter (hellgraue horizontale Linien)
2. Balken (Distanz, Primärfarbe, abgerundete Ecken via `drawRoundRect`)
3. Linienpfad (Fahrtzeit, Sekundärfarbe, `Path` + `drawPath`)
4. Datenpunkte der Linie (Kreise, `drawCircle`)
5. Tourenzahl-Labels über Balken
6. X-Achsen-Labels (Monats-Kurznamen), Y-Achsen-Labels links (km)
7. Durchschnittslinien (gestrichelt via `PathEffect.dashPathEffect`)
8. Legende

Automatische Y-Skalierung: `maxY = periods.maxOf { distanceKm } * 1.15f`

### 5.3 Coroutine-Struktur im ViewModel

```kotlin
private var pdfExportJob: Job? = null

fun exportPdfSummaryReport() {
    pdfExportJob?.cancel()
    pdfExportJob = viewModelScope.launch(Dispatchers.IO) {
        _uiState.update { ... isExportingPdf = true }
        exportPdfSummaryReportUseCase()
            .onSuccess { reportData ->
                val uri = pdfReportGenerator.generate(reportData)
                val fileName = buildPdfFileName()   // "m24-bericht-2026-04-06.pdf"
                _uiState.update { ... pendingPdfExport = PdfExportUiModel(fileName, uri) }
            }
            .onFailure { /* Fehler in bestehenden Snackbar-Kanal leiten */ }
        _uiState.update { ... isExportingPdf = false }
    }
}
```

### 5.4 Datei-Benennung und FileProvider

```
Dateiname:    m24-bericht-2026-04-06.pdf
Cache-Pfad:   context.cacheDir/shared_exports/m24-bericht-2026-04-06.pdf
Content-URI:  content://info.meuse24.m24bikestats.fileprovider/shared_exports/m24-bericht-2026-04-06.pdf
```

Bereinigung analog zu `createSharedCsvUri`: lowercase, `[^a-z0-9._-]` → `-`.

Share-Intent:
```kotlin
Intent(Intent.ACTION_SEND).apply {
    type = "application/pdf"
    putExtra(Intent.EXTRA_STREAM, uri)
    putExtra(Intent.EXTRA_SUBJECT, fileName)
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
}
```

### 5.5 Lokalisierung im PDF-Generator

- `Locale.getDefault()` für alle Zahlenformatierungen (Dezimaltrenner, Tausendertrennzeichen)
- `DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)` für Datumsangaben
- PDF-Abschnittstitel werden über einen schlanken `PdfStringResolver`-Wrapper aufgelöst (analog zu `DashboardStringResolver`), damit `PdfReportGenerator` Android-Context-frei bleibt und testbar ist

---

## 6. Neue String-Ressourcen (Auswahl)

```xml
<!-- FunctionsScreen - PDF-Karte -->
<string name="functions_export_pdf_title">Zusammenfassungsbericht</string>
<string name="functions_export_pdf_subtitle">Kompakter PDF-Bericht mit Nutzerprofil, Fahrrädern, Aktivitätsstatistik und Diagramm</string>
<string name="functions_export_pdf_button">PDF erstellen</string>
<string name="functions_export_pdf_running">PDF wird erstellt …</string>
<string name="functions_cancel_pdf_button">Abbrechen</string>
<string name="functions_share_chooser_pdf">Bericht teilen</string>
<string name="functions_chip_pdf">PDF</string>
<string name="functions_scope_full_summary">Vollständige Zusammenfassung</string>

<!-- PDF-Abschnittstitel -->
<string name="pdf_section_cover">Fahrtenbericht</string>
<string name="pdf_section_account">Konto &amp; Profil</string>
<string name="pdf_section_bikes">Fahrräder &amp; Komponenten</string>
<string name="pdf_section_activities">Aktivitäten-Übersicht</string>
<string name="pdf_section_statistics">Statistik &amp; Diagramm</string>
<string name="pdf_section_rhythm">Rhythmus &amp; Frequenz</string>
<string name="pdf_label_generated">Erstellt am</string>
<string name="pdf_label_period">Zeitraum</string>
<string name="pdf_footer">Erstellt mit M24 Bike Stats</string>
```

---

## 7. Test-Strategie

**`ExportPdfSummaryReportUseCaseTest`** (JVM, kein Instrument nötig)
- Fake-Implementierungen für alle Provider/Repositories
- Prüft: korrektes Befüllen von `PdfReportData` aus den Fakes
- Prüft: `Result.failure` bei Fehler im userInfo-Provider wird korrekt propagiert

**`PdfReportGeneratorTest`** (Robolectric oder Instrumentierungstest)
- Prüft: Datei wird in `cacheDir/shared_exports/` angelegt
- Prüft: `PdfDocument.getPages().size >= 4`
- Prüft: FileProvider-URI hat Schema `content://`

**Manuelle Smoke-Tests:**
- PDF öffnen in: Google Drive, Adobe Acrobat, Samsung-eigener Viewer
- Release-Build: `assembleRelease` ohne R8-Fehler (kein Keep-Rule nötig, da native Android API)

---

## 8. Nicht im Scope (bewusste Abgrenzungen)

- **Keine Einzelauflistung von Aktivitäten** – nur Aggregate
- **Kein Kartenausschnitt / GPX-Visualisierung** (MapLibre renderbar nur im UI-Thread mit SurfaceView)
- **Kein `ACTION_PRINT`-Intent** – Share-Intent reicht, OS-Druck-Dialog ist vom Nutzer aufrufbar
- **Keine PDF-Verschlüsselung** – Daten sind app-intern, keine Übertragung ohne Nutzer-Aktion
- **Keine Setup-Einstellung für PDF-Format** – immer System-Locale

---

## 9. Betroffene Dateien – Gesamtübersicht

### Neu anlegen
```
app/src/main/java/info/meuse24/m24bikestats/
  domain/model/PdfReportData.kt
  domain/usecase/ExportPdfSummaryReportUseCase.kt
  data/export/PdfColorScheme.kt
  data/export/PdfTypography.kt
  data/export/PdfPageBuilder.kt
  data/export/PdfReportGenerator.kt

app/src/test/java/info/meuse24/m24bikestats/
  domain/usecase/ExportPdfSummaryReportUseCaseTest.kt
  data/export/PdfReportGeneratorTest.kt
```

### Bestehende Dateien erweitern
```
app/src/main/java/info/meuse24/m24bikestats/
  presentation/dashboard/DashboardUiModels.kt         (+2 Datenklassen)
  presentation/dashboard/DashboardScreenStates.kt     (+3 Felder in FunctionsUiState)
  presentation/dashboard/DashboardViewModel.kt        (+3 Methoden, +1 Job-Feld)
  presentation/dashboard/FunctionsScreen.kt           (+LaunchedEffect, +Karte, +Parameter)
  presentation/dashboard/DashboardScreen.kt           (+Callback-Durchreichung)
  di/AppModule.kt                                     (+2 Bindings)

app/src/main/res/values/strings.xml                   (+~15 Strings)
app/src/main/res/values-de/strings.xml                (+~15 Strings DE)
```

### Keine Änderungen nötig
```
AndroidManifest.xml        (FileProvider bereits konfiguriert)
file_paths.xml             (shared_exports deckt .pdf ab)
AppNavigation.kt           (keine neue Route)
MainShell.kt               (keine neue Route)
build.gradle.kts           (keine neue Abhängigkeit)
```
