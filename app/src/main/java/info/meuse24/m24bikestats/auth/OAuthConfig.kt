package info.meuse24.m24bikestats.auth

import android.net.Uri
import net.openid.appauth.AuthorizationServiceConfiguration

object OAuthConfig {

    // Öffentlicher Client (kein Secret) – PKCE übernimmt die Absicherung
    const val CLIENT_ID = "euda-2c8d2760-d459-40aa-adc9-6eb7a8b91bd7"

    const val REDIRECT_URI = "m24bikestats://oauth-callback"

    // Bestätigte Endpunkte (Keycloak-Realm "obc" auf p9.authz.bosch.com)
    private const val AUTHORIZATION_ENDPOINT =
        "https://p9.authz.bosch.com/auth/realms/obc/protocol/openid-connect/auth"
    private const val TOKEN_ENDPOINT =
        "https://p9.authz.bosch.com/auth/realms/obc/protocol/openid-connect/token"

    // "openid" + "offline_access" sind OIDC-Standard; weitere Data-Act-Scopes
    // aus https://portal.bosch-ebike.com/data-act/app#/introduction entnehmen
    val SCOPES = listOf("openid", "offline_access")

    val serviceConfiguration = AuthorizationServiceConfiguration(
        Uri.parse(AUTHORIZATION_ENDPOINT),
        Uri.parse(TOKEN_ENDPOINT)
    )
}
