package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.repository.AppSettingsRepository

class RecordExplanationTextsPromptForegroundUsageUseCase(
    private val repository: AppSettingsRepository,
) {
    suspend operator fun invoke(durationMillis: Long) {
        repository.recordExplanationTextsPromptForegroundUsage(durationMillis)
    }
}
