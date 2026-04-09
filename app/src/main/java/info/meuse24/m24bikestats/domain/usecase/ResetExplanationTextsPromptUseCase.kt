package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.repository.AppSettingsRepository

class ResetExplanationTextsPromptUseCase(
    private val repository: AppSettingsRepository,
) {
    suspend operator fun invoke() {
        repository.resetExplanationTextsPrompt()
    }
}
