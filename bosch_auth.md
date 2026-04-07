# Bosch eBike Portal – Authentifizierung (OAuth2)

## Client-Anwendung

| Feld                    | Wert                                              |
|-------------------------|---------------------------------------------------|
| Client Application Name | M24BikeStats                                      |
| Login URL               | https://github.com/meuse24/M24BikeStats           |
| Redirect URI            | `m24bikestats://oauth-callback`                   |
| Confidential Client     | Nein (öffentlicher Client – kein Secret)          |
| Client ID               | `euda-2c8d2760-d459-40aa-adc9-6eb7a8b91bd7`      |

---

## OAuth2-Flow

Bosch nutzt den **Authorization Code Flow + PKCE** (RFC 6749 + RFC 7636) – ohne Client Secret.

## Hinweis vor dem Login

Wenn ein Nutzer seine über Bosch bereitgestellten Daten an die App weitergeben möchte, soll vor dem Login auf das Flow-Portal verwiesen werden:

- Flow-Portal: https://flow.bosch-ebike.com/data-act

Der Hinweis ist inzwischen direkt im Login-Screen der App ergänzt.

### Schritt 1 – Authorization Request (mit PKCE)

AppAuth generiert `code_verifier` und `code_challenge` automatisch.

```
GET https://p9.authz.bosch.com/auth/realms/obc/protocol/openid-connect/auth
  ?response_type=code
  &client_id=euda-2c8d2760-d459-40aa-adc9-6eb7a8b91bd7
  &redirect_uri=m24bikestats://oauth-callback
  &scope=openid%20offline_access%20profile%20email
  &state=<zufälliger_wert>
  &code_challenge=<S256-hash-des-verifiers>
  &code_challenge_method=S256
```

### Schritt 2 – Callback

Nach Zustimmung durch den Nutzer liefert Bosch einen Authorization Code an die Redirect URI:

```
m24bikestats://oauth-callback?code=<authorization_code>&state=<state>
```

### Schritt 3 – Token-Austausch

Kein `client_secret` – stattdessen wird der `code_verifier` mitgeschickt:

```
POST https://p9.authz.bosch.com/auth/realms/obc/protocol/openid-connect/token
Content-Type: application/x-www-form-urlencoded

grant_type=authorization_code
&code=<authorization_code>
&redirect_uri=m24bikestats://oauth-callback
&client_id=euda-2c8d2760-d459-40aa-adc9-6eb7a8b91bd7
&code_verifier=<ursprünglicher_code_verifier>
```

**Antwort:**
```json
{
  "access_token": "...",
  "token_type": "Bearer",
  "expires_in": 3600,
  "refresh_token": "..."
}
```

### Schritt 4 – API-Zugriff

```
GET https://api.bosch-ebike.com/<endpunkt>
Authorization: Bearer <access_token>
Accept: application/json
```

### Schritt 5 – Token erneuern

```
POST https://p9.authz.bosch.com/auth/realms/obc/protocol/openid-connect/token
Content-Type: application/x-www-form-urlencoded

grant_type=refresh_token
&refresh_token=<refresh_token>
&client_id=euda-2c8d2760-d459-40aa-adc9-6eb7a8b91bd7
```

### Schritt 6 – Logout

Die App versucht beim Abmelden einen OIDC-Logout gegen den Bosch-End-Session-Endpunkt:

```
GET https://p9.authz.bosch.com/auth/realms/obc/protocol/openid-connect/logout
  ?id_token_hint=<id_token>
  &post_logout_redirect_uri=m24bikestats://oauth-callback
  &state=<zufälliger_wert>
```

Zusätzlich werden die lokal gespeicherten Tokens gelöscht. Wenn danach trotzdem ein automatischer Login passiert, kommt dieser in der Regel aus der bestehenden Session des Systembrowsers bzw. der Bosch-SingleKey-Anmeldung außerhalb der App.

---

## JWT-Token – bestätigte Claims

Aus einem realen Token dekodiert (Stand: April 2026):

