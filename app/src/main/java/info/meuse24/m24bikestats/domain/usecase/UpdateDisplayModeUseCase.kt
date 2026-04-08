package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.model.DisplayMode
import info.meuse24.m24bikestats.domain.repository.AppSettingsRepository

class UpdateDisplayModeUseCase(
    private val repository: AppSettingsRepository,
) {
    suspend operator fun invoke(mode: DisplayMode) {
        repository.updateDisplayMode(mode)
    }
}
