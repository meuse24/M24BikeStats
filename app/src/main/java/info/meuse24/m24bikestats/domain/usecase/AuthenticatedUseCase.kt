package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.repository.AuthRepository

internal suspend fun <T> withValidAccessToken(
    authRepository: AuthRepository,
    block: suspend (String) -> Result<T>,
): Result<T> = authRepository.getValidAccessToken().fold(
    onSuccess = { token -> block(token) },
    onFailure = { error -> Result.failure(error) },
)
