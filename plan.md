# Implementierungsplan: Home-Screen UI-Upgrade

Stand: 2026-04-08, überarbeitet 2026-04-08

Verwendete Skills:

- `mobile-android-design`
- `android-design-guidelines`
- `android-jetpack-compose`

Hinweis:
In diesem Schritt wird noch kein Produktivcode geändert. Dieses Dokument übersetzt den Designplan in einen konkreten Umsetzungsplan für das bestehende Repo.

## Ziel

Der Home-Screen soll innerhalb der bestehenden Dashboard-Architektur auf ein professionelles Android-Material-3-Niveau gehoben werden, ohne neue fachliche Features einzuführen.

Konkret bedeutet das:

- bessere visuelle Hierarchie
- weniger Überladung im ersten Fold
- klare Primary- und Secondary-Actions
- saubere Material-3-Farbrollen statt dekorativer Flächen
- konsistente Card-Sprache zwischen Home, Aktivitäten und Bike-Bereich
- robuste Darstellung für kleine Screens, große Schriftgrößen, Light/Dark und Dynamic Color

## Harte Projektgrenzen

Diese Umsetzung bleibt bewusst innerhalb der aktuellen Architektur:

- keine neuen Domain-Modelle
- keine neuen UseCases
- keine Änderungen an Repository-Verträgen
- keine Navigationsneugestaltung
- kein Feature-Umfang über UI/UX hinaus

Die vorhandenen Daten reichen aus:

- `HomeUiState`
- `DataStatusUiModel`
- bestehende Sync-Callbacks im `HomeScreen`
- bestehende Mapper- und ViewModel-Struktur

## Ausgangslage im Repo

### Relevante Dateien

Theme und Tokens:

- `app/src/main/java/info/meuse24/m24bikestats/presentation/theme/Color.kt`
- `app/src/main/java/info/meuse24/m24bikestats/presentation/theme/Theme.kt`
- `app/src/main/java/info/meuse24/m24bikestats/presentation/theme/Type.kt`

Home und Shared UI:

- `app/src/main/java/info/meuse24/m24bikestats/presentation/dashboard/HomeScreen.kt`
- `app/src/main/java/info/meuse24/m24bikestats/presentation/dashboard/DashboardSharedUi.kt`
- `app/src/main/java/info/meuse24/m24bikestats/presentation/dashboard/DashboardOverviewComponents.kt`
- `app/src/main/java/info/meuse24/m24bikestats/presentation/dashboard/DashboardScreenStates.kt`
- `app/src/main/java/info/meuse24/m24bikestats/presentation/dashboard/DashboardUiModels.kt`
- `app/src/main/java/info/meuse24/m24bikestats/presentation/dashboard/DashboardUiModelMapper.kt`

Strings:

- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-de/strings.xml`

Bestehende Tests:

- `app/src/test/java/info/meuse24/m24bikestats/presentation/dashboard/DashboardUiModelMapperTest.kt`
- `app/src/test/java/info/meuse24/m24bikestats/presentation/dashboard/DashboardViewModelTest.kt`
- `app/src/test/java/info/meuse24/m24bikestats/domain/usecase/ObserveDataStatusOverviewUseCaseTest.kt`

### Technische Beobachtungen

- `Type.kt` definiert aktuell praktisch nur `bodyLarge`; die restliche Typo-Hierarchie fehlt als bewusstes System.
- `Theme.kt` nutzt Dynamic Color korrekt, aber die statische Fallback-Palette ist noch Starter-Material (nur `primary`, `secondary`, `tertiary` explizit gesetzt).
- `HomeScreen.kt` bündelt Titel, Badge, Erklärung, Zeitraum, KPI, Sync-Metadaten, Progress und mehrere CTAs in einer einzigen Hero-Card.
- Die primäre CTA (`Jetzt synchronisieren`) steht in der aktuellen `DataStatusAndSyncCard` ganz unten — nach konditionalen sekundären Buttons. Das widerspricht der geplanten Action-Hierarchie und muss explizit korrigiert werden.
- `DashboardSharedUi.kt` nutzt große lineare Gradients für Hero-Flächen. Laut Android-/Material-3-Guidelines sollten große Flächen primär über `surface`-Rollen statt über primäre Akzentfarben laufen.
- In `HomeScreen.kt` existieren bereits die privaten Composables `HomeSummaryCard`, `HomeMetricGrid` und `HomeMetricTile`. Diese funktionieren bereits korrekt mit Material-3-Rollen und müssen in Paket 2 zu Shared-Primitives *promoted* werden, nicht neu erstellt.
- Es gibt zwei Badge-Implementierungen: `StatusBadge` (intern in `DashboardSharedUi.kt`) und `DataStatusBadge` (privat in `HomeScreen.kt`). Diese müssen in Paket 2 zu einer einzigen `DashboardStatusBadge` konsolidiert werden.
- `HomeUiState` liegt in `DashboardScreenStates.kt`, nicht in `DashboardUiModels.kt`. Das ist für Paket 4 relevant, da dort Convenience-Properties hinzukommen können.
- `HomeUiState` enthält das Flag `showExplanationTexts`, das über die App-Einstellungen gesteuert wird. Das neue Layout muss dieses Flag weiterhin korrekt berücksichtigen.
- Es gibt aktuell keine Compose-Previews im Projekt. Das ist für UI-Arbeit ein echter blinder Fleck.

## Zielarchitektur für den Home-Screen

Der neue Home-Screen bleibt eine `LazyColumn`, wird aber visuell und strukturell neu geschnitten.

Geplante Reihenfolge der Blöcke:

1. `HomeStatusHeroCard`
2. `HomeStatusMetricGrid`
3. `HomeSyncMetaCard`
4. `Latest Ride`
5. `Bike Status`
6. `Recent Exports`

### Was aus der aktuellen `DataStatusAndSyncCard` wird

Die bestehende große Card wird nicht erweitert, sondern aufgelöst.

Geplante Aufteilung:

- `HomeStatusHeroCard`
  - Gesamtstatus
  - kurze Summary
  - optional Zeitraum
  - genau eine primäre Aktion

- `HomeStatusMetricGrid`
  - 4 bis 6 KPI-Kacheln
  - Zahl dominant, Label sekundär

- `HomeSyncMetaCard`
  - letzter Aktivitäts-/Detail-/Bike-Sync
  - laufender Sync-Fortschritt
  - sekundäre Reparaturaktionen nur wenn relevant

## Konkrete Designentscheidungen für die spätere Umsetzung

### 1. Hero-Fläche

Nicht mehr:

- Vollflächen-Gradient über große Card-Bereiche
- mehrere gleich starke Buttons untereinander
- horizontale KPI-Pills direkt im Hero

Stattdessen:

- ruhige `surfaceContainerHigh`- oder `surfaceContainer`-Fläche
- kleiner farblicher Akzent nur über Badge, Status-Indikator oder Top-Strip
- kompakter Titel
- eine Statuszeile
- eine primäre CTA

### 2. KPI-Darstellung

Nicht mehr:

- kleine Capsule-Pills als Hauptmetriken

Stattdessen:

- 2x2 oder 2x3 Grid
- klare Zahlenhierarchie
- visuelle Problem-Markierung über Tonalität, nicht nur Farbe

### 3. Action-Hierarchie

Der Home-Screen bekommt eine klare Bedienlogik:

- primär: `Jetzt synchronisieren`
- sekundär: `Fehlende Details laden`
- sekundär: `Details auffrischen`
- Abbruch-Action nur im aktiven Sync-Zustand

### 4. Copy

Der erste Fold wird sprachlich verdichtet:

- kürzere Titel
- kürzere Status-Summaries
- technische Metadaten aus dem Hero entfernen
- kürzere Button-Labels

## Umsetzungspakete

## Paket 1: Theme-System professionalisieren

Ziel:
Aus Material-Defaults ein bewusstes Designsystem machen.

Betroffene Dateien:

- `presentation/theme/Color.kt`
- `presentation/theme/Theme.kt`
- `presentation/theme/Type.kt`
- neue Datei: `presentation/theme/DesignTokens.kt`

Konkrete Arbeit:

- Fallback-Palette in `Color.kt` durch vollständige Light-/Dark-Tonalpalette ersetzen
- Farbpalette aus einem Seed ableiten, nicht aus losem Lila/Starter-Set
- `Theme.kt` auf vollständige Material-3-Rollen heben
- Dynamic Color als Default beibehalten
- in `DesignTokens.kt` feste App-Tokens definieren:
  - `ScreenHorizontalPadding`
  - `SectionSpacing`
  - `CardCornerLarge`
  - `CardCornerMedium`
  - `CardBorderAlpha`
  - `ContentMaxWidth`
- `Type.kt` mit vollständiger Material-3-Typografie befüllen:
  - `headlineSmall`
  - `titleLarge`
  - `titleMedium`
  - `titleSmall`
  - `bodyLarge`
  - `bodyMedium`
  - `bodySmall`
  - `labelLarge`
  - `labelMedium`
  - `labelSmall`

Entscheidung:

- keine benutzerdefinierten Hex-Farben in Composables
- keine große Primärfarbe als Hintergrundfläche
- kleine Akzente über Badge, Indikator und CTA

Ergebnis nach Paket 1:

- besserer typografischer Kontrast
- kontrollierte Flächenhierarchie
- wiederverwendbare Tokens für das gesamte Dashboard

## Paket 2: Gemeinsame Dashboard-Grundbausteine neu ordnen

Ziel:
Bevor Home umgebaut wird, müssen die Shared-Primitives professioneller werden.

Betroffene Dateien:

- `presentation/dashboard/DashboardSharedUi.kt`
- optional neue Datei: `presentation/dashboard/DashboardDesignPrimitives.kt`

Konkrete Arbeit:

- `HeroCard` fachlich und visuell entschärfen
- Gradient-abhängige Hero-Logik entfernen
- gemeinsame Card-Primitive definieren:
  - `DashboardSectionCard`
  - `DashboardHeroSurface`
  - `DashboardMetricTile`
  - `DashboardStatusBadge`
  - `DashboardMetaRow`
- `HomeSummaryCard`, `HomeMetricGrid` und `HomeMetricTile` aus `HomeScreen.kt` hierhin *verschieben* und als interne Shared-Primitives promotieren (nicht neu erstellen — sie existieren bereits und funktionieren korrekt)
- `StatusBadge` (aus `DashboardSharedUi.kt`) und `DataStatusBadge` (aus `HomeScreen.kt`) zu einer einzigen `DashboardStatusBadge` konsolidieren — beide Verwendungsstellen danach auf die neue Komponente umstellen
- bestehende `MetricPill` und `CompactMetricPill` nicht mehr als primäre KPI-Komponente verwenden
- `SectionSurface` tonal und border-seitig an neues Theme anpassen
- optional einen zentralen Content-Container ergänzen:
  - `DashboardPageContainer`
  - auf großen Breiten max. ca. 840dp

Android-guideline Bezug:

- auf Medium/Expanded Width nicht ungebremst über die volle Breite laufen
- Surface-Rollen semantisch einsetzen
- große Flächen nicht in Akzentfarben tränken

Ergebnis nach Paket 2:

- saubere Bausteine für Home, Aktivitäten und Bike-Bereich
- weniger Styling-Duplikate in einzelnen Screens

## Paket 3: Home-Screen in kleine, fokussierte Composables zerlegen

Ziel:
Die aktuelle große Status-Card in klar getrennte Bereiche aufspalten.

Betroffene Datei:

- `presentation/dashboard/HomeScreen.kt`

Geplante neue interne Composables:

- `HomeStatusHeroCard`
- `HomeStatusMetricGrid`
- `HomeStatusMetricTile`
- `HomeSyncMetaCard`
- `HomeSyncProgressCard`
- `HomePrimaryActionBar`
- `HomeSecondaryActionRow`
- `HomeSectionHeader`

Konkrete Arbeit:

- `DataStatusAndSyncCard` entfernen oder zu dünnem Orchestrator umbauen
- Status-Hero nur noch für den Gesamtzustand verwenden
- Sync-Metadaten aus dem Hero herausziehen
- Progress-Zustände nur in eigenem Block darstellen
- sekundäre Aktionen nicht mehr als Full-Width-Stapel direkt im Hero
- bestehende `HomeSummaryCard`, `HomeMetricGrid`, `HomeMetricTile` auf neues Tokensystem umstellen

Technische Regeln aus Compose-Sicht:

- State bleibt in `HomeScreen`; neue Composables bleiben stateless
- `Modifier` als erstes optionales Argument
- keine neue Business-Logik in Composables
- UI nur aus `HomeUiState` ableiten
- `showExplanationTexts` weiterhin korrekt auswerten: Hero-Summary und erläuternde Texte in Leer-Zuständen bleiben conditional
- primäre CTA (`Jetzt synchronisieren`) muss im neuen Layout visuell und in der Render-Reihenfolge an erste Stelle rücken — sekundäre Aktionen folgen darunter

Ergebnis nach Paket 3:

- Home-Code wird lesbarer
- jede visuelle Zone hat eine einzige Aufgabe
- spätere UI-Iteration wird viel einfacher

## Paket 4: Data-Status-UI-Modell für das neue Layout vorbereiten

Ziel:
Die UI soll einfacher renderbar sein, ohne Domain-Schichten anzufassen.

Betroffene Dateien:

- `presentation/dashboard/DashboardUiModels.kt`
- `presentation/dashboard/DashboardScreenStates.kt` (enthält `HomeUiState`)
- `presentation/dashboard/DashboardUiModelMapper.kt`
- optional `presentation/dashboard/DashboardStateMappings.kt`

Konkrete Arbeit:

- prüfen, ob `DataStatusUiModel` für das neue Layout ausreicht
- falls nötig nur presentation-seitige Hilfsfelder ergänzen — als berechnete Getter (`val isComplete get() = statusTone == DataStatusTone.COMPLETE`), nicht als gespeicherte Felder:
  - `isComplete`
  - `hasMissingDetails`
  - `hasStaleDetails`
  - `primaryActionLabel`
  - `primaryActionKind`
  - `statusHeadline`
- keine neue fachliche Logik in Domain oder UseCases
- Copy-Verdichtung bereits im Mapper vorbereiten, damit `HomeScreen` keine Textlogik dupliziert

Wichtig:

- wenn zusätzliche Felder nötig werden, nur in `DataStatusUiModel`, nicht in `DataStatusOverview`
- `HomeUiState` liegt in `DashboardScreenStates.kt`; falls Action-Logik sinnvoller dort aufgehängt ist, kann Paket 4 diese Datei ebenfalls leicht berühren
- `DashboardUiModelMapperTest.kt` entsprechend mitziehen

Ergebnis nach Paket 4:

- weniger `when`-/`if`-Verzweigung direkt im Composable
- klarere Trennung zwischen Mapping und Rendering

## Paket 5: Activity- und Bike-Karten an die neue Designsprache angleichen

Ziel:
Der Home-Screen darf nach dem Umbau nicht wie ein Fremdkörper wirken.

Betroffene Datei:

- `presentation/dashboard/DashboardOverviewComponents.kt`

Konkrete Arbeit:

- `ActivityCard` an neue Card-Radien, Borders und Surface-Rollen anpassen
- Action-Bereich in `ActivityCard` entschlacken
- `BikeOverviewCard` gleichziehen
- `StatusBadge` visuell an neue Statuslogik anpassen
- Hero-Komponenten in Aktivitäten/Bikes auf neues Shared-Primitive umstellen

Bewusste Abgrenzung:

- keine inhaltliche Änderung an Activity-/Bike-Features
- nur visuelle Harmonisierung

Ergebnis nach Paket 5:

- Home, Aktivitäten und Bikes wirken wie ein gemeinsames Produkt
- keine Mischung aus altem und neuem UI-Charakter

## Paket 6: Texte auf Mobile-Produktniveau verdichten

Ziel:
Die visuelle Verbesserung muss von kürzerer, klarerer Sprache unterstützt werden.

Betroffene Dateien:

- `res/values/strings.xml`
- `res/values-de/strings.xml`

Konkrete Arbeit:

- Home-bezogene Titel verkürzen
- Status-Summaries kürzen
- CTA-Labels schärfen
- Labels für Metazeilen kürzen
- deutsche und englische Version immer parallel anpassen

Konkrete Copy-Richtung:

- weniger Erklärung
- mehr Status
- weniger technische Formulierungen im Hero
- lange Substantivketten vermeiden

Beispiele für spätere Überarbeitung:

- `Datenzustand & Sync`
- `Der lokale Tourenbestand ist vollständig ...`
- `Vorhandene Details auffrischen`
- `Cloud-Daten neu einlesen`

Ergebnis nach Paket 6:

- besserer erster Eindruck
- weniger Zeilenumbrüche auf Compact Width
- weniger Textdruck im oberen Bereich

## Paket 7: Preview- und QA-Infrastruktur ergänzen

Ziel:
UI-Arbeit nicht blind nur gegen Emulator-Screenshots machen.

Geplante neue Dateien:

- `presentation/dashboard/HomeScreenPreview.kt`
- optional `presentation/dashboard/DashboardCardPreview.kt`

Konkrete Preview-Fälle:

- Home komplett
- Home teilweise unvollständig
- Home leer
- Home während aktivem Sync
- Light Theme
- Dark Theme
- große Schriftgröße

Wichtig:

- da aktuell keine `@Preview` vorhanden sind, ist dieses Paket Pflicht
- Previews sollen mit stabilen Fake-`HomeUiState`-Objekten arbeiten

Ergebnis nach Paket 7:

- schnellere UI-Iteration
- weniger Regressionen
- bessere Review-Basis

## Paket 8: Testanpassungen

Ziel:
UI-Refactoring ohne fachliche Regression.

Betroffene Dateien:

- `DashboardUiModelMapperTest.kt`
- `DashboardViewModelTest.kt`

Konkrete Arbeit:

- Mapper-Tests an neue Status-Copy und ggf. neue UI-Hilfsfelder anpassen
- ViewModel-Tests nur anfassen, wenn UI-Modell-Struktur geändert wird
- keine Domain-Tests anfassen, solange keine fachlichen Modelle geändert werden

Nicht geplant:

- Screenshot-Tests im ersten Schritt
- neue Instrumentation-Tests nur für das visuelle Redesign

## Umsetzungsreihenfolge

Die spätere Umsetzung sollte in genau dieser Reihenfolge passieren:

1. `Theme.kt`, `Type.kt`, `Color.kt`, neue `DesignTokens.kt`
2. `DashboardUiModels.kt`, `DashboardScreenStates.kt` und `DashboardUiModelMapper.kt`
3. `DashboardSharedUi.kt`
4. `HomeScreen.kt`
5. `DashboardOverviewComponents.kt`
6. `values/strings.xml` und `values-de/strings.xml`
7. Previews
8. Tests

Warum diese Reihenfolge:

- erst System, dann Screen
- UIModels vor HomeScreen: `HomeScreen.kt` soll die neuen Convenience-Getter aus `DataStatusUiModel` nutzen können — erst mappen, dann rendern
- erst Bausteine, dann Spezialfall
- erst Rendering, dann Copy-Feinschliff

## Geplanter Dateiumfang

Voraussichtlich zu ändern:

- `app/src/main/java/info/meuse24/m24bikestats/presentation/theme/Color.kt`
- `app/src/main/java/info/meuse24/m24bikestats/presentation/theme/Theme.kt`
- `app/src/main/java/info/meuse24/m24bikestats/presentation/theme/Type.kt`
- `app/src/main/java/info/meuse24/m24bikestats/presentation/dashboard/DashboardSharedUi.kt`
- `app/src/main/java/info/meuse24/m24bikestats/presentation/dashboard/HomeScreen.kt`
- `app/src/main/java/info/meuse24/m24bikestats/presentation/dashboard/DashboardUiModels.kt`
- `app/src/main/java/info/meuse24/m24bikestats/presentation/dashboard/DashboardScreenStates.kt` (ggf. Convenience-Properties auf `HomeUiState`)
- `app/src/main/java/info/meuse24/m24bikestats/presentation/dashboard/DashboardUiModelMapper.kt`
- `app/src/main/java/info/meuse24/m24bikestats/presentation/dashboard/DashboardOverviewComponents.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-de/strings.xml`
- `app/src/test/java/info/meuse24/m24bikestats/presentation/dashboard/DashboardUiModelMapperTest.kt`
- `app/src/test/java/info/meuse24/m24bikestats/presentation/dashboard/DashboardViewModelTest.kt`

Geplante neue Dateien:

- `app/src/main/java/info/meuse24/m24bikestats/presentation/theme/DesignTokens.kt`
- `app/src/main/java/info/meuse24/m24bikestats/presentation/dashboard/HomeScreenPreview.kt`

Optional neue Datei, falls Shared UI zu groß wird:

- `app/src/main/java/info/meuse24/m24bikestats/presentation/dashboard/DashboardDesignPrimitives.kt`

## Abnahmekriterien

Die spätere Implementierung ist erfolgreich, wenn:

- der obere Bereich des Home-Screens nur noch eine klare Hauptbotschaft trägt
- genau eine primäre CTA im Hauptbereich sichtbar ist
- KPI-Zahlen auf einen Blick lesbar sind
- Sync-Metadaten nicht mehr den Hero dominieren
- Activity- und Bike-Karten sichtbar besser zum neuen Home-Stil passen
- Light, Dark und Dynamic Color stabil wirken
- 200% Font Scale keine überlappenden oder abgeschnittenen Texte erzeugt
- lange deutsche Strings den ersten Fold nicht mehr zerstören

## QA-Checkliste für die spätere Umsetzung

Visuell:

- Compact Width auf kleinem Android-Screen
- Medium/Expanded Width mit begrenzter Content-Breite
- Light Theme
- Dark Theme
- Dynamic Color mit mindestens drei unterschiedlichen Wallpapers

Accessibility:

- alle Buttons >= 48dp Touch Target
- sinnvolle `contentDescription` auf interaktiven Icons
- Status nicht nur über Farbe vermittelt
- TalkBack-Reihenfolge logisch

Typografie:

- keine Text-Clips bei großer Schrift
- Labels maximal 11sp nur für echte Metadaten
- Body nie unter 12sp

Interaction:

- Sync-Zustände zeigen sofort sichtbare Rückmeldung
- Cancel nur im aktiven Prozess sichtbar
- keine konkurrierenden Primäraktionen

## Empfohlene spätere Commit-Struktur

Wenn die Umsetzung startet, sollte sie in kleine, reviewbare Schritte geschnitten werden:

1. `theme: define full material tokens and typography`
2. `ui: prepare data status ui model and mapper for new layout`
3. `ui: refactor shared dashboard surfaces and consolidate badges`
4. `ui: split home status card into focused sections`
5. `ui: align activity and bike cards with new dashboard language`
6. `copy: shorten home strings for compact mobile layout`
7. `test: add previews and adjust mapper tests`

## Kurzfassung der eigentlichen Bauentscheidung

Der Home-Screen wird nicht "hübscher gemacht", sondern strukturell neu organisiert:

- weniger Dichte im Hero
- mehr Ruhe in den Flächen
- stärkere KPI-Hierarchie
- klarere Actions
- konsistenteres Theme

Das ist der kleinste sinnvolle Umbau, der den Rookie-Eindruck sichtbar in Richtung professionelles Android-Produkt verschiebt, ohne fachlich unnötig breit zu werden.
