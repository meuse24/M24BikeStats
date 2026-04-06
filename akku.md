# Analyse Akkugesundheit (State of Health - SOH) für Bosch eBike Smart System

Basierend auf den verfügbaren Daten aus der Bosch API lässt sich die Akkugesundheit fundiert berechnen. Da Bosch im BMS (Battery Management System) die kumulierte Energieabgabe misst, haben wir eine bessere Datenbasis als bei reinen Schätzungen.

## 1. Relevante API-Felder

### `BoschBattery`:
| Feld | Typ | Bedeutung |
| :--- | :--- | :--- |
| `deliveredWhOverLifetime` | Int | Kumulative Energieabgabe in Wh seit Produktion (BMS-Odometer) |
| `totalChargeCycles` | Double | Äquivalente Volladezyklen gesamt (on-bike + off-bike) |
| `onBikeChargeCycles` | Double | Ladezyklen am Fahrradanschluss |
| `offBikeChargeCycles` | Double | Ladezyklen mit ausgebautem Akku (extern) |

### `BoschDriveUnit`:
| Feld | Typ | Bedeutung |
| :--- | :--- | :--- |
| `totalPowerOnHours` | Int | Gesamtbetriebsstunden des Antriebssystems |
| `supportPowerOnHours` | Int | Stunden mit aktiver Motorunterstützung (Pedaldruck erkannt) |

**Wichtig:** `totalChargeCycles` ist der korrekte Divisor für die SOH-Formel, nicht die Summe aus on/off-bike. Bosch zählt äquivalente Volladezyklen nach IEC 61960: ein Ladevorgang von 50% auf 100% zählt als 0,5 Zyklen. Die Zykluszahl erscheint als Double (z.B. 83,8), was echtes Coulomb-Counting bestätigt.

---

## 2. Die mathematische Grundlage (SOH-Formel)

Der State of Health (SOH) beschreibt das Verhältnis der aktuellen Maximalkapazität zur ursprünglichen Nennkapazität.

**Formel:**
`SOH = (deliveredWhOverLifetime / (totalChargeCycles * nominalCapacityWh)) * 100`

Die Formel ist eine Variante der **Wh-Throughput-Methode** (Variante des Coulomb-Counting). Sie ist die bestmögliche externe Annäherung ohne direkten BMS-Zugang – besser als OCV-Messung (nur im Ruhezustand anwendbar) oder Innenwiderstandsmessung (erfordert Laborausrüstung).

**Was die Formel misst:** Den *historisch gemittelten Wirkungsgrad* des Akkus, nicht seinen exakten aktuellen Zustand. Ein Akku, der früh stark degradiert ist und jetzt stabil bleibt, erscheint etwas schlechter als er aktuell ist – andersherum gilt das Gleiche.

**Ladeeffizienz:** Li-Ion-Akkus haben einen Ladewirkungsgrad von ca. 90–95%. Das bedeutet, die tatsächlich eingeladene Energie ist etwas höher als `totalChargeCycles * nominalCapacityWh`. Der berechnete SOH wird dadurch minimal nach oben verzerrt – eine konservative Verzerrung zugunsten des Nutzers.

---

## 3. Realistische Bewertungsskala (Farben)

Die Bewertung orientiert sich an der Bosch-Garantie und dem typischen Degradationsverhalten von Li-Ion-Zellen:

*   **Grün (Optimale Gesundheit): 100% - 90%**
    *   Akku ist neuwertig oder wurde sehr schonend behandelt.
    *   Typisch für die ersten 150–200 Zyklen.
*   **Gelb-Grün (Guter Zustand): 89% - 80%**
    *   Normale Alterung. Merklicher, aber im Alltag kaum einschränkender Kapazitätsverlust.
    *   Typischer Bereich nach 300–500 Zyklen bei normaler Nutzung (Community-Erfahrungswert: ~1–2% Verlust pro 100 Zyklen).
*   **Gelb (Ok / Erhöhte Alterung): 79% - 70%**
    *   Der Akku hat die typische "Wohlfühlzone" verlassen. Reichweitenverluste sind spürbar.
*   **Orange (Verschlissen): 69% - 60%**
    *   Nähert sich der Bosch-Garantiegrenze. Der Akku nähert sich dem Ende seiner wirtschaftlichen Lebensdauer.
*   **Rot (Kritisch): < 60%**
    *   Unterhalb der Bosch-Garantiegrenze. Zellendrift und plötzliche Abschaltungen unter Last werden wahrscheinlicher.

**Bosch-Garantie:** Mindestens 60% der Nennkapazität nach **500 äquivalenten Volladezyklen oder 2 Jahren** (je nachdem, was früher eintritt). Gilt für PowerTube und PowerPack.

---

## 4. Heuristische Ergänzungen (Der "M24-Score")

### A. Risikoindikator statt Zyklen-Malus

