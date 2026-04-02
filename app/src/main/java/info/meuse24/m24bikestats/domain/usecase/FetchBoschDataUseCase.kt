package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.model.BoschEndpoint
import info.meuse24.m24bikestats.domain.repository.AuthRepository
import info.meuse24.m24bikestats.domain.repository.BoschRepository

class FetchBoschDataUseCase(
    private val boschRepository: BoschRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(endpoint: BoschEndpoint): Result<String> {
        val token = authRepository.getAccessToken()
            ?: return Result.failure(IllegalStateException("Nicht angemeldet – bitte zuerst anmelden"))
        return boschRepository.fetch(endpoint, token)
    }
}
