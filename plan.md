# Implementierungsplan: Sync-Vereinfachung und OIDC-Cache

Stand: 2026-04-10

## Ziel

Die bisherige Sync-Logik wird auf zwei klar definierte Funktionen reduziert:

- **Funktion 1 – Initial-Sync**: Lädt beim ersten App-Start automatisch alles vollständig (Bikes, alle Aktivitätsseiten, alle fehlenden Details, OIDC-Daten). Auch manuell als Reset im Setup auslösbar.
- **Funktion 2 – Auffrischen**: Manueller Button auf dem Homescreen. Lädt Bikes, OIDC UserInfo, nur neue Aktivitäten (delta-basiert bis zur ersten bekannten ID) und alle noch fehlenden Details nach.

Hintergrund-Sync, konfigurierbare Detail-Modi, redundante Refresh-Buttons auf Sub-Screens und separate sekundäre Sync-Aktionen auf dem Homescreen entfallen.

---

## Begründung der Kernentscheidungen

### Aktivitäten sind unveränderlich
Bosch liefert fertige Touren, die nachträglich nicht geändert werden. Neue Aktivitäten kommen täglich 1–2 hinzu. Es genügt daher, beim Auffrischen nur fehlende Aktivitäten nachzuladen, nicht den gesamten Bestand neu zu lesen.

### API sortiert nach `startTime DESC`
Dokumentiert in `docs_api/activityRecords.v1.yaml`. Damit kann beim inkrementellen Laden abgebrochen werden, sobald die erste bereits bekannte Aktivitäts-ID auftaucht.

### `MISSING_OR_STALE` ist überflüssig
Da Details zu unveränderlichen Aktivitäten gehören, gibt es keinen sinnvollen Fall, in dem ein Detail nach 30 Minuten als "veraltet" gilt. `MISSING_ONLY` ist immer die korrekte Strategie.

### Hintergrund-Sync ist mit einem manuellen Button redundant
Ein täglicher Auto-Sync bringt keinen Mehrwert, wenn der Nutzer selbst mit einem Knopfdruck aktualisieren kann und dabei exakt das Gleiche passiert.

### OIDC-Daten in `EncryptedSharedPreferences`
UserInfo (email, username, subject) und DiscoveryInfo (issuer, Endpunkte) sind kleine, flache Datensätze – kein Listenformat, kein Suchbedarf. Room wäre überdimensioniert. `EncryptedSharedPreferences` ist konsistent mit der bestehenden Token-Speicherung und deckt den Datenschutzbedarf.

### OIDC-Cache ist nötig für Offline-Betrieb und PDF
`PdfReportMetadataRepositoryImpl` ruft `OidcUserInfoProvider.loadCurrentUserInfo()` und `OidcDiscoveryInfoProvider.loadCurrentDiscovery()` live auf. Ohne Cache schlägt die PDF-Erzeugung offline stumm fehl.

---

## Phase A – Entfernen: Hintergrund-Sync

**Begründung**: Mit dem manuellen Auffrischen-Button und dem Auto-Initial-Sync ist kein täglicher WorkManager-Job mehr nötig.

### Zu löschende Dateien
- `background/BackgroundSyncScheduler.kt`
- `background/BackgroundSyncSettingsObserver.kt`
- `background/SmartSystemBackgroundSyncWorker.kt`

### Zu ändernde Dateien

**`AppSettings.kt`**
- Feld `backgroundSyncMode: BackgroundSyncMode` entfernen
- Enum `BackgroundSyncMode` entfernen

**`AppSettingsRepository.kt`**
- `updateBackgroundSyncMode()` entfernen

**`AppSettingsRepositoryImpl.kt`**
- `updateBackgroundSyncMode()` und zugehörigen Preference-Key entfernen

**`M24BikeStatsApp.kt`**
- Zeile `GlobalContext.get().get<BackgroundSyncSettingsObserver>().start(applicationScope)` entfernen — sonst Compile- oder Startup-Fehler nach Löschung des Observers
- Import von `BackgroundSyncSettingsObserver` entfernen

**`AppModule.kt`**
- Bindings für `BackgroundSyncScheduler`, `BackgroundSyncSettingsObserver`, `SmartSystemBackgroundSyncWorker` entfernen

