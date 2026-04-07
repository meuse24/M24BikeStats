# Plan: Gemeinsame Codebasis fuer Kartenlogik

## Ziel

Die drei Kartenpfade

- Gesamt-Touren: `MapSummaryScreen`
- Einzel-Tour: `TrackMapFullScreen`
- PDF: `drawRoutePointMap`

sollen nicht auf eine einzige Rendering-Funktion vereinheitlicht werden. Stattdessen soll die gemeinsame mathematische und geografische Logik in eine kleine, stabile Utility-Schicht ausgelagert werden.

Der Grund dafuer ist einfach:

- Die Render-Ziele sind unterschiedlich: Compose-Map, Compose-Track-Map und PDF-Canvas.
- Die Interaktionen sind unterschiedlich: Klick-Selektion, Auto-Fit, Overlays, Export.
- Die zugrunde liegende Geometrie ist aber sehr aehnlich: Bounds, Padding, Mercator-Projektion, Zoom-Schaetzung, Viewport-Fit.

Damit gibt es echtes Shared-Code-Potenzial, aber nur im Unterbau.

## Bewertung

### Wo ein gemeinsamer Unterbau Mehrwert hat

- Weniger duplizierte Mercator- und Zoom-Mathematik
- Konsistenteres Verhalten beim Auto-Fit ueber App und PDF hinweg
- Einfachere Bugfixes, weil Geometrie nur an einer Stelle angepasst wird
- Leichter testbare Kartenlogik ohne UI- oder PDF-Abhaengigkeiten

### Wo eine Vereinheitlichung wenig oder keinen Mehrwert hat

- Rendering selbst
- Compose-spezifische State- und Camera-Integration
- PDF-spezifisches Tile- und Canvas-Zeichnen
- Layer-Konfiguration fuer Punkte, Linien, Start- und Endmarker

Eine Vollabstraktion wuerde hier eher Komplexitaet erzeugen als sie zu reduzieren.

## Empfohlene gemeinsame Bausteine

### 1. Gemeinsames Geo-Grundmodell

Ein kleines, UI-freies Paket fuer Kartengeometrie, z. B. `presentation.map.geometry` oder `core.map`.

Vorgeschlagene Datentypen:

- `GeoPoint(latitude: Double, longitude: Double)`
- `GeoBounds(minLatitude, maxLatitude, minLongitude, maxLongitude, centerLatitude, centerLongitude)`
- `MapViewportSpec(centerLatitude, centerLongitude, zoom)`

Begruendung:

- Die drei Pfade arbeiten alle mit Koordinaten und Bounds.
- Das Modell macht die spaeteren Helfer klarer und besser testbar.

### 2. Bounds-Berechnung aus Punkten

Eine gemeinsame Funktion fuer:

- Bounds aus Punktlisten berechnen
- optionalen Mindest-Span erzwingen
- symmetrisches Padding anwenden

Vorgeschlagene Helfer:

- `computeGeoBounds(points: List<GeoPoint>): GeoBounds`
- `inflateGeoBounds(bounds: GeoBounds, latPaddingFactor: Double, lngPaddingFactor: Double, minSpan: Double): GeoBounds`

Begruendung:

- Diese Logik existiert heute mehrfach in leicht unterschiedlicher Form.
- Die Unterschiede sollten als Parameter ausdrueckbar sein, nicht als Copy-Paste.

### 3. Gemeinsame Mercator-Helfer

Vorgeschlagene Helfer:

- `latitudeToMercatorYNormalized(latitude: Double): Double`
- `mercatorToLatitude(normalizedY: Double): Double`
- `longitudeToWorldX(longitude: Double, tileSize: Double, zoom: Double): Double`
- `latitudeToWorldY(latitude: Double, tileSize: Double, zoom: Double): Double`

Begruendung:

- Diese Funktionen sind reine Mathematik.
- Sie haben keinen UI-Kontext und sind ideale Shared Utilities.
- Genau dort ist heute die offensichtlichste Duplizierung.

### 4. Gemeinsame Zoom-Schaetzung aus Bounds und Viewport

Vorgeschlagene Helfer:

- `estimateZoomToFit(bounds, viewportWidthPx, viewportHeightPx, tileSize, padding, minZoom, maxZoom): Double`
- optional `fitCameraToBounds(...)` als Rueckgabe von Zentrum plus Zoom

Begruendung:

- Gesamtkarte, Track-Karte und PDF berechnen alle einen Fit-Viewport.
- Die Regeln unterscheiden sich vor allem in Tile-Size, Padding und Zoom-Grenzen.
- Das sind gute Parameter, keine guten Gruende fuer duplizierten Code.

### 5. Gemeinsame Punkt-Fit-Pruefung

Vorgeschlagene Helfer:

- `pointsFitInViewport(...)`
- `normalizeWrappedWorldDelta(...)`

Begruendung:

- Besonders fuer `MapSummaryScreen` wichtig, aber auch allgemein nuetzlich.
- Das ist spezialisierte Geometrie, die zentral besser wartbar ist.

