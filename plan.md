# Implementierungsplan: Statistik-Highlights & Rhythmus-Block

Stand: 2026-04-06

## Ziel

Unter dem bestehenden Vico-Diagramm erscheint eine neue Sektion **"Highlights & Rhythmus"** mit formattierten
Aha-Werten und Bestleistungen — berechnet aus allen gecachten `BoschActivity`-Einträgen, unabhängig von
Wochen-/Monatsgruppierung des Charts.

Die Sektion ist rein lesend und nutzt denselben `GetStatisticsUseCase`-Flow wie der Rest des Screens.
Kein neuer API-Call, kein neuer Room-Query.

---

## Neue Datenstrukturen

### `StatisticsHighlights` — in `StatisticsUiState.kt`

```kotlin
data class StatisticsHighlights(
    // Bestleistungen
    val longestTourKm: Double,
    val totalElevationGainM: Int,           // 0 wenn alle Activities null haben
    val maxSpeedKmh: Double?,               // null wenn kein Wert vorhanden
    val maxRiderPowerWatts: Double?,
    val totalCaloriesBurned: Double?,

    // Effizienz
    val avgTravelSpeedKmh: Double?,         // Gesamtdistanz / Gesamtfahrzeit; null bei 0 Dauer

    // Rhythmus
    val favoriteDayOfWeek: java.time.DayOfWeek?,       // häufigster Starttag
    val dayOfWeekDistribution: Map<java.time.DayOfWeek, Int>,  // DayOfWeek → Tourenanzahl
    val weeklyFrequencyHistogram: Map<Int, Int>,        // toursPerWeek → Wochenanzahl
    val activeWeeksRatio: Double?,          // null wenn Zeitraum < 2 Kalenderwochen
)
```

`StatisticsUiState` bekommt ein neues Feld:

```kotlin
data class StatisticsUiState(
    // ... bestehende Felder ...
    val highlights: StatisticsHighlights? = null,   // null solange isLoading oder 0 Aktivitäten
)
```

---

## Schicht: Berechnung

### Neue Methode in `StatisticsUiModelMapper`

Statt einer eigenen Klasse wird `mapHighlights()` direkt in den bestehenden Mapper aufgenommen —
konsistent mit `mapPeriods()`, selbe Abhängigkeiten (`zoneId`, `locale`), kein extra DI-Binding nötig.

```kotlin
fun mapHighlights(activities: List<BoschActivity>): StatisticsHighlights? {
    if (activities.isEmpty()) return null
    // ... Berechnungen (s.u.) ...
}
```

**Bestleistungen:**

```kotlin
val longestTourKm = activities.maxOf { it.distanceMeters } / 1000.0
val totalElevationGainM = activities.sumOf { it.elevationGainMeters ?: 0 }
val maxSpeedKmh = activities.mapNotNull { it.maxSpeedKmh }.maxOrNull()
val maxRiderPowerWatts = activities.mapNotNull { it.maxRiderPowerWatts }.maxOrNull()
val totalCaloriesBurned = activities.mapNotNull { it.caloriesBurned }
    .takeIf { it.isNotEmpty() }?.sum()
```

**Effizienz — effektive Reisegeschwindigkeit:**

```kotlin
val totalDistanceKm = activities.sumOf { it.distanceMeters } / 1000.0
val totalDurationHours = activities.sumOf { it.durationWithoutStopsSeconds } / 3600.0
val avgTravelSpeedKmh = if (totalDurationHours > 0) totalDistanceKm / totalDurationHours else null
```

**Rhythmus — Wochentag-Verteilung:**

```kotlin
val dayOfWeekDistribution: Map<DayOfWeek, Int> = activities
    .mapNotNull { it.startTime.toLocalDate(zoneId)?.dayOfWeek }
    .groupingBy { it }
    .eachCount()

val favoriteDayOfWeek: DayOfWeek? = dayOfWeekDistribution
    .maxByOrNull { it.value }?.key
```

**Rhythmus — Wochen-Frequenz-Histogram:**

Zählt, wie viele Kalenderwochen (ISO) es mit 0, 1, 2 … Touren gab.

```kotlin
// Alle Touren ihren ISO-Wochen-Startdaten zuordnen
val toursPerWeek: Map<LocalDate, Int> = activities
    .mapNotNull { it.startTime.toLocalDate(zoneId)?.toPeriodStart(StatisticsGrouping.WEEK, locale) }
    .groupingBy { it }
    .eachCount()

// Lückenwochen (0 Touren) auffüllen — nur innerhalb des Aktivitätszeitraums
val firstWeek = toursPerWeek.keys.minOrNull()
val lastWeek = toursPerWeek.keys.maxOrNull()
val allWeeks: List<LocalDate> = generateSequence(firstWeek) { it.plusWeeks(1) }
    .takeWhile { !it.isAfter(lastWeek!!) }
    .toList()

val weeklyFrequencyHistogram: Map<Int, Int> = allWeeks
    .map { week -> toursPerWeek[week] ?: 0 }
    .groupingBy { it }
    .eachCount()
    .toSortedMap()

// Aktivitäts-Quote
val activeWeeksRatio: Double? = if (allWeeks.size >= 2) {
    toursPerWeek.size.toDouble() / allWeeks.size
} else null
```

