package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.model.ExplanationTextsPromptTiming
import info.meuse24.m24bikestats.domain.repository.AppSettingsRepository

class UpdateExplanationTextsPromptTimingUseCase(
    private val repository: AppSettingsRepository,
) {
    suspend operator fun invoke(timing: ExplanationTextsPromptTiming) {
        repository.updateExplanationTextsPromptTiming(timing)
    }
}
