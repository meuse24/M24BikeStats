package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.repository.AppSettingsRepository

class ObserveAppSettingsUseCase(
    private val repository: AppSettingsRepository,
) {
    operator fun invoke() = repository.observeSettings()
}
