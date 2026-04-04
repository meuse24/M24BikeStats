package info.meuse24.m24bikestats.presentation.apitest

import info.meuse24.m24bikestats.api.BoschEndpoint
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
}
