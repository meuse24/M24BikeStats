package info.meuse24.m24bikestats.presentation.apitest

import info.meuse24.m24bikestats.api.BoschEndpoint
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiTestResponseDiagnosticsTest {

    @Test
    fun `bike detail diagnostics flag serviceDue as unused field`() {
        val diagnostics = buildApiTestResponseDiagnostics(
            endpoint = BoschEndpoint.SMART_BIKE_DETAIL,
            response = """
                HTTP 200 OK

                {
                  "id": "bike-1",
                  "language": "de",
                  "driveUnit": {
                    "productName": "Drive Unit Performance Line CX",
                    "walkAssistConfiguration": {
                      "isEnabled": true,
                      "maximumSpeed": 4.0
                    },
                    "odometer": 6336824.0,
                    "powerOnTime": {
                      "total": 867,
                      "withMotorSupport": 867
                    },
                    "activeAssistModes": [
                      { "name": "Tour+", "reachableRange": 75.0 }
                    ]
                  },
                  "batteries": [],
                  "serviceDue": {}
                }
            """.trimIndent(),
        )

        assertTrue(diagnostics?.unusedFieldLines?.any { it.contains("serviceDue = {}") } == true)
    }

    @Test
    fun `extractJsonBody handles windows style line endings`() {
        val jsonBody = extractJsonBody("HTTP 200 OK\r\n\r\n{\r\n  \"email\": \"rider@example.com\"\r\n}")

        assertTrue(jsonBody?.contains("\"email\": \"rider@example.com\"") == true)
    }

    @Test
    fun `oidc discovery diagnostics do not flag actively mapped endpoints as unused`() {
        val diagnostics = buildApiTestResponseDiagnostics(
            endpoint = BoschEndpoint.OIDC_DISCOVERY,
            response = """
                HTTP 200 OK

                {
                  "issuer": "https://issuer.example.com",
                  "authorization_endpoint": "https://issuer.example.com/auth",
                  "token_endpoint": "https://issuer.example.com/token",
                  "userinfo_endpoint": "https://issuer.example.com/userinfo",
                  "jwks_uri": "https://issuer.example.com/jwks",
                  "revocation_endpoint": "https://issuer.example.com/revoke",
                  "introspection_endpoint": "https://issuer.example.com/introspect",
                  "end_session_endpoint": "https://issuer.example.com/logout",
                  "grant_types_supported": ["authorization_code"]
                }
            """.trimIndent(),
        )

        val unusedLines = diagnostics?.unusedFieldLines.orEmpty()
        assertFalse(unusedLines.any { it.contains("jwks_uri") })
        assertFalse(unusedLines.any { it.contains("revocation_endpoint") })
        assertFalse(unusedLines.any { it.contains("introspection_endpoint") })
        assertFalse(unusedLines.any { it.contains("end_session_endpoint") })
    }
}
