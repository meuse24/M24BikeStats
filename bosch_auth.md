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

## Bestätigte API-Endpunkte

**Base URL:** `https://api.bosch-ebike.com`

### Smart System / BES3 (Flow-App, Bikes ab ~2022)

```
GET /activity/smart-system/v1/activities?limit=20&offset=0   → HTTP 200
GET /activity/smart-system/v1/activities/{activityId}/details → HTTP 200
GET /bike-profile/smart-system/v1/bikes                       → HTTP 200
GET /bike-profile/smart-system/v1/bikes/{bikeId}             → HTTP 200
GET /activity/smart-system/v1/activities/{activityId}/track  → HTTP 404
```

Die vier `200`-Antworten sind mit echtem Token und echten IDs am **2. April 2026** live verifiziert.

### OIDC (bestätigt funktionierend)

```
GET https://p9.authz.bosch.com/auth/realms/obc/protocol/openid-connect/userinfo
→ HTTP 200, liefert u. a. sub, email, preferred_username

GET https://p9.authz.bosch.com/auth/realms/obc/.well-known/openid-configuration
→ HTTP 200, liefert authorization_endpoint, token_endpoint, userinfo_endpoint, jwks_uri,
  revocation_endpoint, introspection_endpoint, end_session_endpoint
```

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
- Die App nutzt diese Daten jetzt für die Aktivitätsdetailseite, die interne Trackansicht und den GPX-Export

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
