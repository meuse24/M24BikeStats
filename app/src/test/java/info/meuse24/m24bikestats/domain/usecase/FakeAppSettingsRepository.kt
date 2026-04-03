package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.model.AppSettings
import info.meuse24.m24bikestats.domain.model.CsvSeparator
import info.meuse24.m24bikestats.domain.repository.AppSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeAppSettingsRepository(
    initialSeparator: CsvSeparator = CsvSeparator.COMMA,
) : AppSettingsRepository {
    private val settingsState = MutableStateFlow(AppSettings(csvSeparator = initialSeparator))

    override fun observeSettings(): Flow<AppSettings> = settingsState.asStateFlow()

    override suspend fun getSettings(): AppSettings = settingsState.value

    override suspend fun updateCsvSeparator(separator: CsvSeparator) {
        settingsState.value = settingsState.value.copy(csvSeparator = separator)
    }
}