```json
{
  "iss": "https://p9.authz.bosch.com/auth/realms/obc",
  "aud": ["api-bosch-ebike", "account"],
  "azp": "euda-2c8d2760-d459-40aa-adc9-6eb7a8b91bd7",
  "scope": "offline_access euda:read roles profile email openid web-origins",
  "realm_access": {
    "roles": ["offline_access", "euda-account-developer", "uma_authorization", "user"]
  },
  "bosch-id": "<uuid>",
  "ebike-rider-id": "<uuid>",
  "preferred_username": "skid.<bosch-id>"
}
```

**Wichtige Claims:**
| Claim | Bedeutung |
|---|---|
| `aud: api-bosch-ebike` | Token ist für `api.bosch-ebike.com` ausgestellt |
| `scope: euda:read` | Scope für EU Data Act Read-Zugriff – automatisch erteilt |
| `ebike-rider-id` | Rider-ID, wird ggf. für Detailabfragen benötigt |
| `bosch-id` | Bosch SingleKey ID des Nutzers |

---

## Verifizierte Endpunkte und Resultate

Stand dieser Übersicht:

- Smart-System- und OIDC-Endpunkte gegen echten Account, echte `activityId` und echte `bikeId`
- Batch-Report `bosch-api-test-run-all (1).txt` vom **4. April 2026**
- älterer Einzelbefund vom **2. April 2026** bleibt konsistent, wurde aber durch den neueren Batch-Run ergänzt

### Smart System / BES3

**Base URL:** `https://api.bosch-ebike.com`

| Endpunkt | Status | Aktuelle App-Nutzung | Zweck | Zusammenfassung des Resultats |
|---|---|---|---|---|
| `GET /activity/smart-system/v1/activities?limit=20&offset=0` | HTTP 200 | Ja | Liste der Aktivitäten eines Riders mit Paging | Liefert `pagination`, `activitySummaries` sowie `links.self`, `links.next`, `links.first`, `links.last`; zentrale Quelle für Dashboard, History und Cloud-Sync |
| `GET /activity/smart-system/v1/activities/{activityId}/details` | HTTP 200 | Ja | Detaillierte Punktserie einer Aktivität | Liefert `activityDetails` mit Distanz, Höhe, Geschwindigkeit, Kadenz, Rider-Power und Koordinaten; praktisch der wichtigste Aktivitäts-Endpunkt, weil Track und Metriken gemeinsam kommen |
| `GET /activity/smart-system/v1/activities/{activityId}/track` | HTTP 404 | Nein | Vermuteter reiner GPS-Track-Endpunkt | Für den getesteten Smart-System-Account nicht vorhanden; `/details` deckt den fachlichen Bedarf bereits weitgehend ab |
| `GET /bike-profile/smart-system/v1/bikes` | HTTP 200 | Ja | Liste der Bikes des Accounts | Liefert Bike-Objekte mit Drive Unit, Batteries, Head Unit, Active Assist Modes und Basis-Metadaten; zentrale Quelle für Bike-Liste und initiale Synchronisation |
| `GET /bike-profile/smart-system/v1/bikes/{bikeId}` | HTTP 200 | Ja | Detaildaten zu einem konkreten Bike | Liefert die produktiv nutzbaren Zusatzinfos wie Walk-Assist, Einschaltzeit, Reichweiten pro Modus, Batterie-Lebensdaten und Gerätekomponenten |
| `GET /bike-pass/smart-system/v1/bike-passes?bikeId={bikeId}` | HTTP 200 | Ja | Bike-Pass und Theft-Logs eines Bikes | Liefert Rahmennummer, Positionsangabe, Freitextmerkmale und ggf. Theft-Logs; fachlich oft optional oder leer |
| `GET /service-book/smart-system/v1/service-records?bikeId={bikeId}` | HTTP 200 | Ja | Digitales Service Book pro Bike | Liefert Servicehistorie, Händlerdaten, Software-Updates und Batterie-Messungen; kann legitimerweise `0` Records zurückgeben |
| `GET /bike-registration/smart-system/v1/registrations` | HTTP 200 | Ja | Bike- und Komponentenregistrierungen des Accounts | Liefert Registrierungszeitpunkte sowie Typ-, Serien- und Teilenummern registrierter Komponenten; wird in der App auf das geöffnete Bike gefiltert |
| `GET /activity/smart-system/v1/activities/{activityId}/statistics` | HTTP 404 | Nein | Vermutete aggregierte Aktivitätsstatistik | Im getesteten Account nicht vorhanden; die Kennzahlen aus `activitySummaries` und `activityDetails` bleiben damit die belastbare Datenquelle |
| `GET /activity/smart-system/v1/activities/{activityId}/power-share` | HTTP 404 | Nein | Vermutete Aufteilung von Fahrer- und Motorleistung | Nicht vorhanden; es gibt im aktuellen Vertrag keinen separat erreichbaren Power-Share-Endpunkt |
| `GET /activity/smart-system/v1/activities/{activityId}/riding-mode-usage` | HTTP 404 | Nein | Vermutete Verteilung der Unterstützungsmodi pro Aktivität | Nicht vorhanden; falls Bosch diese Daten intern hat, sind sie über diesen Pfad für den getesteten Account nicht exposed |
| `GET /activity/smart-system/v1/activities/{activityId}/braking-statistics` | HTTP 404 | Nein | Vermutete Bremsstatistik einer Aktivität | Nicht vorhanden; kein nutzbarer Endpunkt für Bremsereignisse oder Bremsintensität gefunden |
| `GET /activity/smart-system/v1/statistics/annual` | HTTP 404 | Nein | Vermutete Jahresstatistik über alle Aktivitäten | Nicht vorhanden; aggregierte Jahresansichten müssen daher aus Rohdaten in der App selbst berechnet werden |
| `GET /bike-profile/smart-system/v1/bikes/{bikeId}/model` | HTTP 404 | Nein | Vermuteter Modell-/Produktstammdaten-Endpunkt | Nicht vorhanden; Modellinformationen müssen aktuell aus dem Bike-Detail und den Komponentennamen abgeleitet werden |

