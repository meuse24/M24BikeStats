package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.repository.AuthRepository

class ClearAuthenticationUseCase(
    private val authRepository: AuthRepository,
) {
    operator fun invoke() {
        authRepository.clearTokens()
    }
}