### Anpassung `StatisticsViewModel`

In der `combine`-Pipeline wird `mapHighlights` aufgerufen und in `toUiState` eingebaut:

```kotlin
val highlights = uiModelMapper.mapHighlights(activities)
// in toUiState():
highlights = highlights,
```

---

## Schicht: UI

### Aufbau der neuen Sektion

`StatisticsHighlightsSection` wird als eigenes `@Composable` in `StatisticsScreen.kt` ergänzt.
Es erscheint als letztes `item { }` in der `LazyColumn`, nach dem Detail-Card.
`AnimatedVisibility` wrappen — wird nur gezeigt wenn `highlights != null`.

Sektion ist in **drei thematische Blöcke** unterteilt, jeweils als eigene `Card`:

---

#### Block A — Bestleistungen (`StatisticsPersonalBestsCard`)

2-spaltige Grid-Anordnung mit `StatisticsMetricTile`:

| Tile | Wert | Immer sichtbar? |
|---|---|---|
| Längste Tour | `"42.3 km"` | ja |
| Gesamte Höhenmeter | `"1 234 m"` | ja (0 m wenn keine Daten) |
| Top-Geschwindigkeit | `"38.4 km/h"` | nur wenn ≥ 1 Aktivität mit Wert |
| Max. Eigenleistung | `"312 W"` | nur wenn ≥ 1 Aktivität mit Wert |
| Verbrauchte Kalorien | `"8 540 kcal"` | nur wenn ≥ 1 Aktivität mit Wert |

Nullable Tiles werden per `if (highlights.maxSpeedKmh != null)` bedingt gerendert — kein Leer-Placeholder.

Formatierungen:
- km: `"%.1f km"` (Locale.getDefault())
- m: `"%,d m"` — Tausender-Trenner für > 999 m
- km/h: `"%.1f km/h"`
- W: `"%d W"`
- kcal: `"%,.0f kcal"`

---

#### Block B — Effizienz (`StatisticsEfficiencyCard`)

Einzeiliges Statement-Format statt Tiles:

```
Effektive Reisegeschwindigkeit
  ⌀ 18.4 km/h  (Strecke ÷ Fahrzeit ohne Pausen)
```

Als `Row` mit großem Wert-Text (`titleLarge`, `FontWeight.Bold`) links und erläuterndem
`bodySmall`-Text darunter. Wird nur gezeigt wenn `highlights.avgTravelSpeedKmh != null`.

---

#### Block C — Rhythmus (`StatisticsRhythmCard`)

**Statement-Zeile:**

```
Dein aktivster Tag: Samstag (23 Touren)
```

Als farblich hervorgehobener Text: Label `onSurfaceVariant`, Wochentag `primary + SemiBold`, Anzahl `onSurface`.

**Wochentag-Mini-Bar:**

7 Spalten (Mo–So) als `Row` mit `weight(1f)` pro Spalte. Jede Spalte zeigt:
- Balken: `Box` mit `fillMaxWidth()` und Höhe proportional zum Maximum, gefärbt mit `primary.copy(alpha = ...)`
- Favoritentag bekommt vollen `primary`-Alpha, andere skaliert
- Darunter: 2-stellige Kurzbezeichnung (`"Mo"`, `"Di"` … oder `"Mon"`, `"Tue"`)
- Ganz unten: Tourenzahl als kleine Zahl

Höhe der gesamten Mini-Bar: 64 dp. Kein Vico, rein Compose.

**Wochen-Frequenz-Tabelle:**

Kompakte `Column` mit Zeilen für jeden `(toursPerWeek, weekCount)`-Eintrag:

```
0 Touren/Woche  → 4 Wochen
1 Tour/Woche    → 11 Wochen
2 Touren/Woche  →  7 Wochen
3+ Touren/Woche →  2 Wochen
```

Zeilen mit `toursPerWeek >= 3` werden zusammengefasst zu `"3+ Touren/Woche"` (Overflow-Schutz).
Jede Zeile als `Row` mit `Spacer(Modifier.weight(1f))` zwischen Label und Wert.
Die Zeile mit dem häufigsten Wert wird mit `surfaceContainerHigh`-Hintergrund hervorgehoben.

