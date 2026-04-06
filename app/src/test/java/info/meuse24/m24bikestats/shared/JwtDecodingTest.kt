package info.meuse24.m24bikestats.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Base64

class JwtDecodingTest {

    @Test
    fun `decode jwt parts decodes url-safe header and payload`() {
        val token = buildJwt(
            header = """{"alg":"RS256","kid":"kid-1"}""",
            payload = """{"sub":"user-1"}""",
        )

        val decoded = decodeJwtParts(token)

        assertEquals("""{"alg":"RS256","kid":"kid-1"}""", decoded?.header)
        assertEquals("""{"sub":"user-1"}""", decoded?.payload)
    }

    @Test
    fun `decode jwt parts returns null for malformed token`() {
        assertNull(decodeJwtParts("invalid-token"))
    }

    private fun buildJwt(header: String, payload: String): String {
        val encodedHeader = Base64.getUrlEncoder().withoutPadding().encodeToString(header.toByteArray())
        val encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())
        return "$encodedHeader.$encodedPayload.signature"
    }
}
