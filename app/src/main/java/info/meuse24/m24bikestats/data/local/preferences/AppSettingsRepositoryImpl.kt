package info.meuse24.m24bikestats.data.local.preferences

import android.content.Context
import android.content.SharedPreferences
import info.meuse24.m24bikestats.domain.model.AppSettings
import info.meuse24.m24bikestats.domain.model.CsvSeparator
import info.meuse24.m24bikestats.domain.model.CsvSeparatorDefaults
import info.meuse24.m24bikestats.domain.repository.AppSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class AppSettingsRepositoryImpl(
    context: Context,
    private val localeProvider: () -> Locale = Locale::getDefault,
) : AppSettingsRepository {

    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private val settingsState = MutableStateFlow(readSettings())

    override fun observeSettings(): Flow<AppSettings> = settingsState.asStateFlow()

    override suspend fun getSettings(): AppSettings = settingsState.value

    override suspend fun updateCsvSeparator(separator: CsvSeparator) {
        preferences.edit()
            .putString(KEY_CSV_SEPARATOR, separator.name)
            .apply()
        settingsState.value = settingsState.value.copy(csvSeparator = separator)
    }

    private fun readSettings(): AppSettings {
        val defaultSeparator = CsvSeparatorDefaults.forLocale(localeProvider())
        val storedSeparator = CsvSeparator.fromStoredValue(
            preferences.getString(KEY_CSV_SEPARATOR, null),
        )
        return AppSettings(csvSeparator = storedSeparator ?: defaultSeparator)
    }

    private companion object {
        private const val PREFERENCES_NAME = "app_settings"
        private const val KEY_CSV_SEPARATOR = "csv_separator"
    }
}
