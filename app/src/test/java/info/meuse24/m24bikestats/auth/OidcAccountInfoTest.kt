package info.meuse24.m24bikestats.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OidcAccountInfoTest {

    @Test
    fun `parse userinfo extracts profile fields from response body`() {
        val response = """
            HTTP 200

            {
              "sub": "user-subject",
              "email": "rider@example.com",
              "preferred_username": "rider"
            }
        """.trimIndent()

        val uiModel = parseOidcUserInfo(response)

        assertEquals("rider@example.com", uiModel?.email)
        assertEquals("rider", uiModel?.username)
        assertEquals("user-subject", uiModel?.subject)
    }

    @Test
    fun `parse discovery extracts endpoints and grant types from response body`() {
        val response = """
            HTTP 200

            {
              "issuer": "https://issuer.example.com",
              "authorization_endpoint": "https://issuer.example.com/auth",
              "token_endpoint": "https://issuer.example.com/token",
              "userinfo_endpoint": "https://issuer.example.com/userinfo",
              "jwks_uri": "https://issuer.example.com/jwks",
              "revocation_endpoint": "https://issuer.example.com/revoke",
              "introspection_endpoint": "https://issuer.example.com/introspect",
              "end_session_endpoint": "https://issuer.example.com/logout",
              "grant_types_supported": ["authorization_code", "refresh_token"]
            }
        """.trimIndent()

        val uiModel = parseOidcDiscoveryInfo(response)

        assertEquals("https://issuer.example.com", uiModel?.issuer)
        assertEquals("https://issuer.example.com/token", uiModel?.tokenEndpoint)
        assertEquals("https://issuer.example.com/userinfo", uiModel?.userInfoEndpoint)
        assertEquals(listOf("authorization_code", "refresh_token"), uiModel?.supportedGrantTypes)
    }

    @Test
    fun `parse userinfo returns null for non json responses`() {
        assertNull(parseOidcUserInfo("HTTP 404\n\nnot found"))
    }
}
