# Plan: Aktivitäts-Heatmap & Clustering (Weltkarte)

**Stand:** 2026-04-06
**Status:** Bereit zur Umsetzung
**Ziel:** Performante, interaktive Weltkarte, die alle bisherigen Touren als Heatmap/Cluster visualisiert. Basis sind geometrische Mittelpunkte der Tracks (Bounding-Box-Zentrum), da >90 % der Touren vom selben Startpunkt stammen.

---

## Warum dieser Ansatz?

| Problem | Lösung |
|---|---|
| Alle Startpunkte identisch → Karte zeigt nur 1 Punkt | Bounding-Box-Zentrum statt Startpunkt |
| 1,5 Mio GPS-Punkte = OOM auf Mobile | 400 Koordinaten-Paare aus Room-Query |
| Punktewolke unleserlich | MapLibre Clustering + Heatmap-Layer |

---

## Out of Scope (nicht anfassen)

- Kein Live-Tracking
- Keine Linien / Track-Pfade in der Weltkarte
- Keine Offline-Tiles
- Kein neuer Bosch-Cloud-Call nur für die Karte

---

## Schritt-für-Schritt Implementierung

> **Reihenfolge einhalten.** Jede Phase baut auf der vorherigen auf. Nichts überspringen.

---

### Phase 1 — Room: Schema-Migration (version 6 → 7)

**Ziel:** `ActivityEntity` bekommt zwei neue nullable Felder.

#### 1.1 `ActivityEntity.kt` erweitern

Datei: `data/local/entity/ActivityEntity.kt`

Zwei Felder am Ende der `data class` ergänzen:

```kotlin
val centerLatitude: Double? = null,
val centerLongitude: Double? = null,
```

> **Warum nullable?** Die meisten bestehenden Touren haben noch kein berechnetes Zentrum. NULL = "noch nicht berechnet".

#### 1.2 Migration schreiben

Datei: `data/local/database/BoschDatabaseMigrations.kt`

Neues Objekt `MIGRATION_6_7` ergänzen und in `ALL` aufnehmen:

```kotlin
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `activities` ADD COLUMN `centerLatitude` REAL")
        db.execSQL("ALTER TABLE `activities` ADD COLUMN `centerLongitude` REAL")
    }
}

val ALL = arrayOf(
    MIGRATION_2_3,
    MIGRATION_3_4,
    MIGRATION_4_5,
    MIGRATION_5_6,
    MIGRATION_6_7, // neu
)
```

> `ALTER TABLE ADD COLUMN` mit nullable REAL ist in SQLite immer sicher. Kein Table-Rebuild nötig.

#### 1.3 Datenbankversion hochsetzen

Datei: `data/local/database/BoschDatabase.kt`

`version = 6` → `version = 7`

#### 1.4 Schema-Snapshot exportieren

```bash
./gradlew generateRoomSchemas
```

Prüfen: `app/schemas/info.meuse24.m24bikestats.data.local.database.BoschDatabase/7.json` muss existieren.

---

### Phase 2 — Data Layer: Zentrum berechnen und speichern

**Ziel:** Sobald Detail-Punkte gespeichert werden, wird das Zentrum berechnet und in `activities` geschrieben.

#### 2.1 DAO: Update-Methode ergänzen

Datei: `data/local/dao/ActivityDao.kt`

```kotlin
@Query("""
    UPDATE activities
    SET centerLatitude = :lat, centerLongitude = :lng
    WHERE id = :activityId
""")
suspend fun updateCenter(activityId: String, lat: Double, lng: Double)

@Query("""
    SELECT id
    FROM activities
    WHERE centerLatitude IS NULL
""")
suspend fun getIdsWithoutCenter(): List<String>
```

#### 2.2 Hilfsklasse: Zentrum berechnen

Neue Datei: `data/local/mapper/ActivityCenterCalculator.kt`

```kotlin
package info.meuse24.m24bikestats.data.local.mapper

object ActivityCenterCalculator {

    /**
     * Gibt den geometrischen Mittelpunkt der Bounding Box zurück,
     * oder null wenn keine GPS-Punkte vorhanden sind.
     */
    fun calculate(
        points: List<Pair<Double, Double>> // (lat, lng)
    ): Pair<Double, Double>? {
        val valid = points.filter { (lat, lng) ->
            lat != 0.0 && lng != 0.0
        }
        if (valid.isEmpty()) return null
        val minLat = valid.minOf { it.first }
        val maxLat = valid.maxOf { it.first }
        val minLng = valid.minOf { it.second }
        val maxLng = valid.maxOf { it.second }
        return Pair((minLat + maxLat) / 2.0, (minLng + maxLng) / 2.0)
    }
}
```