**Aktivitäts-Quote:**

Nur wenn `highlights.activeWeeksRatio != null`:

```
Aktiv in 73 % aller Wochen
```

Als `LinearProgressIndicator` (0..1) mit Wert-Text daneben. Farbe: `primary`.

---

### String-Ressourcen (neu)

**`res/values/strings.xml`:**

```xml
<!-- Highlights Section -->
<string name="statistics_highlights_section">Highlights &amp; Rhythmus</string>
<string name="statistics_highlights_personal_bests">Bestleistungen</string>
<string name="statistics_highlights_longest_tour">Längste Tour</string>
<string name="statistics_highlights_total_elevation">Gesamthöhenmeter</string>
<string name="statistics_highlights_max_speed">Top-Geschwindigkeit</string>
<string name="statistics_highlights_max_power">Max. Eigenleistung</string>
<string name="statistics_highlights_total_calories">Kalorien gesamt</string>
<string name="statistics_highlights_efficiency">Effizienz</string>
<string name="statistics_highlights_avg_speed">Effektive Reisegeschw.</string>
<string name="statistics_highlights_avg_speed_hint">Strecke ÷ Fahrzeit ohne Pausen</string>
<string name="statistics_highlights_rhythm">Rhythmus</string>
<string name="statistics_highlights_favorite_day">Aktivster Tag: %1$s (%2$d Touren)</string>
<string name="statistics_highlights_active_weeks">Aktiv in %1$d\u2009%% aller Wochen</string>
<string name="statistics_highlights_freq_row_zero">0 Touren / Woche</string>
<string name="statistics_highlights_freq_row_one">1 Tour / Woche</string>
<string name="statistics_highlights_freq_row_n">%1$d Touren / Woche</string>
<string name="statistics_highlights_freq_row_overflow">%1$d+ Touren / Woche</string>
<string name="statistics_highlights_freq_weeks">%1$d Wochen</string>
```

**`res/values-de/strings.xml`:** analog Deutsch — Wochentagnamen kommen aus `DayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())`, daher keine statischen String-Ressourcen nötig.

---

## Tests

### `StatisticsUiModelMapperTest` (Erweiterung)

Neue Testfälle für `mapHighlights()`:

| Testfall | Was geprüft wird |
|---|---|
| Leere Liste | Rückgabe `null` |
| Alle nullable Felder null | `maxSpeedKmh == null`, `totalCaloriesBurned == null` |
| Einzelne Aktivität | `activeWeeksRatio == null` (< 2 Wochen), `favoriteDayOfWeek` korrekt |
| Zwei Aktivitäten, selber Wochentag | `favoriteDayOfWeek` = dieser Tag, `activeWeeksRatio` berechnet |
| Aktivitäten über 3 Wochen, eine Woche leer | Histogram enthält `0 → 1`, Quote = 2/3 |
| Wochentag-Verteilung Mo+Sa dominiert | `favoriteDayOfWeek` = Sa bei Sa > Mo |
| `avgTravelSpeedKmh` bei 0 Dauer | `null`, kein Division-by-zero |

---

## Reihenfolge der Umsetzung

| # | Schritt | Datei(en) |
|---|---------|-----------|
| 1 | `StatisticsHighlights` Datenklasse ergänzen | `StatisticsUiState.kt` |
| 2 | `mapHighlights()` im Mapper implementieren | `StatisticsUiModelMapper.kt` |
| 3 | ViewModel-Pipeline um `highlights` erweitern | `StatisticsViewModel.kt` |
| 4 | String-Ressourcen ergänzen (EN + DE) | `strings.xml`, evtl. `strings-de.xml` |
| 5 | `StatisticsPersonalBestsCard` implementieren | `StatisticsScreen.kt` |
| 6 | `StatisticsEfficiencyCard` implementieren | `StatisticsScreen.kt` |
| 7 | `StatisticsRhythmCard` implementieren (Statement + Mini-Bar + Tabelle + Quote) | `StatisticsScreen.kt` |
| 8 | Tests für `mapHighlights()` schreiben | `StatisticsUiModelMapperTest.kt` |

Schritte 1–3 sind ein Commit (Datenmodell + Logik), Schritte 4–7 ein zweiter (UI), Schritt 8 kann direkt nach Schritt 2 erfolgen (TDD-Stil möglich).

---

## Nicht im Plan

- Vergleich mit Vorjahr / Vormonat (fehlen historische Referenzpunkte im Cache-Modell)
- Höhenprofile oder Track-Metriken (kommen aus `BoschActivityDetail`, nicht aus `BoschActivity`)
- Push-Notifications für Bestleistungen
- Persistierung der Highlights (sie sind deterministisch aus dem Cache berechenbar, kein eigener Store nötig)
