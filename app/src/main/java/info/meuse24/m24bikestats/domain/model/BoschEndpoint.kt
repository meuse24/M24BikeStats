package info.meuse24.m24bikestats.domain.model

/**
 * Bosch eBike Data Act API – Endpunkte für Smart System (BES3 / Flow-App).
 *
 * Base URL: https://api.bosch-ebike.com
 * Quelle: open-ebike/open-ebike-backend (Bruno-Collection)
 *
 * Getestet mit Cannondale Performance Line CX (Smart System):
 *   - smart-system Endpunkte → HTTP 200 ✓
 *   - ebike-system-2 Endpunkte → HTTP 400 (falscher System-Typ, entfernt)
 */
enum class BoschEndpoint(val label: String, val baseUrl: String, val path: String) {

    // --- Smart System (BES3) – bestätigt HTTP 200 ✓ ---

    SMART_ACTIVITIES(
        "Aktivitäten ✓",
        "https://api.bosch-ebike.com",
        "/activity/smart-system/v1/activities?limit=20&offset=0"
    ),
    SMART_BIKES(
        "Bikes ✓",
        "https://api.bosch-ebike.com",
        "/bike-profile/smart-system/v1/bikes"
    ),

    // --- Smart System – weitere Endpunkte (aus Bruno-Collection, noch ungetestet) ---

    SMART_ACTIVITY_DETAIL(
        "Aktivität Detail /details (ID ersetzen)",
        "https://api.bosch-ebike.com",
        "/activity/smart-system/v1/activities/ACTIVITY_ID/details"
    ),
    SMART_ACTIVITY_TRACK(
        "Aktivität GPS-Track (ID ersetzen)",
        "https://api.bosch-ebike.com",
        "/activity/smart-system/v1/activities/ACTIVITY_ID/track"
    ),
    SMART_BIKE_DETAIL(
        "Bike Detail (ID ersetzen)",
        "https://api.bosch-ebike.com",
        "/bike-profile/smart-system/v1/bikes/BIKE_ID"
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

    ;

    fun toRequest(
        activityId: String? = null,
        bikeId: String? = null,
    ): BoschRequest {
        val resolvedPath = path
            .replace("ACTIVITY_ID", activityId ?: "ACTIVITY_ID")
            .replace("BIKE_ID", bikeId ?: "BIKE_ID")

        return BoschRequest(
            label = label,
            baseUrl = baseUrl,
            path = resolvedPath,
            isLocalOnly = this == TOKEN_INFO,
        )
    }
}
