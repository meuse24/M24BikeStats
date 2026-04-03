package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.model.CsvSeparator
import info.meuse24.m24bikestats.domain.repository.AppSettingsRepository

class UpdateCsvSeparatorUseCase(
    private val repository: AppSettingsRepository,
) {
    suspend operator fun invoke(separator: CsvSeparator) {
        repository.updateCsvSeparator(separator)
    }
}