> Android-frei, reine Kotlin-Logik → gut testbar.

#### 2.3 Zentrum nach Detail-Sync schreiben

Datei: `data/repository/BoschSmartSystemRepositoryImpl.kt` (oder wo Detail-Punkte in Room gespeichert werden — dort suchen, wo `activityDetailDao` aufgerufen wird nach dem Insert der Punkte)

Nach dem Insert der `ActivityDetailPointEntity`-Liste die Berechnung aufrufen:

```kotlin
// nach activityDetailDao.upsertPoints(points)
val gpsCoords = points
    .filter { it.latitude != null && it.longitude != null }
    .map { it.latitude!! to it.longitude!! }
val center = ActivityCenterCalculator.calculate(gpsCoords)
if (center != null) {
    activityDao.updateCenter(activityId, center.first, center.second)
}
```

#### 2.4 Mapper aktualisieren

Datei: `data/local/mapper/ActivityEntityMapper.kt`

In `toDomain()` und `toEntity()` die neuen Felder hinzufügen:

```kotlin
// toDomain()
centerLatitude = centerLatitude,
centerLongitude = centerLongitude,

// toEntity()
centerLatitude = centerLatitude,
centerLongitude = centerLongitude,
```

---

### Phase 3 — Domain Layer

**Ziel:** `BoschActivity` kennt das Zentrum. Ein schlanker UseCase liefert nur die Kartenpunkte.

#### 3.1 `BoschActivity.kt` erweitern

```kotlin
val centerLatitude: Double? = null,
val centerLongitude: Double? = null,
```

#### 3.2 Domain-Modell: `ActivityMapPoint`

Neue Datei: `domain/model/ActivityMapPoint.kt`

```kotlin
package info.meuse24.m24bikestats.domain.model

data class ActivityMapPoint(
    val activityId: String,
    val latitude: Double,
    val longitude: Double,
)
```

> Absichtlich schlank. Kein Titel, kein Datum — MapLibre braucht nur Koordinaten für Heatmap/Cluster.

#### 3.3 UseCase: `GetActivityMapPointsUseCase`

Neue Datei: `domain/usecase/GetActivityMapPointsUseCase.kt`

```kotlin
package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.model.ActivityMapPoint
import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetActivityMapPointsUseCase(
    private val repository: BoschSmartSystemRepository,
) {
    operator fun invoke(): Flow<List<ActivityMapPoint>> =
        repository.observeAllActivities().map { activities ->
            activities.mapNotNull { activity ->
                val lat = activity.centerLatitude ?: return@mapNotNull null
                val lng = activity.centerLongitude ?: return@mapNotNull null
                ActivityMapPoint(activity.id, lat, lng)
            }
        }
}
```

> `Flow` statt `suspend` — der Screen reagiert automatisch, wenn neue Touren synchronisiert werden.

---

### Phase 4 — Legacy-Worker: Bestehende 400 Touren nachladen

**Ziel:** Einmalig für alle Touren ohne Zentrum das Zentrum aus `activity_detail_points` berechnen.

#### 4.1 Worker erstellen

Neue Datei: `background/ComputeActivityCentersWorker.kt`

```kotlin
package info.meuse24.m24bikestats.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import info.meuse24.m24bikestats.data.local.dao.ActivityDao
import info.meuse24.m24bikestats.data.local.dao.ActivityDetailDao
import info.meuse24.m24bikestats.data.local.mapper.ActivityCenterCalculator
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ComputeActivityCentersWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val activityDao: ActivityDao by inject()
    private val activityDetailDao: ActivityDetailDao by inject()

    override suspend fun doWork(): Result {
        val ids = activityDao.getIdsWithoutCenter()
        for (id in ids) {
            val points = activityDetailDao.getGpsPointsForActivity(id)
            val center = ActivityCenterCalculator.calculate(points)
            if (center != null) {
                activityDao.updateCenter(id, center.first, center.second)
            }
        }
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "compute_activity_centers_once"
    }
}
```

#### 4.2 Neue DAO-Methode für GPS-Punkte

Datei: `data/local/dao/ActivityDetailDao.kt`

```kotlin
@Query("""
    SELECT latitude, longitude
    FROM activity_detail_points
    WHERE activityId = :activityId
      AND latitude IS NOT NULL
      AND longitude IS NOT NULL
""")
suspend fun getGpsPointsForActivity(activityId: String): List<GpsPointProjection>
```

Neue Projection-Klasse (gleiche Datei oder eigene Datei in `data/local/model/`):

```kotlin
data class GpsPointProjection(
    val latitude: Double,
    val longitude: Double,
)
```

