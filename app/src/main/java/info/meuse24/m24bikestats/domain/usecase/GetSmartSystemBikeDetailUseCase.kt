package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.model.BoschBike
import info.meuse24.m24bikestats.domain.repository.AuthRepository
import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemRepository

class GetSmartSystemBikeDetailUseCase(
    private val repository: BoschSmartSystemRepository,
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(bikeId: String): Result<BoschBike> {
        val token = authRepository.getValidAccessToken()
            .getOrElse { return Result.failure(it) }
        return repository.getBikeDetail(token, bikeId)
    }
}
