package info.meuse24.m24bikestats.support.apitest

import info.meuse24.m24bikestats.domain.repository.AuthRepository
import info.meuse24.m24bikestats.domain.usecase.withValidAccessToken

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