**`DashboardFeedHandler.kt`**
- `updateBackgroundSyncModeUseCase`-Parameter entfernen
- `updateBackgroundSyncMode()`-Methode entfernen
- `backgroundSyncMode` aus Settings-Observation entfernen

**`DashboardViewModel.kt`**
- `updateBackgroundSyncMode()` entfernen

**`SetupScreen.kt`**
- `backgroundSyncMode`-Parameter und zugehöriges Dropdown entfernen
- `onBackgroundSyncModeSelected`-Callback entfernen

**`AppNavigation.kt`**
- Background-Sync-Callbacks an SetupScreen entfernen

**`UpdateBackgroundSyncModeUseCase.kt`**
- Datei löschen

---

## Phase B – Entfernen: Konfigurierbarer Detail-Modus

**Begründung**: `MISSING_OR_STALE` ist mit unveränderlichen Aktivitäten sinnlos. `MISSING_ONLY` wird fest verdrahtet, die Einstellung entfällt.

### Zu ändernde Dateien

**`AppSettings.kt`**
- Feld `cloudSyncDetailMode: CloudSyncDetailMode` entfernen
- Enum `CloudSyncDetailMode` auf einen einzelnen Wert reduzieren oder ganz entfernen

**`AppSettingsRepository.kt`**
- `updateCloudSyncDetailMode()` entfernen

**`AppSettingsRepositoryImpl.kt`**
- `updateCloudSyncDetailMode()` und Preference-Key entfernen

**`SyncSmartSystemCloudUseCase.kt`**
- `detailMode`-Parameter entfernen, intern immer `MISSING_ONLY`-Logik verwenden

**`SmartSystemBackgroundSyncWorker.kt`**
- Entfällt ohnehin (Phase A)

**`DashboardFeedHandler.kt`**
- `updateCloudSyncDetailModeUseCase`-Parameter entfernen
- `updateCloudSyncDetailMode()`-Methode entfernen
- `cloudSyncDetailMode` aus Settings-Observation entfernen

**`DashboardViewModel.kt`**
- `updateCloudSyncDetailMode()` entfernen

**`SetupScreen.kt`**
- `cloudSyncDetailMode`-Parameter und Dropdown entfernen
- `onCloudSyncDetailModeSelected`-Callback entfernen

**`AppNavigation.kt`**
- Callbacks an SetupScreen bereinigen

**`UpdateCloudSyncDetailModeUseCase.kt`**
- Datei löschen

**`HomeScreen.kt`**
- `onRefreshStaleActivityDetails`-Callback und zugehörigen `HomeSecondaryActionRow`-Zweig entfernen
- `HomeStatusMetricGrid`: Tile "Veraltet" entfernen

**`DashboardOperationsHandler.kt`**
- `refreshStaleActivityDetails()` entfernen

**`DashboardViewModel.kt`**
- `refreshStaleActivityDetails()` entfernen

**`AppNavigation.kt`**
- `onRefreshStaleActivityDetails`-Callback entfernen

---

## Phase C – Entfernen: Redundante Refresh-Buttons auf Sub-Screens

**Begründung**: Sub-Screens lesen aus dem Room-Cache; nach einem Auffrischen auf dem Homescreen aktualisieren sie sich automatisch via Flow. Explizite Refresh-Buttons sind überflüssig.

### Top-Bar Refresh-Aktionen in `AppNavigation.kt`

Die Funktion `shouldShowRefreshAction()` gibt aktuell `true` für die Routen `ACTIVITIES`, `BIKE` und `STATISTICS` zurück und zeigt dort einen Refresh-`IconButton` in der TopBar, der `dashboardViewModel::refresh` aufruft. Diese Einstiegspunkte müssen entfernt werden, da sie eine andere Semantik hätten als der Auffrischen-Button auf dem Homescreen:
- `shouldShowRefreshAction()` und die zugehörige `IconButton`-Verzweigung in `topBarActions` entfernen
- Import von `Icons.Default.Refresh` prüfen und ggf. entfernen (wird noch an anderer Stelle verwendet)

### Betroffene Screens

**Aktivitäten-Detail-Screen (`DashboardDetailScreens.kt`)**
- `onRefreshActivity`-Parameter und zugehöriger `IconButton` entfernen
- Der `onLoadActivity`-Aufruf (cache-first beim Öffnen) bleibt bestehen