### Paging über Activity-Links

Die Aktivitätenliste liefert einen stabil wirkenden Paging-Vertrag:

- `links.self`
- `links.first`
- `links.next`
- `links.prev`
- `links.last`

Im Batch-Run wurden mehrere dieser verlinkten URLs direkt nachgeladen. Die aufgelösten Requests wie `offset=20`, `offset=40`, `offset=420` und `offset=440` lieferten jeweils `HTTP 200`. Daraus ergibt sich:

- Paging über `limit` und `offset` ist funktional verifiziert
- die serverseitig gelieferten Link-URLs sind konsistent und nutzbar
- `next` und `prev` funktionieren auch für tiefe Seiten im Verlauf
- `last` funktioniert und zeigt das historische Ende der Aktivitätenliste

Für die App ist das relevant, weil damit sowohl klassisches Paging per Parametern als auch ein späterer Wechsel auf servergelieferte Links möglich ist.

### OIDC und Auth-Infrastruktur

**Base URL:** `https://p9.authz.bosch.com`

| Endpunkt | Status | Aktuelle App-Nutzung | Zweck | Zusammenfassung des Resultats |
|---|---|---|---|---|
| `GET /auth/realms/obc/protocol/openid-connect/userinfo` | HTTP 200 | Ja | Standard-OIDC-UserInfo für den eingeloggten Nutzer | Liefert u. a. `sub`, `email_verified`, `preferred_username`, `email`; wird jetzt in der Kontoansicht zusätzlich zu den Bike-Daten angezeigt |
| `GET /auth/realms/obc/protocol/openid-connect/certs` | HTTP 200 | Ja | JWKS-Endpoint für die Signaturschlüssel des OIDC-Providers | Liefert `keys[]` mit `kid`, `kty`, `alg`, `use`, `x5c`, Thumbprints; für Token-Validierung und Zertifikatsanzeige gut nutzbar |
| `GET /auth/realms/obc/.well-known/openid-configuration` | HTTP 200 | Ja | Discovery-Dokument des OIDC-Providers | Liefert die komplette OIDC-Metadatenbasis wie `authorization_endpoint`, `token_endpoint`, `userinfo_endpoint`, `jwks_uri`, `revocation_endpoint`, `introspection_endpoint`, `end_session_endpoint`; wird jetzt in der Kontoansicht transparent mit angezeigt |
| `TOKEN_INFO` lokal dekodiert | lokal, kein HTTP-Call | Ja, aber nur Diagnose | Diagnoseansicht für JWT-Header und JWT-Payload | Dekodiert das aktuelle Access Token lokal und zeigt u. a. `iss`, `aud`, `scope`, `bosch-id`, `ebike-rider-id`, `preferred_username`; hilfreich zur Analyse von Claims und Schlüssel-IDs |

