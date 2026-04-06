# Implementierungsplan: Statistik-Diagramm verbessern

Stand: 2026-04-06

## Ziel

Das bestehende Vico-Kombi-Diagramm (`StatisticsChartCard`) robuster, informativer und sauberer machen.
Keine neuen Datenspalten oder Serien — nur Verbesserungen an bestehendem Code und sinnvolle UX-Ergänzungen.

---

## Schritt 1 — Bug: Index-Lookup in `dataLabelValueFormatter` fixieren

**Datei:** `StatisticsScreen.kt`  
**Problem:** `distances.indexOf(value)` sucht den Distanzwert linear per Wert. Bei identischen Distanzwerten (z.B. zwei Perioden mit exakt 42.0 km) gibt `indexOf` immer den ersten Treffer zurück — die Tour-Anzahl wird dann für den falschen Balken angezeigt.  
**Ursache:** Vico übergibt `value` als Y-Wert der Kolumne, aber `x` trägt den Index.  
**Fix:** Den `CartesianValueFormatter`-Lambda-Parameter `value` durch den `x`-basierten Index ersetzen.

```kotlin
// Vorher (fehlerhaft)
CartesianValueFormatter { context, value, _ ->
    val distances = extras.getOrNull(statisticsDistanceValuesKey) ?: return@CartesianValueFormatter ""
    val tourCounts = extras.getOrNull(statisticsTourCountsKey) ?: return@CartesianValueFormatter ""
    tourCounts.getOrNull(distances.indexOf(value))?.toString().orEmpty()
}

// Nachher (korrekt)
CartesianValueFormatter { context, value, _ ->
    val tourCounts = context.model.extraStore.getOrNull(statisticsTourCountsKey)
        ?: return@CartesianValueFormatter ""
    tourCounts.getOrNull(value.toInt())?.toString().orEmpty()
}
```

Damit wird `statisticsDistanceValuesKey` im `ExtraStore` obsolet und kann entfernt werden.

**Betroffene Dateien:** `StatisticsScreen.kt`

---

## Schritt 2 — Threshold-Konstanten bereinigen

**Datei:** `StatisticsScreen.kt`  
**Problem:** Die Konstanten `STATISTICS_SCROLL_THRESHOLD = 7` und `STATISTICS_ZOOM_THRESHOLD = 10` werden im Code jeweils mit `* 2` multipliziert. Der eigentliche Schwellwert steht also nicht in der Konstante. Das ist irreführend.  
**Fix:** Konstanten auf die tatsächlich genutzten Werte setzen und die Multiplikation entfernen.

```kotlin
// Vorher
private const val STATISTICS_SCROLL_THRESHOLD = 7
private const val STATISTICS_ZOOM_THRESHOLD = 10
// ...
val scrollThreshold = STATISTICS_SCROLL_THRESHOLD * 2  // = 14
val zoomThreshold = STATISTICS_ZOOM_THRESHOLD * 2      // = 20

// Nachher
private const val STATISTICS_SCROLL_THRESHOLD = 14
private const val STATISTICS_ZOOM_THRESHOLD = 20
// ...
val scrollThreshold = STATISTICS_SCROLL_THRESHOLD
val zoomThreshold = STATISTICS_ZOOM_THRESHOLD
```

**Betroffene Dateien:** `StatisticsScreen.kt`

---

## Schritt 3 — Y-Achsen mit Einheiten-Suffix

**Datei:** `StatisticsScreen.kt`  
**Problem:** Start-Achse zeigt `"125"`, End-Achse zeigt `"2"` — ohne Einheit ist die Bedeutung ohne Legende nicht erkennbar.  
**Fix:** Einheiten-Suffix in den `CartesianValueFormatter` integrieren.

```kotlin
// Start-Achse (Distanz)
valueFormatter = remember {
    CartesianValueFormatter { _, value, _ -> "${value.roundToInt()} km" }
}

// End-Achse (Fahrtzeit)
valueFormatter = remember {
    CartesianValueFormatter { _, value, _ -> "${value.roundToInt()} h" }
}
```

**Betroffene Dateien:** `StatisticsScreen.kt`

---

## Schritt 4 — DataLabel-Padding für Tour-Anzahl explizit setzen

**Datei:** `StatisticsScreen.kt`  
**Problem:** Das `rememberTextComponent` für `dataLabel` hat kein `padding`. Das Label sitzt dadurch eng am Hintergrund-Shape.  
**Fix:** Explizites Padding ergänzen.

```kotlin
dataLabel = rememberTextComponent(
    color = MaterialTheme.colorScheme.onSurface,
    textSize = 11.sp,
    padding = Insets(4f, 2f, 4f, 2f),
    background = rememberShapeComponent(
        shape = CorneredShape.rounded(allPercent = 50),
        fill = fill(MaterialTheme.colorScheme.surfaceContainer),
    ),
),
```

**Betroffene Dateien:** `StatisticsScreen.kt`

---

## Schritt 5 — Format-Extensions und `durationHours` aus dem Screen herauslösen

**Problem:** `toReadableDistance()`, `toReadableHours()` und `durationHours` sind `private` Extensions in `StatisticsScreen.kt`. Sie können daher nicht getestet werden und erzeugen Locale-abhängige Ausgaben ohne Testabdeckung.  
**Fix:** Die Extensions in `PeriodStats` einarbeiten oder nach `StatisticsUiState.kt` verschieben (package-internal, nicht `private`). `durationHours` wird direkt als Property in `PeriodStats` ergänzt.

