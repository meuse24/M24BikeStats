package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.model.CsvExportFormat
import info.meuse24.m24bikestats.domain.repository.AppSettingsRepository

class UpdateCsvExportFormatUseCase(
    private val repository: AppSettingsRepository,
) {
    suspend operator fun invoke(format: CsvExportFormat) {
        repository.updateCsvExportFormat(format)
    }
}
