# Bosch API Gap-Liste aus `docs_api`

Stand: 2026-04-07

## Bereits in der App angebunden

- Activities: `/activity/smart-system/v1/activities`
- Activity Detail: `/activity/smart-system/v1/activities/{activityId}/details`
- Bikes: `/bike-profile/smart-system/v1/bikes`
- Bike Detail: `/bike-profile/smart-system/v1/bikes/{bikeId}`

## Bereits verfügbar, aber im Modell nur teilweise genutzt

### eBike Profile API

Status: hoher Nutzen, geringe bis mittlere Umsetzungskosten

- `oemId`
- `serviceDue.date`
- `serviceDue.odometer`
- `antiLockBrakeSystems`
- `connectModule`
- vollständigeres Komponenten-Inventar je Bike

Nutzen für die App:

- Service-fällig-Anzeige
- bessere Bike-Detailansicht
- saubereres Komponentenprofil

## Neue Datenquellen mit hohem Produktnutzen

### 1. Digital Service Book API

Endpoint:

- `/service-book/smart-system/v1/service-records?bikeId=...`

Neue Daten:

- Servicehistorie je Bike
- Händlerdaten
- `odometerValue` bei Serviceereignissen
- Inspektionen
- Software-Updates
- Customer Reports
- Bike-ID-Wechsel
- Batterie-Messungen

Besonders wertvoll:

- Batterie-Messung mit
- `fullChargeCycles`
- `measuredEnergyCapacity`
- `nominalEnergyCapacity`
- `measuredCapacityPercentage`
- `onBikeMeasurement`

App-Ideen:

- Wartungschronik
- Service-Timeline
- Software-/Firmware-Historie
- Batteriegesundheit aus echten Messwerten

Priorität: sehr hoch

### 2. Bike Pass API

Endpoint:

- `/bike-pass/smart-system/v1/bike-passes?bikeId=...`

Neue Daten:

- `frameNumber`
- `frameNumberPosition`
- Bike-Beschreibung / Merkmale
- `theftReportLogs`
- `riderPortalLink`
- Diebstahl-Ort / Zeitpunkt

App-Ideen:

- Bike-Pass-Ansicht
- Diebstahl-/Sicherheitsansicht
- Rahmennummer und Identifikationsdaten

Priorität: hoch

### 3. eBike Registration API

Endpoint:

- `/bike-registration/smart-system/v1/registrations`

Neue Daten:

- Registrierungszeitpunkt von Bike
- Registrierungszeitpunkt von Komponenten
- `componentType`
- `partNumber`
- `serialNumber`

App-Ideen:

- Registrierungsübersicht
- Komponenten-Inventar
- Abgleich registrierter Komponenten gegen Bike-Profil

Priorität: hoch

Status:

- umgesetzt am 2026-04-07
- App zeigt jetzt Registrierungsübersicht und registrierte Komponenten im Konto-/Bike-Detail
- API-Test hat dafür eine eigene Kurzansicht

## Neue Datenquellen mit speziellem, aber starkem Mehrwert

### 4. Diagnosis Field Data API

Endpoint:

- `/diagnosis-field-data/smart-system/v1/capacity-testers?partNumber=...&serialNumber=...`

Voraussetzung:

- Batterie-`partNumber` und `serialNumber` aus dem Bike-Profil

Neue Daten:

- Capacity-Tester-Messhistorie
- Fehlerzähler
- Spannungswerte
- Stromwerte
- Temperatur-Extrema
- Messzeitpunkte

App-Ideen:

- fundierte Batterie-Health-Ansicht
- Diagnose-/Werkstattmodus
- Verlauf realer Batterietests

Einschränkung:

- häufig nur verfügbar, wenn echte Werkstatt-/Tester-Messungen existieren

Priorität: mittel bis hoch

## Eher B2B / Werkstatt / Hersteller

### 5. Remote Configuration API

Endpoint:

- `/remote-configuration/smart-system/v1/cases?bikeId=...`

Neue Daten:

- Konfigurationsfälle
- Freigabe-/Ablehnungsstatus
- Original-/Zielkonfiguration
- Händlerbezug

Eignung:

- eher für Diagnose oder Hersteller-/Werkstattkontext

Priorität: niedrig

### 6. Bulk Configuration API

Endpoint:

- `/bulk-configuration/smart-system/v1/installation-reports?bikeId=...`

Neue Daten:

- Installationsreports für Bulk-Konfigurationen

Eignung:

- technisch
- wenig Endnutzerwert

Priorität: niedrig

### 7. Release Management API

Endpoint:

- `/software-update/smart-system/v1/installation-reports?bikeId=...`

Neue Daten:

- Update-Installationsreports
- Status einzelner Komponenten
- Fehlerdetails bei fehlgeschlagenen Updates

Eignung:

- nützlich für Diagnose- oder Supportansicht
- für normale Rider-App nur sekundär

Priorität: niedrig bis mittel

## Empfohlene Umsetzungsreihenfolge

1. eBike Profile vervollständigen
2. Digital Service Book integrieren
3. Bike Pass integrieren
4. eBike Registration integrieren
5. Diagnosis Field Data integrieren
6. Release Management optional
7. Remote Configuration und Bulk Configuration nur bei Diagnosefokus

## Wahrscheinlich bester nächster Schritt

Wenn nur ein Bereich als Nächstes umgesetzt werden soll:

- zuerst `Digital Service Book`

Begründung:

- höchster zusätzlicher Nutzwert
- echte neue Datendomäne
- direkt sichtbarer Mehrwert in der App
- besonders stark für Batterie- und Wartungsansichten

## Wichtige Einschränkungen

- Viele Endpunkte liefern nur Daten, wenn der Nutzer diese Daten im Bosch-Portal freigegeben hat.
- Einige Endpunkte sind oft leer, obwohl sie technisch funktionieren.
- `Diagnosis Field Data` ist ereignisbasiert und meist nur nach Werkstatt-/Kapazitätstest sinnvoll.
- `Bike Pass` und `Service Book` sind je nach Nutzerprofil unterschiedlich vollständig.

## Technische Umsetzungsliste für Priorität 1 bis 3

### 1. eBike Profile vervollständigen

Ziel:

- vorhandenen `/bikes`- und `/bikes/{bikeId}`-Pfad fachlich vollständiger ausnutzen

Betroffene Bereiche:

- Domain-Modelle
- JSON-Parser
- Room-Entities und Migration
- Mapper
- Bike-Detail-UI

Konkrete Schritte:

1. `BoschBike` erweitern um
- `oemId`
- `serviceDueDate`
- `serviceDueOdometerMeters`
- `connectModule`
- `antiLockBrakeSystems`

2. gemeinsames Komponentenmodell prüfen
- entweder `BoschComponent` beibehalten und mehrfach verwenden
- oder differenzierte Typen nur dann einführen, wenn UI/Logik sie wirklich braucht

3. `BikeEntity` erweitern um
- `oemId`
- `serviceDueDate`
- `serviceDueOdometerMeters`
- `connectModuleSerialNumber`
- `connectModulePartNumber`
- `connectModuleProductName`

4. zusätzliche Tabelle für ABS-Komponenten anlegen
- z. B. `bike_abs_components`
- gleiche Struktur wie Batterie-/Assist-Mode-Tabellen

5. Room-Migration vorbereiten
- Datenbankversion von `7` auf `8`
- neue Spalten für `bikes`
- neue Tabelle für ABS-Komponenten
- bestehende Daten erhalten

6. Parser in `BoschSmartSystemParser` erweitern
- `oemId`
- `serviceDue`
- `connectModule`
- `antiLockBrakeSystems`

7. Entity-Mapping ergänzen
- Domain -> Entity
- CachedBike/Projection -> Domain

8. UI erweitern
- Service-fällig-Karte oder Detailsektion
- OEM-ID anzeigen
- ConnectModule anzeigen
- ABS-Komponenten anzeigen

9. Tests ergänzen
- Parser-Test mit vollständigem Bike-Profil
- Mapper-Test für neue Felder
- Migrationstest für DB v8
- UI-Mapper-Test für Service-fällig-Anzeige

Offene Entscheidung:

- `serviceDue.date` als ISO-String speichern oder früh in `Instant`/Epoch konvertieren