**Bike-Detail-Screen (`DashboardDetailScreens.kt`)**
- `onRefreshBike`-Parameter und zugehöriger `IconButton` entfernen
- Der `onLoadBike`-Aufruf (cache-first beim Öffnen) bleibt bestehen

**Track-Screen (`DashboardTrackScreen.kt`)**
- `onRefreshActivity`-Parameter entfernen, falls vorhanden

**`DashboardDetailActionHandler.kt`**
- `refreshBikeDetail()` und `refreshActivityDetail()` entfernen (diese riefen denselben UseCase wie die Load-Varianten auf, nur mit `force=true`)

**`DashboardViewModel.kt`**
- `refreshBikeDetail()` und `refreshActivityDetail()` entfernen

**`AppNavigation.kt`**
- `onRefreshActivity`- und `onRefreshBike`-Callbacks entfernen

---

## Phase D – Entfernen: Separate Sync-UseCase-Kette auf dem Homescreen

**Begründung**: `syncCloudData()`, `loadMissingActivityDetails()` und `cancelPendingActivityDetailsSync()` werden durch die neue einheitliche Auffrischen-Funktion ersetzt.

**`DashboardOperationsHandler.kt`**
- `syncCloudData()` entfernen (wird durch neuen `RefreshSmartSystemDataUseCase` im ViewModel ersetzt)
- `loadMissingActivityDetails()` entfernen (ist in Auffrischen integriert)
- `cancelCloudSync()` bleibt vorerst als Cancel-Mechanismus, wird ggf. umbenannt
- `cancelPendingActivityDetailsSync()` entfernen (kein separater Detail-Sync mehr)

**`HomeScreen.kt`**
- `onLoadMissingActivityDetails`-Callback entfernen
- `onCancelPendingActivityDetailsSync`-Callback entfernen

**`DashboardViewModel.kt`**
- `loadMissingActivityDetails()` entfernen
- `cancelPendingActivityDetailsSync()` entfernen

**`AppNavigation.kt`**
- Callbacks bereinigen

**`RefreshPendingSmartSystemActivityDetailsUseCase.kt`**
- Prüfen ob noch anderweitig verwendet; falls nicht, löschen

---

## Phase E – Neu: OIDC-Cache via `EncryptedSharedPreferences`

**Begründung**: UserInfo und DiscoveryInfo müssen offline verfügbar sein (PDF-Erzeugung, Konto-Screen).

### Neue Datei: `auth/OidcCacheRepository.kt` (Interface)

```
interface OidcCacheRepository {
    fun getCachedUserInfo(): CachedOidcUserInfo?
    fun saveCachedUserInfo(info: CachedOidcUserInfo)
    fun getCachedDiscoveryInfo(): CachedOidcDiscoveryInfo?
    fun saveCachedDiscoveryInfo(info: CachedOidcDiscoveryInfo)
    fun clearOidcCache()
}

data class CachedOidcUserInfo(
    val email: String?,
    val username: String?,
    val subject: String?,
)

data class CachedOidcDiscoveryInfo(
    val issuer: String?,
    val authorizationEndpoint: String?,
    val tokenEndpoint: String?,
    val userInfoEndpoint: String?,
    val jwksUri: String?,
    val revocationEndpoint: String?,
    val introspectionEndpoint: String?,
    val endSessionEndpoint: String?,
    val supportedGrantTypes: List<String>,  // als kommaseparierter String in Prefs speichern
)
// Hinweis: OidcDiscoveryInfoUiModel hat bereits alle diese Felder – das Cache-Modell muss vollständig sein,
// damit der Konto-Screen offline keine Lücken zeigt.
```

### Neue Datei: `data/local/preferences/OidcCacheRepositoryImpl.kt`

- Nutzt `EncryptedSharedPreferences` (gleiche Instanz wie Token-Speicherung oder eigene Instanz)
- Eigene Preference-Keys für jeden OIDC-Wert
- `clearOidcCache()` löscht alle Keys

### Geänderte Datei: `auth/OidcAccountInfo.kt` – `LiveOidcUserInfoProvider`

- Liest OIDC UserInfo live von der API
- Schreibt das Ergebnis nach `OidcCacheRepository`
- Fällt bei API-Fehler auf den Cache zurück (statt `null` zurückzugeben)

### Geänderte Datei: `auth/OidcAccountInfo.kt` – `LiveOidcDiscoveryInfoProvider`

