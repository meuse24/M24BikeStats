package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.shared.TokenInfoFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetCurrentAccessTokenInfoUseCaseTest {

    @Test
    fun `formats decoded header and payload`() {
        val token = buildToken(
            """{"alg":"HS256"}""",
            """{"sub":"user-1"}""",
        )
        val useCase = GetCurrentAccessTokenInfoUseCase(
            authRepository = FakeAuthRepository(Result.success(token)),
        )

        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(
            "${TokenInfoFormat.HEADER_MARKER}\n{\"alg\":\"HS256\"}\n\n${TokenInfoFormat.PAYLOAD_MARKER}\n{\"sub\":\"user-1\"}",
            result.getOrThrow(),
        )
    }

    @Test
    fun `fails when token is missing`() {
        val useCase = GetCurrentAccessTokenInfoUseCase(
            authRepository = FakeAuthRepository(Result.failure(IllegalStateException("no token"))),
        )

        val result = useCase()

        assertTrue(result.isFailure)
        assertEquals("Kein Access Token verfügbar", result.exceptionOrNull()?.message)
    }

    private fun buildToken(
        header: String,
        payload: String,
    ): String {
        val encoder = java.util.Base64.getUrlEncoder().withoutPadding()
        val encodedHeader = encoder.encodeToString(header.toByteArray())
        val encodedPayload = encoder.encodeToString(payload.toByteArray())
        return "$encodedHeader.$encodedPayload.signature"
    }
}