Empfehlung:

- zunächst als Original-String persistieren, Formatierung erst in UI/Mapper

### 2. Digital Service Book integrieren

Ziel:

- neue Domäne für Wartung, Softwarehistorie und Batterie-Messwerte aufbauen

Endpoint:

- `/service-book/smart-system/v1/service-records?bikeId=...`

Betroffene Bereiche:

- API-Endpunktkatalog
- Repository
- Parser
- neue Domain-Modelle
- neue Room-Tabellen
- Sync-Orchestrierung
- neue UI-Screens oder Sektionen

Konkrete Schritte:

1. neuen API-Test-Endpunkt ergänzen
- `SERVICE_BOOK_RECORDS`
- Pfad mit `bikeId`

2. Repository-API erweitern
- `getServiceRecords(accessToken, bikeId)`
- `observeCachedServiceRecords(bikeId)`
- optional `getCachedServiceRecords(bikeId)`

3. neue Domain-Modelle anlegen
- `BoschServiceRecord`
- `BoschServiceRecordType`
- `BoschBikeDealer`
- `BoschBatteryMeasurement`
- optional spezialisierte Submodelle für
- `softwareUpdate`
- `customerReport`
- `inspection`

4. Modellierung pragmatisch halten
- in Phase 1 nicht die komplette OpenAPI 1:1 abbilden
- zuerst nur die app-relevanten Teilmengen aufnehmen:
- `id`
- `type`
- `bikeId`
- `createdAt`
- `odometerValue`
- `bikeDealer`
- Batterie-Messung
- Software-Update-Kernaussagen

5. neue Room-Tabellen anlegen
- `service_records`
- optional `service_record_software_updates`
- optional `service_record_components`
- alternativ in Phase 1 einige Detailpayloads als JSON-Spalte speichern

6. Room-Migration planen
- Datenbankversion von `8` auf `9` falls Punkt 1 zuerst umgesetzt wird
- falls separat implementiert: von `7` auf `8`

7. Parser implementieren
- robuster Umgang mit optionalen `details.*`
- nur relevante Felder extrahieren
- unbekannte Teilstrukturen ignorieren statt Fail

8. Sync-Strategie festlegen
- Laden pro `bikeId`
- beim Bike-Refresh optional Service-Records für sichtbare Bikes nachziehen
- Background-Sync nur optional, um Netzlast klein zu halten

9. UI-Plan Phase 1
- neue Detailsektion "Service"
- chronologische Liste
- Zeilen mit
- Typ
- Datum
- Händler
- km-Stand
- Highlight für Batterie-Messungen

10. UI-Plan Phase 2
- Software-Update-Historie
- Batteriegesundheit aus gemessener Kapazität
- Service-Countdown aus letztem Record plus `serviceDue`

11. Export prüfen
- optional PDF-Summary um Servicehistorie ergänzen
- CSV-Export für Service-Records separat erwägen

12. Tests ergänzen
- Parser-Test für
- `BBP_MEASUREMENT`
- `SOFTWARE_UPDATE`
- `INSPECTION`
- DAO-/Repository-Test
- UI-Mapper-Test
- Sync-Test für leere Listen und Teilpayloads

Wichtige Modellentscheidung:

- volle Normalisierung aller Unterstrukturen lohnt anfangs nicht

Empfehlung:

- Phase 1: Service-Record-Kopf normalisiert speichern, ausgewählte Detailfelder explizit speichern, Rest optional als Raw-JSON

### 3. Bike Pass integrieren

Ziel:

- Bike-Identitäts- und Sicherheitsdaten je Bike ergänzen

Endpoint:

- `/bike-pass/smart-system/v1/bike-passes?bikeId=...`

Betroffene Bereiche:

- API-Endpunktkatalog
- Repository
- Parser
- neue Room-Tabellen
- Bike-Detail-UI

Konkrete Schritte:

1. neuen API-Test-Endpunkt ergänzen
- `BIKE_PASS`

2. Repository-API erweitern
- `getBikePass(accessToken, bikeId)`
- `observeCachedBikePass(bikeId)`

