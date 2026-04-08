# Plan: Datenzustand & Sync

Stand: 2026-04-08

## Ziel

Die App soll für Nutzer klarer sichtbar machen:

- welche Bosch-Daten bereits lokal vorliegen
- welche Daten noch fehlen
- welche Detaildaten veraltet sein könnten
- wann zuletzt erfolgreich synchronisiert wurde
- welche gezielten Nachlade-Aktionen sinnvoll sind

Die Verbesserung soll echten Nutzwert bringen, ohne die App künstlich zu verkomplizieren.

## Problem

Die App ist funktional bereits stark, aber der Datenzustand ist nur indirekt erkennbar.

Aktuell sieht der Nutzer zwar Aktivitäten, Bikes, Exporte und Statistiken, aber nicht auf einen Blick:

- wie vollständig der lokale Cache ist
- ob Aktivitätsdetails flächendeckend vorhanden sind
- ob GPS-/Detaildaten fehlen
- ob nach einem Sync wirklich alles aktuell ist

Dadurch entsteht Unsicherheit:

- "Sind meine Daten vollständig?"
- "Fehlen noch Details für ältere Touren?"
- "Warum ist etwas im Export oder in der Statistik nicht enthalten?"

## Lösungsidee

Ein neuer Bereich `Datenzustand & Sync` schafft Transparenz über Cache, Vollständigkeit und Aktualität.

Pragmatischer Ansatz:

1. V1 als starke Sektion auf dem Home-Screen
2. V2 optional als eigener Detail-Screen mit tieferen Diagnosen

So entsteht sofort Mehrwert, ohne die Navigation unnötig aufzublähen.

## Nutzerwert

Der Bereich soll dem Nutzer sofort beantworten:

- Wie viele Aktivitäten lokal vorhanden sind
- Welcher Statistik- bzw. Cache-Zeitraum abgedeckt ist
- Wie viele Aktivitäten Detaildaten haben
- Wie viele GPS-Punkte lokal vorliegen
- Ob Detaildaten fehlen oder veraltet sind
- Wann Bikes, Aktivitäten und Details zuletzt erfolgreich synchronisiert wurden
- Welche Aktion als Nächstes sinnvoll ist

## UX-Vorschlag

### V1: Home-Screen-Card `Datenzustand & Sync`

Inhalt:

- abgedeckter Aktivitätszeitraum
- Anzahl gecachter Aktivitäten
- Anzahl Aktivitäten mit Details
- Anzahl GPS-Punkte
- Anteil vollständiger Aktivitäten
- Status `vollständig`, `teilweise`, `veraltet`, `leer`

Quick Actions:

- `Fehlende Details laden`
- `Veraltete Details aktualisieren`
- `Alles synchronisieren`

Zusätzliche Hinweise:

- kleine Statuszeile mit letztem erfolgreichen Sync
- klare Erklärung, dass Exporte und Statistik nur Cache-Daten nutzen

### V2: Detail-Screen `Datenzustand`

Zusätzliche Inhalte:

- Aufschlüsselung nach Aktivitäten, Bikes, Details, GPX-relevanten Punkten
- Liste der problematischen Bereiche
- getrennte Zeitstempel für Bike-, Aktivitäten- und Detail-Sync
- Diagnosehinweise bei Teilbestand oder Leerstand
- eventuell Filter "nur unvollständige Aktivitäten"

## Fachliche Metriken

Folgende Kennzahlen sind für V1 sinnvoll:

- `cachedActivityCount` — aus `BoschSmartSystemCacheStatusRepository.getCachedActivityTotalCount()`
- `coveredActivityStart` / `coveredActivityEnd` — min/max `startTime` aus `observeCachedActivities()` (gleiche Quelle wie `GetStatisticsUseCase`, keine UseCase-Abhängigkeit)
- `detailedActivityCount` — aus `ActivityDetailCacheOverview.detailedActivityCount` (bereits als Flow via `observeCachedActivityDetailCacheOverview()`)
- `missingDetailCount` — abgeleitet: `cachedActivityCount - detailedActivityCount`, kein eigenes DB-Feld
- `gpsPointCount` — aus `ActivityDetailCacheOverview.gpsPointCount`
- `detailCoverageRatio` — abgeleitet: `detailedActivityCount / cachedActivityCount`
- `hasStaleDetails` — nutzt `getActivityIdsNeedingDetailSync()` mit aktuellem `CloudSyncDetailMode`
- `lastActivitySyncAt` — aus `ActivityDao.getCacheUpdatedAtEpochMillis()` (aktuell suspend, nicht reaktiv — siehe Technischer Schnitt)
- `lastBikeSyncAt` — aus `BikeDao.getCacheUpdatedAtEpochMillis()` (gleiche Einschränkung)
- `lastDetailSyncAt` — kein globaler Zeitstempel vorhanden; braucht neue DAO-Query `MAX(updatedAtEpochMillis)` über `activity_details`, das ist der einzige Punkt mit echter neuer Datenschicht-Arbeit

Abgeleitete Zustände:

- `empty` — `cachedActivityCount == 0`
- `partial` — `missingDetailCount > 0`
- `complete` — `missingDetailCount == 0 && !hasStaleDetails`
- `stale` — `hasStaleDetails`

## Technischer Schnitt

### Domain

Neues Aggregat:

- `DataStatusOverview`

Neuer UseCase:

