package info.meuse24.m24bikestats.domain.model

enum class BoschEndpoint(val label: String, val path: String) {
    BIKES("Bikes / Fahrräder", "/data-act/v1/bikes"),
    TRIPS("Trips / Fahrten", "/data-act/v1/trips"),
    TRIP_DETAILS("Trip-Details (Platzhalter-ID)", "/data-act/v1/trips/TRIP_ID"),
    FITNESS("Fitness-Daten", "/data-act/v1/fitness-data"),
    USER_PROFILE("Nutzerprofil", "/data-act/v1/user/profile"),
    USERINFO("OIDC UserInfo", "/auth/realms/obc/protocol/openid-connect/userinfo"),
}