### Fachliche Schlussfolgerungen aus dem aktuellen Stand

- Der belastbare Smart-System-Vertrag besteht derzeit aus Aktivitätenliste, Aktivitätsdetails, Bikeliste und Bike-Detail
- Zusätzlich produktiv nutzbar sind Bike Pass, Service Book und Registrierungen, sofern Bosch für Konto und Bike tatsächlich Daten liefert
- `/activities/{activityId}/details` ist funktional wertvoller als `/track`, weil dort bereits Trackpunkte und Metrikwerte gemeinsam vorliegen
- `Service Book = 0` ist ein normaler fachlicher Zustand und kein technischer Fehler
- Die `404`-Antworten auf `statistics`, `power-share`, `riding-mode-usage`, `braking-statistics`, `statistics/annual` und `bike model` sprechen dafür, dass diese Pfade entweder intern, veraltet oder für den getesteten Account nicht freigeschaltet sind
- Aggregationen wie Jahresstatistik, Modusnutzung oder Bremsauswertung müssen aktuell aus den bestätigten Rohdaten in der App selbst berechnet werden
- Die Detaildaten enthalten real teils `latitude=0.0` und `longitude=0.0` sowie aufeinanderfolgende Duplikate derselben Koordinate; vor Karten-, Profil- oder GPX-Nutzung ist daher eine Bereinigung sinnvoll

### Verifizierte Antwortstrukturen

`GET /activity/smart-system/v1/activities?limit=20&offset=0`

```json
{
  "pagination": {
    "total": 453,
    "offset": 0,
    "limit": 20
  },
  "activitySummaries": [
    {
      "id": "<activity-id>",
      "startTime": "2026-03-27T07:12:57Z",
      "endTime": "2026-03-27T19:21:11Z",
      "timeZone": "Europe/Vienna",
      "durationWithoutStops": 2201,
      "title": "Lauterach Rundfahrt",
      "bikeId": "<bike-id>",
      "startOdometer": 6324521,
      "distance": 13555,
      "speed": { "average": 22.17, "maximum": 42.15 },
      "cadence": { "average": 66.0, "maximum": 103.0 },
      "riderPower": { "average": 102.0, "maximum": 444.0 },
      "elevation": { "gain": 75, "loss": 80 },
      "caloriesBurned": 173.0
    }
  ]
}
```

Pagination ist funktional relevant:
- `total` beschreibt die Gesamtzahl der Aktivitäten
- `offset` und `limit` steuern weitere Seiten
- `links.next` und `links.last` sind zusätzlich vorhanden und können künftig als robusterer Paging-Vertrag genutzt werden
- Die App nutzt diese Parameter jetzt im Dashboard für `Mehr Aktivitäten laden`

`GET /activity/smart-system/v1/activities/{activityId}/details`

```json
{
  "activityDetails": [
    {
      "distance": 4.0,
      "altitude": 0.0,
      "speed": 3.0,
      "cadence": 0.0,
      "latitude": 47.48244,
      "longitude": 9.71577,
      "riderPower": 0.0
    }
  ]
}
```