### 6. Kleine spezialisierte Adapter pro Anwendungsfall

Die drei konkreten Pfade sollen duenn bleiben und nur noch ihre Spezialfaelle steuern:

- `MapSummaryScreen`: Punkte, Klick-Toleranz, gespeicherte Kamera
- `TrackMapFullScreen`: Track-Bounds, asymmetrisches Padding fuer Top/Bottom-UI
- `PdfPageBuilder`: PDF-Tile-Size, tileProvider, Canvas-Zeichnung

Begruendung:

- So bleibt jede Funktion lesbar.
- Die Spezifika gehen nicht in einer generischen Monster-API unter.

## Was bewusst getrennt bleiben sollte

Diese Teile sollten nicht vereinheitlicht werden:

- `MaplibreMap`-Composables
- Layer-Aufbau fuer Punktkarte und Track-Karte
- PDF-Rendering in `PdfPageBuilder`
- Tile-Background-Rendering fuer PDFs
- Klick-Interaktion und Marker-Auswahl
- Share-/Export-spezifische Aktionen

Begruendung:

- Hier ist die Aehnlichkeit oberflaechlich, nicht strukturell.
- Eine gemeinsame API wuerde eher kuenstlich wirken und die Lesbarkeit verschlechtern.

## Empfohlene Zielstruktur

`presentation/map/geometry/` scheidet als Paketort aus: `PdfPageBuilder` liegt in `data/export/` und darf nicht aus `presentation/` importieren — das wuerde die Schichtenregel (`data` → `presentation`) verletzen.

`domain/` scheidet ebenfalls aus: reine Projektions- und Viewport-Mathematik ist keine fachliche Domain-Logik.

Der richtige Ort ist das bestehende `shared/`-Paket, das bereits von beiden Schichten genutzt wird:

- `app/src/main/java/info/meuse24/m24bikestats/shared/mapgeo/GeoPoint.kt`
- `app/src/main/java/info/meuse24/m24bikestats/shared/mapgeo/GeoBounds.kt`
- `app/src/main/java/info/meuse24/m24bikestats/shared/mapgeo/MapProjection.kt`
- `app/src/main/java/info/meuse24/m24bikestats/shared/mapgeo/MapViewportCalculator.kt`

Hinweis: `ActivityMapPoint` im Domain-Modell traegt bereits Koordinaten. Ob ein eigener `GeoPoint`-Typ neben `ActivityMapPoint` noetig ist oder ob eine Extension auf `ActivityMapPoint` genuegt, sollte bei der Umsetzung von Phase 1 entschieden werden.

## Umsetzungsreihenfolge

### Phase 1: Extraktion ohne Verhaltensaenderung

- Gemeinsame Mercator-Helfer extrahieren
- Gemeinsame Bounds-Berechnung extrahieren
- Alle drei bestehenden Pfade intern darauf umstellen
- Keine API- oder UI-Aenderungen

Warum zuerst:

- Niedrigstes Risiko
- Schnellster Test auf echten Mehrwert

### Phase 2: Gemeinsame Viewport-/Zoom-Berechnung

- `estimateZoomToFit` zentralisieren
- `fitCameraToBounds` oder aequivalente Rueckgabe einfuehren
- Parameter fuer Tile-Size, Padding und Zoom-Limits explizit machen

Warum erst danach:

- Hier sitzen die inhaltlichen Unterschiede.
- Diese Stufe sollte erst angegangen werden, wenn Phase 1 sauber laeuft.

### Phase 3: Optionale Feinarbeit

- `pointsFitInViewport` und Saved-Camera-Pruefungen konsolidieren
- gemeinsame Tests fuer typische Edge-Cases ergaenzen
- nur bei Bedarf weitere kleine Helper zusammenziehen

## Teststrategie

Die extrahierte Geometrie sollte separat unit-getestet werden.

Wichtige Faelle:

- Einzelpunkt
- sehr eng beieinanderliegende Punkte
- grosse Nord-Sued- und Ost-West-Spannen
- Punkte nahe Datumsgrenze oder mit grosser Laengenausdehnung
- asymmetrisches Padding
- minimale und maximale Zoom-Grenzen

Begruendung:

- Genau hier entsteht der eigentliche Nutzen der Extraktion.
- Ohne Tests verschiebt man die Komplexitaet nur an einen anderen Ort.

## Entscheidung

Es gibt klaren Mehrwert fuer eine gemeinsame Codebasis im Bereich Geometrie, Projektion und Fit-Berechnung.

Es gibt dagegen wenig Mehrwert fuer eine gemeinsame Rendering-Schicht.

Die richtige Richtung ist daher:

- gemeinsamer mathematischer Unterbau
- getrennte, kleine Renderer fuer App-Gesamtkarte, Einzeltrack und PDF

Das reduziert Duplizierung, ohne die drei Anwendungsfaelle unnoetig zu verkoppeln.
