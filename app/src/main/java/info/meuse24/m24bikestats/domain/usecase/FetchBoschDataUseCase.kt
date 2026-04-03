package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.model.BoschEndpoint
import info.meuse24.m24bikestats.domain.model.BoschRequest
import info.meuse24.m24bikestats.domain.repository.AuthRepository
import info.meuse24.m24bikestats.domain.repository.BoschRepository

class FetchBoschDataUseCase(
    private val boschRepository: BoschRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(endpoint: BoschEndpoint): Result<String> {
        return invoke(endpoint.toRequest())
    }

    suspend operator fun invoke(request: BoschRequest): Result<String> =
        withValidAccessToken(authRepository) { token ->
            boschRepository.fetch(request, token)
        }
}
