package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.model.CloudSyncDetailMode
import info.meuse24.m24bikestats.domain.repository.AppSettingsRepository

class UpdateCloudSyncDetailModeUseCase(
    private val repository: AppSettingsRepository,
) {
    suspend operator fun invoke(mode: CloudSyncDetailMode) {
        repository.updateCloudSyncDetailMode(mode)
    }
}
