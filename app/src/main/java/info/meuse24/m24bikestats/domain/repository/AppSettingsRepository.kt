package info.meuse24.m24bikestats.domain.repository

import info.meuse24.m24bikestats.domain.model.AppSettings
import info.meuse24.m24bikestats.domain.model.CsvExportFormat
import kotlinx.coroutines.flow.Flow

interface AppSettingsRepository {
    fun observeSettings(): Flow<AppSettings>
    suspend fun getSettings(): AppSettings
    suspend fun updateCsvExportFormat(format: CsvExportFormat)
}
