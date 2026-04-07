package info.meuse24.m24bikestats.api

import info.meuse24.m24bikestats.domain.model.BoschApiRequest

/**
 * Bosch eBike Data Act API – Endpunkte für Smart System (BES3 / Flow-App).
 *
 * Base URL: https://api.bosch-ebike.com
 *
 * Diese App unterstützt ausschließlich Bosch Smart System.
 * Bosch eBike System 2 / eBike Connect ist bewusst nicht Teil dieser Liste.
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

    // --- Smart System – weitere bestätigte Detail-Endpunkte ---

    SMART_ACTIVITY_DETAIL(
        "Aktivität Detail /details (ID ersetzen)",
        "https://api.bosch-ebike.com",
        "/activity/smart-system/v1/activities/ACTIVITY_ID/details"
    ),
    SMART_BIKE_DETAIL(
        "Bike Detail (ID ersetzen)",
        "https://api.bosch-ebike.com",
        "/bike-profile/smart-system/v1/bikes/BIKE_ID"
    ),
    SMART_BIKE_PASS(
        "Bike Pass (Bike-ID ersetzen)",
        "https://api.bosch-ebike.com",
        "/bike-pass/smart-system/v1/bike-passes?bikeId=BIKE_ID"
    ),
    SMART_SERVICE_RECORDS(
        "Service Book (Bike-ID ersetzen)",
        "https://api.bosch-ebike.com",
        "/service-book/smart-system/v1/service-records?bikeId=BIKE_ID"
    ),
    SMART_REGISTRATIONS(
        "Registrierungen",
        "https://api.bosch-ebike.com",
        "/bike-registration/smart-system/v1/registrations"
    ),

    // --- OIDC (bestätigt ✓) ---

    USERINFO(
        "OIDC UserInfo ✓",
        "https://p9.authz.bosch.com",
        "/auth/realms/obc/protocol/openid-connect/userinfo"
    ),
    OIDC_CERTS(
        "OIDC JWKS /certs",
        "https://p9.authz.bosch.com",
        "/auth/realms/obc/protocol/openid-connect/certs"
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
    ): BoschApiRequest {
        require(this != TOKEN_INFO) { "TOKEN_INFO is local-only and does not map to BoschApiRequest" }

        val resolvedPath = path
            .replace("ACTIVITY_ID", activityId ?: "ACTIVITY_ID")
            .replace("BIKE_ID", bikeId ?: "BIKE_ID")

        return BoschApiRequest(
            label = label,
            baseUrl = baseUrl,
            path = resolvedPath,
        )
    }
}