- Analog: lädt live, schreibt in Cache, fällt bei Fehler auf Cache zurück

### Geänderter Datei: `AppModule.kt`

- `OidcCacheRepositoryImpl` binden
- `LiveOidcUserInfoProvider` und `LiveOidcDiscoveryInfoProvider` erhalten `OidcCacheRepository` als Abhängigkeit

---

## Phase F – Neu: AppSettings erweitern

**Begründung**: Der Auto-Initial-Sync braucht ein persistentes Flag, der Delta-Sync eine Zeitmarkierung für die neuste bekannte Aktivität.

### Zu ändernde Datei: `AppSettings.kt`

Neue Felder:
```kotlin
val initialSyncCompletedAtEpochMillis: Long = 0L  // 0 = noch nie durchgeführt
val latestCachedActivityStartTimeMillis: Long = 0L  // startTime der neusten gecachten Aktivität
```

### Achtung: `fallbackToDestructiveMigration`

Room löscht bei einer fehlenden Migration die gesamte Datenbank, lässt aber `SharedPreferences` unberührt. Das bedeutet: `initialSyncCompletedAtEpochMillis > 0` kann gesetzt sein, obwohl die DB leer ist.

**Gegenmaßnahme im ViewModel `init{}`-Block**: Zusätzlich zur Flag-Prüfung die DB auf Leerheit prüfen:
```
if (initialSyncCompletedAt > 0 && activityDao.count() == 0) {
    appSettingsRepository.resetInitialSyncFlag()
}
```
Danach läuft die normale Flag-Prüfung und der Initial-Sync startet automatisch.

### Zu ändernde Datei: `AppSettingsRepository.kt`

Neue Methoden:
```
suspend fun markInitialSyncCompleted(atEpochMillis: Long)
suspend fun resetInitialSyncFlag()
suspend fun updateLatestCachedActivityStartTime(epochMillis: Long)
```

### Zu ändernde Datei: `AppSettingsRepositoryImpl.kt`

- Implementierung der neuen Methoden
- Neue Preference-Keys: `KEY_INITIAL_SYNC_COMPLETED_AT`, `KEY_LATEST_ACTIVITY_START_TIME`

---

## Phase G – Neu: Initial-Sync UseCase ausbauen

**Begründung**: Der bestehende `SyncSmartSystemCloudUseCase` kommt dem Ziel nah, braucht aber drei Erweiterungen: OIDC cachen, Flags setzen, Detail-Modus-Parameter entfernen.

### Zu ändernde Datei: `domain/usecase/SyncSmartSystemCloudUseCase.kt`

Neu: Umbenennung in `PerformInitialSyncUseCase` (oder Name belassen – fachliche Entscheidung beim Implementieren).

Änderungen:
- `detailMode`-Parameter entfernen (immer `MISSING_ONLY`)
- Nach erfolgreichem Abschluss und **nach** dem letzten Room-Commit:
  - `appSettingsRepository.updateLatestCachedActivityStartTime(newestActivityStartTime)`
  - `appSettingsRepository.markInitialSyncCompleted(nowMillis())` — zwingend als letzter Schritt, damit ein Abbruch kein unvollständiges "done"-Flag hinterlässt
- OIDC UserInfo und DiscoveryInfo vor den Aktivitäten laden und cachen (via `OidcUserInfoProvider` und `OidcDiscoveryInfoProvider`, die jetzt automatisch schreiben)
- Fehlerbehandlung: bei OIDC-Fehler weitermachen (kein harter Abbruch), bei Aktivitäten-/Bike-Fehler abbrechen wie bisher

Neue Abhängigkeiten im Konstruktor:
- `appSettingsRepository: AppSettingsRepository`
- `oidcUserInfoProvider: OidcUserInfoProvider`
- `oidcDiscoveryInfoProvider: OidcDiscoveryInfoProvider`

---

## Phase H – Neu: Auffrischen UseCase

**Begründung**: Der inkrementelle Refresh ist fachlich anders als der vollständige Initial-Sync. Ein eigener UseCase macht die Abgrenzung explizit.

### Neue Datei: `domain/usecase/RefreshSmartSystemDataUseCase.kt`