Ein Akku mit 95% SOH bei 10 Zyklen ist "gesünder" als einer mit 95% SOH bei 400 Zyklen, weil bei hoher Zyklenzahl der Innenwiderstand höher ist und Spannungseinbrüche unter Last wahrscheinlicher werden.

**Empfehlung:** Den Zyklenstand *nicht* als Abzug auf den SOH-Prozentwert rechnen (das verfälscht eine physikalische Größe), sondern als separaten **Risikoindikator** anzeigen:

```
SOH: 82%  (Gelb-Grün)
Risiko:   Mittel  (380 Zyklen – erhöhter Innenwiderstand wahrscheinlich)
```

Schwellenwerte für den Risikoindikator:
| Zyklen | Risikoklasse |
| :--- | :--- |
| < 200 | Niedrig |
| 200–400 | Mittel |
| > 400 | Erhöht |

### B. Last-Intensität (Stressfaktor)

`Wh_pro_Stunde = deliveredWhOverLifetime / supportPowerOnHours`

**Wichtig:** `supportPowerOnHours` (nicht `totalPowerOnHours`) verwenden, da er nur die Stunden mit echter Motorlast (Pedaldruck erkannt) misst. `totalPowerOnHours` enthält auch Stand-by-Zeit und verfälscht den Stressfaktor.

Für den Stressfaktor muss `BoschBike.driveUnit.supportPowerOnHours` mit `BoschBike.battery.deliveredWhOverLifetime` verknüpft werden.

| Wh/h | Bewertung |
| :--- | :--- |
| < 150 | Schonend (viel Eco/Tour, Flachland) |
| 150–250 | Normal |
| > 250 | Intensiv (viel Turbo/Sport, Bergfahrten) |

---

## 5. Implementierung in der App

### Daten-Mapping für PowerTube/PowerPack:
| Modellname enthält... | nominalCapacityWh |
| :--- | :--- |
| "750" | 750 |
| "625" | 625 |
| "545" | 545 |
| "500" | 500 |
| "400" | 400 |
| "300" | 300 |

### Beispielrechnung:
*   Akku: PowerTube 750
*   `deliveredWhOverLifetime`: 51.554 Wh
*   `totalChargeCycles`: 83,8
*   Rechnung: `51554 / (83,8 * 750) = 51554 / 62850 = 0,8202`
*   **Ergebnis: 82,0% (Gelb-Grün)**

---

## 6. Grenzen der Berechnung

*   **Historischer Mittelwert:** Die Formel misst die *durchschnittliche* Energieabgabe über alle Zyklen, nicht den aktuellen Zustand. Starke Früh- oder Spätdegradation wird verwischt.
*   **Lade-Tiefe-Bias:** Wer akkuschonend nur von 20% auf 80% lädt, hat weniger `deliveredWh` pro Zyklus. Der SOH erscheint schlechter, obwohl der Akku tatsächlich gesünder ist (gegenläufig zum "Bayerischen-Wald-Effekt").
*   **Der "Bayerische-Wald-Effekt":** Umgekehrt liefert ein Flachland-Eco-Fahrer pro Zyklus mehr Energie (weniger Wärmeverluste durch hohe Ströme). SOH erscheint besser als bei einem Mountainbiker im Turbo-Modus.
*   **Temperaturblindheit:** Keine Logdaten über Lager- oder Betriebstemperatur verfügbar. Akku bei -5°C liefert durch erhöhten Innenwiderstand weniger Wh, SOH erscheint schlechter – obwohl der Akku bei Raumtemperatur noch voll leistungsfähig wäre. Lagerung bei voller Ladung und hoher Temperatur (Garage im Sommer) kann laut Community-Berichten 5–10% SOH-Verlust über einen Sommer verursachen – bleibt in der Formel unsichtbar.
*   **Interne Bosch-BMS-Methode:** Bosch nutzt laut Community-Analysen wahrscheinlich eine Kombination aus Coulomb-Counting, Innenwiderstandsmessung und OCV-Korrektur. Das interne BMS-Ergebnis wird nicht direkt per API exponiert; die Formel ist eine externe Annäherung.

---

## 7. Methodenvergleich

| Methode | Qualität | Machbarkeit ohne BMS-Zugang |
|---|---|---|
| **Wh-Throughput** (diese Formel) | Mittel – historischer Durchschnitt | Gut – API liefert alle Daten |
| **Open Circuit Voltage (OCV)** | Hoch – direkte Kapazitätsschätzung | Nicht möglich |
| **Innenwiderstandsmessung (EIS)** | Sehr hoch – erfasst Alterung früh | Nicht möglich |
| **Modellbasiert (Kalman-Filter)** | Sehr hoch | Zu komplex ohne Labordaten |
| **Kapazitätstest (volle Entladung)** | Hoch – direkte Messung | Nicht praktikabel im Fahrbetrieb |

**Fazit:** Die Wh-Throughput-Methode ist für eine App ohne BMS-Direktzugang die einzig praktikable und trotzdem fundierte Option.

---
*Erstellt für M24 Bike Stats – aktualisiert 2026-04-06*
