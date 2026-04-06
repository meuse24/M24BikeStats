package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.model.BoschApiRequest
import info.meuse24.m24bikestats.domain.repository.AuthRepository
import info.meuse24.m24bikestats.domain.repository.BoschApiRepository

class FetchBoschDataUseCase(
    private val boschRepository: BoschApiRepository,
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(request: BoschApiRequest): Result<String> =
        withValidAccessToken(authRepository) { token ->
            boschRepository.fetch(request, token)
        }
}
