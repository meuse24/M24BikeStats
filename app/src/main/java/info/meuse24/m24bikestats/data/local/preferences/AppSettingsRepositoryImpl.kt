package info.meuse24.m24bikestats.data.local.preferences

import android.content.Context
import android.content.SharedPreferences
import info.meuse24.m24bikestats.domain.model.AppSettings
import info.meuse24.m24bikestats.domain.model.CsvExportFormat
import info.meuse24.m24bikestats.domain.model.CsvSeparator
import info.meuse24.m24bikestats.domain.model.toLegacyExportFormat
import info.meuse24.m24bikestats.domain.repository.AppSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppSettingsRepositoryImpl(
    context: Context,
) : AppSettingsRepository {

    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private val settingsState = MutableStateFlow(readSettings())

    override fun observeSettings(): Flow<AppSettings> = settingsState.asStateFlow()

    override suspend fun getSettings(): AppSettings = settingsState.value

    override suspend fun updateCsvExportFormat(format: CsvExportFormat) {
        preferences.edit()
            .putString(KEY_CSV_EXPORT_FORMAT, format.name)
            .remove(KEY_CSV_SEPARATOR)
            .apply()
        settingsState.value = settingsState.value.copy(csvExportFormat = format)
    }

    private fun readSettings(): AppSettings {
        val storedFormat = CsvExportFormat.fromStoredValue(
            preferences.getString(KEY_CSV_EXPORT_FORMAT, null),
        )
        val legacySeparator = CsvSeparator.fromStoredValue(
            preferences.getString(KEY_CSV_SEPARATOR, null),
        )
        return AppSettings(
            csvExportFormat = storedFormat ?: legacySeparator?.toLegacyExportFormat() ?: CsvExportFormat.SYSTEM_DEFAULT,
        )
    }

    private companion object {
        private const val PREFERENCES_NAME = "app_settings"
        private const val KEY_CSV_EXPORT_FORMAT = "csv_export_format"
        private const val KEY_CSV_SEPARATOR = "csv_separator"
    }
}
