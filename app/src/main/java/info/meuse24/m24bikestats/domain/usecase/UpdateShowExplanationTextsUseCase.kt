package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.repository.AppSettingsRepository

class UpdateShowExplanationTextsUseCase(
    private val repository: AppSettingsRepository,
) {
    suspend operator fun invoke(show: Boolean) {
        repository.updateShowExplanationTexts(show)
    }
}
