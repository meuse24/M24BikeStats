package info.meuse24.m24bikestats.domain.model

/**
 * Bosch eBike Data Act API – bestätigte Endpunkte aus open-ebike-backend (Bruno-Collection).
 * Basis: https://api.bosch-ebike.com
 *
 * BES3 = Smart System (Flow-App, neuere Bikes ab ~2022) → wahrscheinlich dein Bike
 * BES2 = eBike System 2 (ältere Modelle)
 */
enum class BoschEndpoint(val label: String, val baseUrl: String, val path: String) {

    // --- Smart System (BES3) – wahrscheinlich korrekt für Cannondale Flow-Portal ---
    SMART_ACTIVITIES(
        "Aktivitäten – Smart System ★",
        "https://api.bosch-ebike.com",
        "/activity/smart-system/v1/activities?limit=20&offset=0"
    ),
    SMART_BIKES(
        "Bikes – Smart System ★",
        "https://api.bosch-ebike.com",
        "/bike-profile/smart-system/v1/bikes"
    ),

    // --- eBike System 2 (BES2) – ältere Modelle ---
    BES2_ACTIVITIES(
        "Aktivitäten – eBike System 2",
        "https://api.bosch-ebike.com",
        "/activity/ebike-system-2/v1/activities?limit=20&offset=0"
    ),
    BES2_BIKES(
        "Bikes – eBike System 2",
        "https://api.bosch-ebike.com",
        "/bike-profile/ebike-system-2/v1/bikes"
    ),

    // --- OIDC (bestätigt ✓) ---
    USERINFO(
        "OIDC UserInfo ✓",
        "https://p9.authz.bosch.com",
        "/auth/realms/obc/protocol/openid-connect/userinfo"
    ),

    // --- Diagnose (kein HTTP-Call) ---
    TOKEN_INFO(
        "🔍 Token-Info (lokal dekodiert)",
        "",
        ""
    ),
    OIDC_DISCOVERY(
        "🔍 OIDC Discovery",
        "https://p9.authz.bosch.com",
        "/auth/realms/obc/.well-known/openid-configuration"
    ),
}