Ablauf:
1. Bikes vollständig laden und cachen
2. OIDC UserInfo **und DiscoveryInfo** laden und cachen — beide, nicht nur UserInfo; Discovery kann beim Initial-Sync transient fehlschlagen und muss beim Auffrischen nachgeholt werden können
3. Aktivitäten delta-laden:
   - Seiten laden (limit = 20, nach startTime DESC)
   - Für jede Seite: prüfen welche IDs neu sind (noch nicht in `activityDao`)
   - Wenn eine Seite keine neuen IDs enthält → Abbruch
   - Wenn Seite kürzer als limit → letzte Seite, Abbruch nach Verarbeitung
   - Neue Aktivitäten in Room cachen
   - **Hard-Limit: maximal 5 Seiten** (= 100 Aktivitäten); wer mehr benötigt (z.B. nach monatelanger Pause), nutzt Reset + Initial-Sync
4. Alle fehlenden Details nachladen (`MISSING_ONLY` über alle gecachten Aktivitäten)
5. `latestCachedActivityStartTime` **erst nach erfolgreichem Room-Commit** der neuen Aktivitäten setzen — nie vorher, um Historienlücken bei Netzwerkabbruch zu verhindern
6. Fortschritts-Callbacks analog zum bestehenden `SyncSmartSystemCloudUseCase`

Abhängigkeiten:
- `repository: BoschSmartSystemRepository`
- `cacheStatusRepository: BoschSmartSystemCacheStatusRepository`
- `authRepository: AuthRepository`
- `appSettingsRepository: AppSettingsRepository`
- `oidcUserInfoProvider: OidcUserInfoProvider`

---

## Phase I – ViewModel und Auto-Initial-Sync

**Begründung**: Das ViewModel orchestriert, wann welche Sync-Funktion läuft.

### Zu ändernde Datei: `DashboardViewModel.kt`

**`init {}`-Block**:
- Prüft `appSettings.initialSyncCompletedAtEpochMillis == 0L`
- Falls ja: `performInitialSync()` starten (nicht `refresh(force=false)`)
- Falls nein: nur Flows beobachten, kein automatischer Netzwerkaufruf

**Neue Methode `performInitialSync()`**:
- Ruft `PerformInitialSyncUseCase` auf
- Zeigt Fortschritt im UiState
- Bei Fehler: `isInitialLoading = false`, Fehlermeldung setzen

**Umgebaute Methode `refresh()`** (wird zu "Auffrischen"):
- Ruft `RefreshSmartSystemDataUseCase` auf
- Kein `force`-Parameter mehr
- Zeigt Fortschritt im UiState

**Entfernte Methoden**:
- `syncCloudData()` (war manueller Vollsync-Button)
- `loadMissingActivityDetails()`
- `refreshStaleActivityDetails()`
- `cancelPendingActivityDetailsSync()`
- `refreshBikeDetail()`
- `refreshActivityDetail()`

**`DashboardFeedHandler.kt`**:
- `refresh(force: Boolean)`-Methode entfernen oder zu `triggerRefresh()` vereinfachen
- `updateCloudSyncDetailModeUseCase`, `updateBackgroundSyncModeUseCase` entfernen

**`DashboardOperationsHandler.kt`**:
- `syncCloudData()` entfernen
- `loadMissingActivityDetails()` entfernen
- `refreshStaleActivityDetails()` entfernen
- `cancelPendingActivityDetailsSync()` entfernen

---

## Phase J – HomeScreen UI

**Begründung**: Ein einziger, klarer "Auffrischen"-Button ersetzt die bisherige Kombination aus Sync-Button, Detail-Laden und Stale-Refresh.

### Zu ändernde Datei: `HomeScreen.kt`

**Callbacks vereinfachen**:
```kotlin
fun HomeScreen(
    uiState: HomeUiState,
    onRefresh: () -> Unit,           // war: onSyncCloudData + onLoadMissingActivityDetails
    onCancelRefresh: () -> Unit,     // war: onCancelSyncCloudData
    onNavigateToActivityDetail: (String) -> Unit,
    onNavigateToActivityTrack: (String) -> Unit,
    modifier: Modifier = Modifier,
)
```

**`HomeStatusHeroCard`**:
- Primärer Button: "Auffrischen" (Icon: `Icons.Default.Sync` oder `Icons.Default.Refresh`)
- Label im laufenden Zustand: "Lädt…"
- Kein separater Sync-Button mehr

