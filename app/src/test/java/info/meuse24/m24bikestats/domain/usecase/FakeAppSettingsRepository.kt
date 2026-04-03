package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.model.AppSettings
import info.meuse24.m24bikestats.domain.model.CsvExportFormat
import info.meuse24.m24bikestats.domain.repository.AppSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeAppSettingsRepository(
    initialFormat: CsvExportFormat = CsvExportFormat.SYSTEM_DEFAULT,
) : AppSettingsRepository {
    private val settingsState = MutableStateFlow(AppSettings(csvExportFormat = initialFormat))

    override fun observeSettings(): Flow<AppSettings> = settingsState.asStateFlow()

    override suspend fun getSettings(): AppSettings = settingsState.value

    override suspend fun updateCsvExportFormat(format: CsvExportFormat) {
        settingsState.value = settingsState.value.copy(csvExportFormat = format)
    }
}