3. neue Domain-Modelle anlegen
- `BoschBikePass`
- `BoschTheftReportLog`
- `BoschTheftLocation`
- `BoschBikeOwner` nur falls im UI wirklich benötigt

4. neue Room-Tabellen anlegen
- `bike_passes`
- `bike_theft_report_logs`

5. Parser implementieren für
- `frameNumber`
- `frameNumberPosition`
- `description`
- `createdAt`
- `updatedAt`
- `theftReportLogs`
- `riderPortalLink`
- `location`

6. Migration planen
- nächste DB-Version nach Service-Book-Schritt

7. UI-Plan Phase 1
- neue Bike-Pass-Sektion im Bike-Detail
- Rahmennummer
- Position der Rahmennummer
- Merkmale/Beschreibung
- letzter Aktualisierungszeitpunkt

8. UI-Plan Phase 2
- Diebstahl-Log-Historie
- Kartenlink/Portal-Link als Aktion
- letzter bekannter Ort nur bei vorhandenen Daten

9. Datenschutz prüfen
- heikle Daten wie Besitzerkontakt oder exakter Diebstahl-Ort bewusst nur bei echtem Mehrwert anzeigen

10. Sync-Strategie
- beim Laden von Bike-Details Bike-Pass direkt mitziehen
- bei Fehler oder leerer Antwort Bike-Profil weiterhin normal anzeigen

11. Tests ergänzen
- Parser-Test für Bike-Pass ohne Theft Logs
- Parser-Test mit Theft Logs
- DAO-/Repository-Test
- UI-Mapper-Test für leere und vollständige Zustände

## Empfohlene Reihenfolge auf Code-Ebene

1. Punkt 1 zuerst komplett abschließen, weil dafür die bestehende Bike-Pipeline nur erweitert werden muss.
2. Danach Punkt 3 oder Punkt 2 je nach UI-Ziel.
3. Wenn du schnell sichtbaren Mehrwert willst: nach Punkt 1 direkt Bike Pass.
4. Wenn du fachlich den größten Mehrwert willst: nach Punkt 1 direkt Digital Service Book.

## Ticket-Schnitt für die Umsetzung

### Ticket A: Bike-Profil vervollständigen

- Domain erweitern
- Parser erweitern
- Room v8
- Bike-Detail-UI ergänzen
- Tests anpassen

### Ticket B: Bike Pass Basisintegration

- Endpoint + Repository
- Parser + Tabellen
- Bike-Pass-Sektion im UI
- Tests

### Ticket C: Service Book Basisintegration

- Endpoint + Repository
- minimales Datenmodell
- Tabellen für Service-Record-Kopf plus Batterie-Messung
- Timeline-UI
- Tests

### Ticket D: eBike Registration Basisintegration

- Endpoint + Repository
- minimales Datenmodell
- Tabelle für Registrierungen
- Registrierungssektionen im Bike-Detail
- API-Test-Zusammenfassung
- Tests

## Umsetzungsstand 2026-04-07

Erledigt:

- Ticket A: eBike Profile erweitert um `oemId`, `serviceDue`, `connectModule`, ABS
- Ticket B: Bike Pass mit Theft-Logs integriert
- Ticket C: Service Book integriert
- Ticket D: eBike Registration integriert
- Login-Screen zeigt zusätzlich den Hinweis auf das Flow-Portal `https://flow.bosch-ebike.com/data-act`

Aktuelle sichtbare Bereiche in der App:

- Konto-/Bike-Detail: `Service book`, `Service record X`, `Bike pass`, `Theft report X`, `Registrations`, `Registered component X`
- API-Test: `Bike Pass`, `Service Book`, `Registrations`

Wichtige fachliche Hinweise:

- `Service book = 0` ist zulässig und bedeutet meist nur, dass Bosch für dieses Bike keine freigegebenen Serviceeinträge liefert.
- Registrierungen kommen kontoweit und werden in der App auf das aktuelle Bike und seine bekannten Komponenten gefiltert.
- Zusatzdaten aus Bike Pass, Service Book und Registrierungen bleiben bei temporären API-Fehlern im Cache erhalten.

Nächste sinnvolle Erweiterung:

- `Diagnosis Field Data` für echte Batterie-Kapazitätstester-Daten