Im Worker dann:
```kotlin
val points = activityDetailDao.getGpsPointsForActivity(id)
    .map { it.latitude to it.longitude }
```

#### 4.3 Worker einmalig starten

Datei: `background/BackgroundSyncScheduler.kt` oder in `AppModule.kt` beim App-Start.

Empfehlung: In `AppModule.kt` nach dem DI-Setup:

```kotlin
// Einmalig ausführen — WorkManager verhindert doppelten Start bei KEEP-Policy
WorkManager.getInstance(androidContext())
    .enqueueUniqueWork(
        ComputeActivityCentersWorker.WORK_NAME,
        ExistingWorkPolicy.KEEP,
        OneTimeWorkRequestBuilder<ComputeActivityCentersWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()
    )
```

> `KEEP` = wenn der Worker läuft oder schon gelaufen ist, nichts tun. Sicher bei App-Neustart.

---

### Phase 5 — Presentation Layer: ViewModel & UiState

**Ziel:** Saubere Datenpipeline für den Map-Screen.

#### 5.1 UiState

Neue Datei: `presentation/map/MapSummaryUiState.kt`

```kotlin
package info.meuse24.m24bikestats.presentation.map

import info.meuse24.m24bikestats.domain.model.ActivityMapPoint

data class MapSummaryUiState(
    val points: List<ActivityMapPoint> = emptyList(),
    val isLoading: Boolean = true,
)
```

#### 5.2 GeoJSON-Mapper

Neue Datei: `presentation/map/ActivityMapPointGeoJsonMapper.kt`

```kotlin
package info.meuse24.m24bikestats.presentation.map

import info.meuse24.m24bikestats.domain.model.ActivityMapPoint
import org.json.JSONArray
import org.json.JSONObject

object ActivityMapPointGeoJsonMapper {

    fun toGeoJsonString(points: List<ActivityMapPoint>): String {
        val features = JSONArray()
        for (point in points) {
            val geometry = JSONObject().apply {
                put("type", "Point")
                put("coordinates", JSONArray().apply {
                    put(point.longitude)  // GeoJSON: [lng, lat]
                    put(point.latitude)
                })
            }
            val feature = JSONObject().apply {
                put("type", "Feature")
                put("geometry", geometry)
                put("properties", JSONObject().apply {
                    put("activityId", point.activityId)
                })
            }
            features.put(feature)
        }
        return JSONObject().apply {
            put("type", "FeatureCollection")
            put("features", features)
        }.toString()
    }
}
```

> Kein Gson/Moshi nötig. `org.json` ist in Android ohne zusätzliche Dependency verfügbar. GeoJSON-Koordinaten sind `[longitude, latitude]` — nicht vertauschen!

#### 5.3 ViewModel

Neue Datei: `presentation/map/MapSummaryViewModel.kt`

```kotlin
package info.meuse24.m24bikestats.presentation.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.meuse24.m24bikestats.domain.usecase.GetActivityMapPointsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class MapSummaryViewModel(
    getMapPoints: GetActivityMapPointsUseCase,
) : ViewModel() {

    val uiState = getMapPoints()
        .map { points ->
            MapSummaryUiState(
                points = points,
                isLoading = false,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MapSummaryUiState(),
        )
}
```

---

### Phase 6 — UI: MapSummaryScreen

**Ziel:** Compose-Screen mit MapLibre, Heatmap- und Cluster-Layer.

Neue Datei: `presentation/map/MapSummaryScreen.kt`

Aufbau:

```kotlin
@Composable
fun MapSummaryScreen(
    uiState: MapSummaryUiState,
    onBackClick: () -> Unit,
) {
    // AndroidView mit MapLibre MapView
    // Wenn uiState.isLoading → CircularProgressIndicator
    // Wenn points leer → Hinweistext "Noch keine synchronisierten Touren mit GPS-Daten"
    // Sonst: GeoJSON-Source + Layer aufbauen
}
```

MapLibre Layer-Aufbau (in `onMapReady`-Callback):

```
1. GeoJsonSource("activity-source")
   → cluster = true
   → clusterRadius = 50
   → clusterMaxZoom = 14

2. HeatmapLayer("heatmap-layer", "activity-source")
   → Farb-Ramp: blau (kalt) → gelb → rot (heiß)
   → maxZoom = 9 (verschwindet beim Reinzoomen)

3. CircleLayer("cluster-circle", "activity-source")
   → filter: ["has", "point_count"]
   → Radius nach point_count gestaffelt

4. SymbolLayer("cluster-count", "activity-source")
   → textField: "{point_count_abbreviated}"

5. CircleLayer("single-point", "activity-source")
   → filter: ["!", ["has", "point_count"]]
   → einzelne Punkte als kleine Kreise
```

