package info.meuse24.m24bikestats.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Base64

class OidcCertificateInfoJwtTest {

    @Test
    fun `extract jwt key id reads kid from token header`() {
        val token = buildJwtWithKid("matching-kid")

        assertEquals("matching-kid", extractJwtKeyId(token))
    }

    @Test
    fun `extract jwt key id returns null for malformed token`() {
        assertNull(extractJwtKeyId("not-a-jwt"))
    }

    private fun buildJwtWithKid(kid: String): String {
        val header = """{"alg":"RS256","kid":"$kid"}"""
        val encodedHeader = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(header.toByteArray())
        return "$encodedHeader.payload.signature"
    }
}