**`HomeSyncMetaCard`**:
- `HomeSecondaryActionRow` entfernen (kein "Fehlende Details laden", kein "Details auffrischen")
- Nur noch: letzter Sync-Zeitpunkt, laufender Fortschritt, Cancel-Button

**`HomeStatusMetricGrid`**:
- Tile "Veraltet" (`staleDetailCount`) entfernen
- Verbleibende Tiles: Aktivitäten, Details (mit Coverage), Fehlend, GPS-Punkte

**Initial-Sync-Zustand**:
- Wenn `isInitialLoading == true` und noch kein Cache: eigene leere State-Card mit Hinweis "Erste Einrichtung läuft…"
- Cancel-Button auch beim Initial-Sync anzeigen

---

## Phase K – Setup-Screen: Bereinigen und Reset-Funktion

**Begründung**: Entfernte Einstellungen müssen aus dem UI verschwinden; der Reset (= Initial-Sync erneut auslösen) wird die neue Haupt-Aktion im Sync-Abschnitt.

### Zu ändernde Datei: `SetupScreen.kt`

**Entfernen**:
- Dropdown `cloudSyncDetailMode` (mit Label, Optionen, Callback)
- Dropdown `backgroundSyncMode` (mit Label, Optionen, Callback)

**Neu hinzufügen: Reset-Funktion**

```kotlin
onResetAllData: () -> Unit,
```

UI: `OutlinedButton` mit Warnung-Ton und Label "Alle Daten zurücksetzen".

**Pflicht: Bestätigungsdialog vor Ausführung** — eine destruktive Aktion darf nicht als sofort ausführbarer Button ohne Rückfrage existieren. Ablauf:
1. Button-Klick öffnet `AlertDialog` mit Titel "Alle Daten zurücksetzen?" und kurzer Warnung
2. Bestätigen → `resetAllData()` ausführen
3. Abbrechen → Dialog schließen, nichts passiert

Erklärungstext im Setup (wenn `showExplanationTexts`):
> Löscht den lokalen Datencache (Aktivitäten, Details, Bikes, Konto) und lädt sofort alles neu von der Cloud.

### Reset-Logik (UseCase oder direkt im ViewModel)

Neue Methode `resetAllData()` im ViewModel:
1. **Alle laufenden Coroutinen canceln** (Sync, Exports, Detail-Ladevorgänge) — ohne Cancel können Race Conditions entstehen, bei denen ein laufender Job den Cache nach `clearAllTables()` sofort wieder befüllt
2. `appSettingsRepository.resetInitialSyncFlag()` und `latestCachedActivityStartTime` zurücksetzen
3. `oidcCacheRepository.clearOidcCache()`
4. Room-Datenbank atomar leeren via `roomDatabase.clearAllTables()` — nicht einzelne `deleteAll()`-Aufrufe; das ist performanter, atomar und erfasst automatisch neu hinzukommende Tabellen
5. UiState zurücksetzen (isInitialLoading = true, alle Listen leeren)
6. `performInitialSync()` sofort starten — kein Neustart nötig, der Nutzer bekommt direktes Feedback

**Reset ist destruktiv und startet sofort** — kein "beim nächsten Start"-Modus. Der Nutzer sieht den Fortschritt des Initial-Syncs unmittelbar.

**Entscheidung**: Reset startet den Initial-Sync sofort (ohne Neustart), damit der Nutzer sofortiges Feedback bekommt.

---

## Phase L – Logout: OIDC-Cache und lokale Daten löschen

**Begründung**: `ClearAuthenticationUseCase` löscht derzeit nur Tokens (`authRepository.clearTokens()`). Gecachte OIDC-Daten (UserInfo mit E-Mail, DiscoveryInfo) bleiben erhalten und würden nach einem Logout offline dem nächsten Nutzer angezeigt — auch im PDF-Export. Das ist ein Datenschutz-Problem bei geteilten Geräten.

### Zu ändernde Datei: `ClearAuthenticationUseCase.kt`

Erweitern um:
- `oidcCacheRepository.clearOidcCache()`
- `appSettingsRepository.resetInitialSyncFlag()` — damit der nächste angemeldete Nutzer einen vollständigen Initial-Sync bekommt, nicht den Delta-Refresh des Vorgängers
- `appSettingsRepository.resetLatestCachedActivityStartTime()`

