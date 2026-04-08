package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.repository.AuthRepository
import info.meuse24.m24bikestats.shared.TokenInfoFormat
import info.meuse24.m24bikestats.shared.decodeJwtParts

class GetCurrentAccessTokenInfoUseCase(
    private val authRepository: AuthRepository,
) {
    operator fun invoke(): Result<String> = runCatching {
        val token = authRepository.getAccessToken()
            ?: error("Kein Access Token verfügbar")
        val parts = decodeJwtParts(token)
            ?: error("Kein gültiges JWT")
        TokenInfoFormat.format(header = parts.header, payload = parts.payload)
    }
}
