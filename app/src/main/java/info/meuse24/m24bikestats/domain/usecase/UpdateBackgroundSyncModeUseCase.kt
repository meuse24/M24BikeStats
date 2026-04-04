package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.model.BackgroundSyncMode
import info.meuse24.m24bikestats.domain.repository.AppSettingsRepository

class UpdateBackgroundSyncModeUseCase(
    private val repository: AppSettingsRepository,
) {
    suspend operator fun invoke(mode: BackgroundSyncMode) {
        repository.updateBackgroundSyncMode(mode)
    }
}
