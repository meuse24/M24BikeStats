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

Quelle: [open-ebike/open-ebike-backend](https://github.com/open-ebike/open-ebike-backend) (Bruno-Collection)

**Base URL:** `https://api.bosch-ebike.com`

### Smart System / BES3 (Flow-App, Bikes ab ~2022)

```
GET /activity/smart-system/v1/activities?limit=20&offset=0
GET /bike-profile/smart-system/v1/bikes
```

### eBike System 2 / BES2 (ältere Modelle)

```
GET /activity/ebike-system-2/v1/activities?limit=20&offset=0
GET /bike-profile/ebike-system-2/v1/bikes
```

### OIDC (bestätigt funktionierend)

```
GET https://p9.authz.bosch.com/auth/realms/obc/protocol/openid-connect/userinfo
→ HTTP 200, liefert sub, email, bosch-id, ebike-rider-id
```

---

## Sicherheitshinweise

- Kein Client Secret nötig – PKCE schützt den Flow kryptografisch
- `state`-Parameter wird von AppAuth automatisch gesetzt (CSRF-Schutz)
- Tokens werden in `EncryptedSharedPreferences` (AES-256-GCM, Android Keystore) gespeichert
- Token-Lebensdauer: 3600 Sekunden (1 Stunde) – Refresh Token für stille Verlängerung vorhanden

---

## Offene Punkte

- [ ] Bestätigen ob Smart System oder BES2 für Cannondale Performance Line CX korrekt
- [ ] Weitere Endpunkte aus Bruno-Collection auswerten (Diagnose, Remote Config etc.)
- [ ] Token-Refresh-Logik in `AuthManager` implementieren