- `ObserveDataStatusOverviewUseCase`

Aufgabe:

- vorhandene Cache- und Sync-Informationen aus bestehenden Quellen bündeln
- keine Android-Abhängigkeiten
- nur fachliche Aggregation, keine UI-Formatierung

Konkrete Inputs (bereits existierend, direkt wiederverwendbar):

- `BoschSmartSystemRepository.observeCachedActivities()` → count, covered period
- `BoschSmartSystemCacheStatusRepository.observeCachedActivityDetailCacheOverview()` → detailedActivityCount, gpsPointCount
- `BoschSmartSystemCacheStatusRepository.getActivityIdsNeedingDetailSync()` → hasStaleDetails
- `AppSettings.cloudSyncDetailMode` → für Stale-Erkennung nötig

Reaktivitätsproblem bei Sync-Zeitstempeln:

`ActivityDao.getCacheUpdatedAtEpochMillis()` und `BikeDao.getCacheUpdatedAtEpochMillis()` sind `suspend`-Funktionen, keine Flows. Optionen:
- A (empfohlen): Room-Queries als `Flow<Long?>` auf den `_cache_state`-Tabellen ergänzen
- B (einfacher): Zeitstempel bei jeder Activity-Flow-Emission mitladen (einmalig per `suspend`, da der Flow ohnehin getriggert wird)

### Data

Neues DAO-Element nötig:

- `ActivityDetailDao.getMaxUpdatedAtEpochMillis(): Long?` — für globales `lastDetailSyncAt`
- Optional: `Flow<Long?>` auf `activity_cache_state` und `bike_cache_state` für reaktive Sync-Zeitstempel

Alles andere ist bereits vorhanden. Keine neuen Preferences-Einträge oder Room-Tabellen nötig.

### Presentation

V1:

- neue Home-Komponente `DataStatusCard`
- Mapping von `DataStatusOverview` nach UI-Modell
- klare Statusfarben und knappe Texte
- Quick Actions verdrahten **bestehende UseCases**:
  - `Fehlende Details laden` → `RefreshSmartSystemActivityDetailUseCase` gefiltert via `getActivityIdsNeedingDetailSync()`
  - `Veraltete Details aktualisieren` → gleicher Pfad, anderer Detail-Modus
  - `Alles synchronisieren` → `SyncSmartSystemCloudUseCase`

V2:

- eigener Screen `DataStatusScreen`
- Navigation nur ergänzen, wenn V1 in der Praxis echten Mehrwert zeigt

## Umsetzung in Phasen

### Phase 1

- `ActivityDetailDao.getMaxUpdatedAtEpochMillis()` hinzufügen (neues DAO, einzige neue DB-Arbeit)
- Optional: `Flow<Long?>` auf `activity_cache_state` / `bike_cache_state` (wenn reaktive Zeitstempel gewünscht)
- Domain-Modell `DataStatusOverview` definieren
- UseCase `ObserveDataStatusOverviewUseCase` anlegen — kombiniert `observeCachedActivities()`, `observeCachedActivityDetailCacheOverview()`, Sync-Zeitstempel
- Unit-Tests für Aggregation schreiben (Fake-Repositories bereits vorhanden)

### Phase 2

- Home-Screen um `Datenzustand & Sync` erweitern
- `DataStatusCard` mit Kennzahlen und Quick Actions bauen — Quick Actions verdrahten bestehende UseCases, keine neuen nötig
- Lokalisierte Texte in Deutsch und Englisch ergänzen
- Mapper-Tests ergänzen

### Phase 3

- Reaktive Sync-Zeitstempel falls in Phase 1 noch nicht umgesetzt
- Bessere Hinweise für veraltete Details
- Optional separaten Detail-Screen ergänzen

## Akzeptanzkriterien

- Nutzer erkennt auf dem Home-Screen sofort den lokalen Datenzustand
- Nutzer erkennt, ob Aktivitätsdetails fehlen
- Nutzer erkennt, ob vorhandene Daten potenziell veraltet sind
- Sync-Aktionen sind direkt aus dem Kontext ausführbar
- Statistik- und Exportverhalten werden besser verständlich
- Aggregationslogik bleibt außerhalb der Compose-UI

## Nicht-Ziele

- keine vollständige neue Admin- oder Diagnosekonsole
- keine unnötige Modul-Aufspaltung nur für diese Verbesserung
- keine technische Tiefendiagnose pro API-Call in V1
- keine zusätzliche Cloud-Abfrage nur für reine Anzeigezwecke, wenn Cache-Daten genügen

## Warum diese Verbesserung priorisieren

Die App hat bereits viele starke Funktionen. Der größte zusätzliche Nutzen liegt jetzt nicht in noch mehr Endpunkten, sondern in besserer Transparenz über Datenqualität und Vollständigkeit.

`Datenzustand & Sync` verbessert:

- Vertrauen in die App
- Verständlichkeit von Statistik und Export
- wahrgenommene Zuverlässigkeit
- Bedienbarkeit für weniger technische Nutzer

## Empfohlener Start

Pragmatisch zuerst nur V1 umsetzen:

- neues Domain-Aggregat
- neue Home-Card
- drei klare Kennzahlen
- zwei Quick Actions
- letzter erfolgreicher Sync

Das ist klein genug für einen sauberen ersten Schritt und groß genug, um für Nutzer sofort sichtbar besser zu sein.