Diese Detailpunkte sind funktional relevant:
- `distance` beschreibt die kumulierte Distanz entlang der Aktivität
- `latitude` und `longitude` liefern die Trackpunkte
- `altitude`, `speed`, `cadence` und `riderPower` liefern punktbezogene Metrikdaten
- `0.0/0.0`-Koordinaten und redundante Wiederholungen derselben Koordinate kommen real vor und sollten gefiltert bzw. komprimiert werden
- Die App nutzt diese Daten jetzt für die Aktivitätsdetailseite, eine MapLibre/OpenFreeMap-Kartenansicht mit Auto-Fit, Linienprofile und einen GPX-Exportdialog mit Vorschau

`GET /bike-profile/smart-system/v1/bikes/{bikeId}`

```json
{
  "id": "<bike-id>",
  "createdAt": "2024-06-14T12:45:12.123452Z",
  "driveUnit": {
    "serialNumber": "<serial>",
    "partNumber": "EB11100000",
    "productName": "Drive Unit Performance Line CX",
    "walkAssistConfiguration": {
      "isEnabled": true,
      "maximumSpeed": 4.0
    },
    "odometer": 6336824.0,
    "rearWheelCircumferenceUser": 2260.0,
    "maximumAssistanceSpeed": 27.4,
    "activeAssistModes": [
      { "name": "A100M00040", "reachableRange": 97.0 }
    ],
    "powerOnTime": {
      "total": 867,
      "withMotorSupport": 867
    }
  },
  "remoteControl": {
    "serialNumber": "<serial>",
    "partNumber": "EB1310000E",
    "productName": "LED Remote"
  },
  "batteries": [
    {
      "serialNumber": "<serial>",
      "partNumber": "EB1210000X",
      "productName": "PowerTube 750",
      "deliveredWhOverLifetime": 51554,
      "chargeCycles": {
        "total": 83.8,
        "onBike": 79.2,
        "offBike": 4.5
      }
    }
  ],
  "language": "de",
  "serviceDue": {},
  "headUnit": {
    "serialNumber": "<serial>",
    "partNumber": "EB13100003",
    "productName": "Kiox 300"
  }
}
```

Diese Bike-Detaildaten sind zusätzlich produktiv nutzbar:

- `walkAssistConfiguration.isEnabled`
- `walkAssistConfiguration.maximumSpeed`
- `powerOnTime.total`
- `powerOnTime.withMotorSupport`
- `activeAssistModes[].reachableRange`
- `rearWheelCircumferenceUser`

Für die UI besonders wertvoll sind:

- Walk-Assist-Status
- Einschaltzeit gesamt / mit Motorunterstützung
- Assist-Reichweiten pro Modus

---

## Sicherheitshinweise

- Kein Client Secret nötig – PKCE schützt den Flow kryptografisch
- `state`-Parameter wird von AppAuth automatisch gesetzt (CSRF-Schutz)
- Tokens werden in `EncryptedSharedPreferences` (AES-256-GCM, Android Keystore) gespeichert
- Token-Lebensdauer: 3600 Sekunden (1 Stunde) – Refresh Token für stille Verlängerung vorhanden
- Die App soll vor API-Aufrufen den Access Token automatisch per Refresh Token erneuern
- Beim Logout versucht die App zusätzlich, die OIDC-Session beim Provider zu beenden

---

## Offene Punkte

- [x] Smart System für Cannondale Performance Line CX bestätigt
- [x] Bike-Detail-Endpunkt mit echter `bikeId` bestätigt
- [x] Fachliche Dashboard-UI für Aktivitäten und Bikes auf Basis der verifizierten JSON-Strukturen
- [x] Aktivitäten-Paginierung über `limit`/`offset`
- [x] Aktivitäts-Detailpunkte über `/activities/{activityId}/details` bestätigt
- [x] Track-/Kartenansicht auf Basis der bestätigten Detailpunkte ergänzt
- [x] GPX-Export des vollständigen Tracks über Android Share Sheet ergänzt
- [x] Logout gegen Bosch OIDC-End-Session erfolgreich getestet
- [ ] Alternativen für Activity-Detail- und Track-Endpunkte recherchieren