Auto-Fit beim Laden:
```kotlin
// CameraUpdateFactory.newLatLngBounds(bounds, padding = 64)
// bounds aus minLat/maxLat/minLng/maxLng der points-Liste
```

> MapLibre ist bereits im Projekt (TrackScreen). Keine neue Dependency.

---

### Phase 7 — DI & Navigation

#### 7.1 Koin: AppModule erweitern

Datei: `di/AppModule.kt`

```kotlin
// UseCase
single { GetActivityMapPointsUseCase(get()) }

// ViewModel
viewModel { MapSummaryViewModel(get()) }
```

#### 7.2 Navigation: Route hinzufügen

Datei: `presentation/navigation/model/MainDestination.kt` (oder wo Routen definiert sind)

```kotlin
object MapSummary : MainDestination("map_summary")
```

Datei: `presentation/navigation/AppNavigation.kt`

```kotlin
composable(MainDestination.MapSummary.route) {
    val viewModel: MapSummaryViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MapSummaryScreen(
        uiState = uiState,
        onBackClick = { navController.popBackStack() },
    )
}
```

#### 7.3 Einstiegspunkt in UI

Empfehlung: Icon/Button im `StatisticsScreen` (unten) oder `FunctionsScreen`.

Kein neuer Drawer-Eintrag — die Karte ist ein Feature, kein primärer Navigationsbereich.

---

## Dateiübersicht: Was wird angefasst / neu erstellt?

### Neu erstellen

| Datei | Typ |
|---|---|
| `data/local/mapper/ActivityCenterCalculator.kt` | Hilfsklasse, Android-frei |
| `data/local/model/GpsPointProjection.kt` | Room-Projection |
| `domain/model/ActivityMapPoint.kt` | Domain-Modell |
| `domain/usecase/GetActivityMapPointsUseCase.kt` | UseCase |
| `background/ComputeActivityCentersWorker.kt` | WorkManager Worker |
| `presentation/map/MapSummaryUiState.kt` | UiState |
| `presentation/map/ActivityMapPointGeoJsonMapper.kt` | Mapper |
| `presentation/map/MapSummaryViewModel.kt` | ViewModel |
| `presentation/map/MapSummaryScreen.kt` | Compose Screen |

### Anfassen (bestehende Dateien)

| Datei | Änderung |
|---|---|
| `data/local/entity/ActivityEntity.kt` | 2 Felder ergänzen |
| `data/local/database/BoschDatabase.kt` | version 6 → 7 |
| `data/local/database/BoschDatabaseMigrations.kt` | MIGRATION_6_7 + ALL |
| `data/local/dao/ActivityDao.kt` | `updateCenter`, `getIdsWithoutCenter` |
| `data/local/dao/ActivityDetailDao.kt` | `getGpsPointsForActivity` |
| `data/local/mapper/ActivityEntityMapper.kt` | neue Felder mitmappen |
| `domain/model/BoschActivity.kt` | 2 Felder ergänzen |
| `data/repository/BoschSmartSystemRepositoryImpl.kt` | Zentrum nach Detail-Insert berechnen |
| `di/AppModule.kt` | UseCase + ViewModel binden + Worker starten |
| `presentation/navigation/AppNavigation.kt` | Route + composable |
| `presentation/navigation/model/MainDestination.kt` | neue Route |
| `presentation/statistics/StatisticsScreen.kt` oder `FunctionsScreen.kt` | Einstiegs-Button |

---

## Tests

| Test | Datei |
|---|---|
| `ActivityCenterCalculator`: leere Liste, alle null, normale Liste, Pole-Grenzfall | `test/.../data/local/mapper/ActivityCenterCalculatorTest.kt` |
| `ActivityMapPointGeoJsonMapper`: leere Liste, Koordinaten-Reihenfolge (lng vor lat!) | `test/.../presentation/map/ActivityMapPointGeoJsonMapperTest.kt` |
| `GetActivityMapPointsUseCase`: null-Koordinaten werden gefiltert | `test/.../domain/usecase/GetActivityMapPointsUseCaseTest.kt` |

---

## Reihenfolge der Commits

1. `Room migration 6→7: add centerLatitude/centerLongitude to activities`
2. `Data: ActivityCenterCalculator + DAO + update on detail-sync`
3. `Domain: ActivityMapPoint model + GetActivityMapPointsUseCase`
4. `Background: ComputeActivityCentersWorker for legacy data`
5. `Presentation: MapSummaryScreen with heatmap and clustering`
6. `Navigation + DI: wire up MapSummaryScreen`
7. Tests

---

*Plan v2 — 2026-04-06*