**Aktivitäten und Bike-Daten beim Logout löschen?** Das ist eine fachliche Entscheidung: Da die App an einen Bosch-Account gebunden ist und kein Multi-Account-Betrieb existiert, wäre es sauber, auch den Room-Cache zu leeren. Allerdings erhöht das die Logout-Komplexität erheblich. Minimalansatz: nur OIDC-Cache + Flags leeren; Room-Daten bleiben, bis der nächste Initial-Sync sie überschreibt. Dieser Punkt sollte vor Implementierung final entschieden werden.

### Neue Abhängigkeit in `AppModule.kt`

`ClearAuthenticationUseCase` erhält `OidcCacheRepository` und `AppSettingsRepository` als Konstruktor-Parameter.

---

## Phase M – Strings (DE + EN)

Neue/geänderte Strings:

| Key | DE | EN |
|---|---|---|
| `home_refresh_button` | Auffrischen | Refresh |
| `home_refresh_running` | Lädt… | Loading… |
| `home_initial_sync_title` | Erste Einrichtung | Initial Setup |
| `home_initial_sync_text` | Daten werden vollständig geladen… | Loading all data… |
| `setup_reset_button` | Alle Daten zurücksetzen | Reset All Data |
| `setup_reset_description` | Löscht den Cache und lädt beim nächsten Start alles neu ein. | Clears the cache and reloads everything on next start. |

Zu entfernende Strings:
- alle `background_sync_*`-Labels
- alle `cloud_sync_detail_mode_*`-Labels
- `home_data_status_action_stale`
- `home_data_status_stale`
- `home_sync_cancel_button` (ggf. umbenennen statt löschen)

---

## Phase M – Tests

### `DashboardViewModelTest.kt`
- Tests für `syncCloudData()`, `loadMissingActivityDetails()`, `refreshStaleActivityDetails()` entfernen
- Neue Tests: Auto-Initial-Sync beim ersten Start, kein Auto-Sync wenn Flag gesetzt
- Test: `refresh()` ruft `RefreshSmartSystemDataUseCase` auf

### `AppSettingsTest.kt`
- Tests für `backgroundSyncMode`, `cloudSyncDetailMode` entfernen
- Neue Tests für `initialSyncCompletedAtEpochMillis` und `latestCachedActivityStartTimeMillis`

### Neue Testdatei: `PerformInitialSyncUseCaseTest.kt`
- OIDC-Daten werden gecacht
- Alle Aktivitätsseiten werden geladen
- Fehlende Details werden nachgeladen
- Flags werden gesetzt

### Neue Testdatei: `RefreshSmartSystemDataUseCaseTest.kt`
- Delta-Abbruch bei erster bekannter ID
- Neue Aktivitäten werden gecacht
- Fehlende Details werden nachgeladen
- Flags werden aktualisiert

### `StatisticsViewModelTest.kt`, `StatisticsUiModelMapperTest.kt`
- Keine Änderungen erwartet

---

## Umsetzungsreihenfolge

Die Phasen bauen aufeinander auf und sollten in dieser Reihenfolge umgesetzt werden:

1. **Phase A** – Hintergrund-Sync entfernen inkl. `M24BikeStatsApp.kt`-Startup-Hook
2. **Phase B** – Detail-Modus entfernen (vereinfacht UseCase-Signatur für Phase G)
3. **Phase C** – Sub-Screen Refresh-Buttons + Top-Bar Refresh-Aktionen entfernen
4. **Phase D** – Sekundäre Homescreen-Sync-Aktionen entfernen
5. **Phase E** – OIDC-Cache implementieren (vollständiges Modell inkl. aller DiscoveryInfo-Felder)
6. **Phase F** – AppSettings erweitern + destructiveMigration-Gegenmaßnahme
7. **Phase G** – `PerformInitialSyncUseCase` ausbauen
8. **Phase H** – `RefreshSmartSystemDataUseCase` neu erstellen (mit DiscoveryInfo + Hard-Limit)
9. **Phase I** – ViewModel umbauen (Auto-Initial-Sync + DB-Leerheits-Check)
10. **Phase J** – HomeScreen UI anpassen
11. **Phase K** – SetupScreen bereinigen + Reset-Funktion mit Bestätigungsdialog
12. **Phase L** – Logout: OIDC-Cache + Flags leeren
13. **Phase M** – Strings anpassen (DE + EN)
14. **Phase N** – Tests anpassen und ergänzen

