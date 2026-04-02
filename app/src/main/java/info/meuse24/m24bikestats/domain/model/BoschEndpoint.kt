package info.meuse24.m24bikestats.domain.model

/**
 * Bekannte Bosch eBike Data Act API-Endpunkte.
 *
 * Da die offizielle Dokumentation keine öffentlichen Pfade listet, sind hier
 * mehrere Kombinationen (Base-URL × Pfad) eingetragen.
 * Nach dem ersten erfolgreichen Aufruf nicht benötigte Einträge entfernen.
 *
 * OIDC UserInfo (✓ bestätigt funktioniert) dient als Kontrollaufruf.
 */
enum class BoschEndpoint(val label: String, val baseUrl: String, val path: String) {

    // --- flow.bosch-ebike.com (Portal-Host – wahrscheinlichste API-Basis) ---
    BIKES_FLOW(
        "Bikes [flow / data-act/v1]",
        "https://flow.bosch-ebike.com",
        "/data-act/v1/bikes"
    ),
    TRIPS_FLOW(
        "Trips [flow / data-act/v1]",
        "https://flow.bosch-ebike.com",
        "/data-act/v1/trips"
    ),
    FITNESS_FLOW(
        "Fitness [flow / data-act/v1]",
        "https://flow.bosch-ebike.com",
        "/data-act/v1/fitness-data"
    ),

    // --- api.bosch-ebike.com – direkt ohne Präfix ---
    BIKES_API_DIRECT(
        "Bikes [api / direkt]",
        "https://api.bosch-ebike.com",
        "/bikes"
    ),
    TRIPS_API_DIRECT(
        "Trips [api / direkt]",
        "https://api.bosch-ebike.com",
        "/trips"
    ),

    // --- api.bosch-ebike.com – mit /data-act/v1/-Präfix (bisherige Annahme → 404) ---
    BIKES_API_DATAACT(
        "Bikes [api / data-act/v1]",
        "https://api.bosch-ebike.com",
        "/data-act/v1/bikes"
    ),

    // --- OIDC (bestätigt funktioniert) ---
    USERINFO(
        "OIDC UserInfo ✓",
        "https://p9.authz.bosch.com",
        "/auth/realms/obc/protocol/openid-connect/userinfo"
    ),
}
