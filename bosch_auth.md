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
  &scope=openid%20offline_access
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
GET https://api.bosch-ebike.com/...
Authorization: Bearer <access_token>
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

## Sicherheitshinweise

- Kein Client Secret nötig – PKCE schützt den Flow kryptografisch
- `state`-Parameter wird von AppAuth automatisch gesetzt (CSRF-Schutz)
- Tokens werden in `EncryptedSharedPreferences` (AES-256-GCM, Android Keystore) gespeichert

---

## Offene Punkte

- [ ] Genaue Scope-Werte aus `https://portal.bosch-ebike.com/data-act/app#/introduction` ermitteln