---

## Vollständige Liste betroffener Dateien

### Zu löschende Dateien
- `background/BackgroundSyncScheduler.kt`
- `background/BackgroundSyncSettingsObserver.kt`
- `background/SmartSystemBackgroundSyncWorker.kt`
- `domain/usecase/UpdateBackgroundSyncModeUseCase.kt`
- `domain/usecase/UpdateCloudSyncDetailModeUseCase.kt`
- `domain/usecase/RefreshPendingSmartSystemActivityDetailsUseCase.kt` (nach Prüfung ob noch referenziert)

### Neue Dateien
- `auth/OidcCacheRepository.kt` (Interface + Datenklassen)
- `data/local/preferences/OidcCacheRepositoryImpl.kt`
- `domain/usecase/RefreshSmartSystemDataUseCase.kt`
- `src/test/.../PerformInitialSyncUseCaseTest.kt`
- `src/test/.../RefreshSmartSystemDataUseCaseTest.kt`

### Zu ändernde Dateien

App:
- `M24BikeStatsApp.kt` (BackgroundSyncSettingsObserver-Startup-Zeile entfernen)

Domain/Model:
- `domain/model/AppSettings.kt`
- `domain/repository/AppSettingsRepository.kt`
- `domain/usecase/SyncSmartSystemCloudUseCase.kt`
- `domain/usecase/ClearAuthenticationUseCase.kt` (OIDC-Cache + Flags beim Logout leeren)

Data:
- `data/local/preferences/AppSettingsRepositoryImpl.kt`

Auth:
- `auth/OidcAccountInfo.kt` (UserInfo- und DiscoveryInfo-Provider, inkl. Cache-Fallback)

DI:
- `di/AppModule.kt`

Presentation:
- `presentation/dashboard/DashboardViewModel.kt`
- `presentation/dashboard/DashboardFeedHandler.kt`
- `presentation/dashboard/DashboardOperationsHandler.kt`
- `presentation/dashboard/DashboardDetailActionHandler.kt`
- `presentation/dashboard/HomeScreen.kt`
- `presentation/dashboard/DashboardDetailScreens.kt`
- `presentation/dashboard/DashboardUiModels.kt` (UiState-Felder für Stale bereinigen)
- `presentation/navigation/SetupScreen.kt` (inkl. AlertDialog für Reset)
- `presentation/navigation/AppNavigation.kt` (Top-Bar Refresh-Aktionen + Sub-Screen Callbacks)

Strings:
- `res/values/strings.xml`
- `res/values-de/strings.xml`

Tests:
- `test/.../DashboardViewModelTest.kt`
- `test/.../AppSettingsTest.kt`

---

## Abnahmekriterien

- App startet und lädt beim allerersten Start automatisch alle Daten (Initial-Sync)
- Bei allen weiteren Starts kein automatischer Netzwerkaufruf
- "Auffrischen"-Button auf dem Homescreen lädt Bikes, OIDC, neue Aktivitäten und fehlende Details
- Beim Auffrischen wird die Paginierung abgebrochen, sobald eine bekannte Aktivitäts-ID gefunden wird
- PDF-Erzeugung funktioniert auch offline (UserInfo und DiscoveryInfo aus Cache)
- Konto-Screen zeigt gecachte OIDC-Daten ohne Netzwerkaufruf
- Kein Hintergrund-Sync läuft (WorkManager-Job nicht registriert)
- Kein Refresh-Button in ActivityDetail-, BikeDetail- und Track-Screen
- Setup zeigt keinen Detail-Modus und keinen Hintergrund-Sync
- Reset im Setup leert den Cache und startet den Initial-Sync neu
- Nach Logout sind OIDC-Cache und Sync-Flags geleert
- Nach einem Logout und erneutem Login startet der Initial-Sync automatisch
- Nach `fallbackToDestructiveMigration` (leere DB bei gesetztem Flag) startet der Initial-Sync automatisch
- Reset im Setup zeigt Bestätigungsdialog und startet sofort den Initial-Sync ohne App-Neustart
- Kein Refresh-Icon mehr in der TopBar auf Aktivitäten-, Bike- und Statistik-Screen
- Konto-Screen zeigt offline vollständige DiscoveryInfo (alle Felder) aus dem Cache
- Alle Unit-Tests grün
- `./gradlew assembleRelease` erfolgreich