```kotlin
// StatisticsUiState.kt — Ergänzungen

val PeriodStats.durationHours: Double
    get() = durationMinutes / 60.0

fun Double.toReadableDistance(): String =
    String.format(Locale.getDefault(), "%.1f km", this)

fun Double.toReadableHours(): String =
    String.format(Locale.getDefault(), "%.1f h", this)
```

In `StatisticsScreen.kt` den `private`-Block entfernen; die Extensions sind jetzt aus dem Screen erreichbar, da selbes Package.

**Betroffene Dateien:** `StatisticsUiState.kt`, `StatisticsScreen.kt`

---

## Schritt 6 — Durchschnittliche Fahrtdauer als zweite Dekoration

**Datei:** `StatisticsScreen.kt`  
**Problem:** Es gibt eine `HorizontalLine`-Dekoration für die Durchschnitts-Distanz auf der Start-Achse. Eine symmetrische Linie für die Durchschnitts-Fahrtdauer auf der End-Achse fehlt.  
**Fix:** Analog zur bestehenden Distanz-Linie eine Dauer-Linie ergänzen.

```kotlin
val averageDurationHours = remember(periods) {
    periods.takeIf { it.size > 1 }
        ?.map { it.durationHours }
        ?.average()
        ?.takeIf { it > 0.0 }
}

// In decorations-Block ergänzen:
averageDurationHours?.let { avgDuration ->
    HorizontalLine(
        y = { avgDuration },
        line = rememberLineComponent(
            fill = fill(durationColor.copy(alpha = 0.35f)),
            thickness = 1.dp,
            shape = DashedShape(dashLengthDp = 6f, gapLengthDp = 6f),
        ),
        labelComponent = averageDurationLabelComponent,
        label = { stringResource(R.string.statistics_average_duration_label, avgDuration.roundToInt()) },
        horizontalLabelPosition = Position.Horizontal.Start,
        verticalLabelPosition = Position.Vertical.Top,
        verticalAxisPosition = Axis.Position.Vertical.End,
    )
}
```

Neuer String-Ressource: `statistics_average_duration_label` (DE: `"Ø %d h"`, EN: `"avg %d h"`).

**Betroffene Dateien:** `StatisticsScreen.kt`, `res/values/strings.xml`, `res/values-de/strings.xml`

---

## Schritt 7 — Durchschnittswerte pro Tour in Summary-Tiles

**Dateien:** `StatisticsUiState.kt`, `StatisticsViewModel.kt`, `StatisticsScreen.kt`, `strings.xml`  
**Problem:** Die Summary-Tiles zeigen nur Gesamtwerte. Für Fahrer ist der Durchschnitt je Tour (avg km/Tour, avg h/Tour) mindestens genauso relevant.  
**Fix:** Zwei neue Properties im `StatisticsUiState` ergänzen.

```kotlin
// StatisticsUiState.kt
data class StatisticsUiState(
    // ... bestehende Felder ...
    val avgDistanceKm: Double = 0.0,
    val avgDurationHours: Double = 0.0,
)
```

Im ViewModel berechnen:

```kotlin
// StatisticsViewModel.kt — in toUiState()
avgDistanceKm = if (size > 0) sumOf { it.distanceMeters } / 1000.0 / size else 0.0,
avgDurationHours = if (size > 0) sumOf { it.durationWithoutStopsSeconds }.toDouble() / 3600.0 / size else 0.0,
```

Im Screen eine zweite `StatisticsSummaryRow` oder erweiterbare Tiles ergänzen. Alternativ: eine zweite Zeile mit den Avg-Tiles unterhalb der ersten.

Neue String-Ressourcen: `statistics_summary_avg_distance`, `statistics_summary_avg_duration`.

**Betroffene Dateien:** `StatisticsUiState.kt`, `StatisticsViewModel.kt`, `StatisticsScreen.kt`, `strings.xml`, `strings-de.xml` (falls vorhanden)

---

## Reihenfolge der Umsetzung

| # | Schritt | Typ | Risiko |
|---|---------|-----|--------|
| 1 | Bug: Index-Lookup | Bug-Fix | niedrig |
| 2 | Threshold-Konstanten | Refactoring | trivial |
| 3 | Y-Achsen Einheiten | Visual | trivial |
| 4 | DataLabel-Padding | Visual | trivial |
| 5 | Extensions herauslösen | Refactoring | niedrig |
| 6 | Ø-Dauer-Dekoration | Feature | niedrig |
| 7 | Avg-Tiles | Feature | mittel |

Schritte 1–5 können in einem Commit zusammengefasst werden (Bugfix + Cleanup).  
Schritte 6–7 jeweils als eigener Commit (Features).

---

## Nicht im Plan

- Neue Datenserien (Höhenmeter, Geschwindigkeit) — fehlen in `BoschActivity`
- Gradient-Fill für Balken — Vico 2.x unterstützt das über `ShaderProvider`, erhöht Komplexität ohne klaren Mehrwert
- Responsive Chart-Höhe — 280dp ist für das aktuelle Layout ausreichend
- Marker-Redesign — bestehender Marker ist funktional und klar
