package info.meuse24.m24bikestats.data.local.preferences

import android.content.Context
import android.content.SharedPreferences
import info.meuse24.m24bikestats.domain.model.AppSettings
import info.meuse24.m24bikestats.domain.model.BackgroundSyncMode
import info.meuse24.m24bikestats.domain.model.CloudSyncDetailMode
import info.meuse24.m24bikestats.domain.model.CsvExportFormat
import info.meuse24.m24bikestats.domain.model.CsvSeparator
import info.meuse24.m24bikestats.domain.model.DisplayMode
import info.meuse24.m24bikestats.domain.model.ExplanationTextsPromptTiming
import info.meuse24.m24bikestats.domain.model.toLegacyExportFormat
import info.meuse24.m24bikestats.domain.repository.AppSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppSettingsRepositoryImpl(
    private val context: Context,
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

    override suspend fun updateCloudSyncDetailMode(mode: CloudSyncDetailMode) {
        preferences.edit()
            .putString(KEY_CLOUD_SYNC_DETAIL_MODE, mode.name)
            .apply()
        settingsState.value = settingsState.value.copy(cloudSyncDetailMode = mode)
    }

    override suspend fun updateBackgroundSyncMode(mode: BackgroundSyncMode) {
        preferences.edit()
            .putString(KEY_BACKGROUND_SYNC_MODE, mode.name)
            .apply()
        settingsState.value = settingsState.value.copy(backgroundSyncMode = mode)
    }

    override suspend fun updateDisplayMode(mode: DisplayMode) {
        preferences.edit()
            .putString(KEY_DISPLAY_MODE, mode.name)
            .apply()
        settingsState.value = settingsState.value.copy(displayMode = mode)
    }

    override suspend fun updateShowExplanationTexts(show: Boolean) {
        val handled = if (show) {
            settingsState.value.explanationTextsPromptHandled
        } else {
            true
        }
        preferences.edit()
            .putBoolean(KEY_SHOW_EXPLANATION_TEXTS, show)
            .putBoolean(KEY_EXPLANATION_TEXTS_PROMPT_HANDLED, handled)
            .apply()
        settingsState.value = settingsState.value.copy(
            showExplanationTexts = show,
            explanationTextsPromptHandled = handled,
        )
    }

    override suspend fun updateExplanationTextsPromptTiming(timing: ExplanationTextsPromptTiming) {
        preferences.edit()
            .putString(KEY_EXPLANATION_TEXTS_PROMPT_TIMING, timing.name)
            .apply()
        settingsState.value = settingsState.value.copy(explanationTextsPromptTiming = timing)
    }

    override suspend fun resetExplanationTextsPrompt() {
        val nowEpochMillis = System.currentTimeMillis()
        preferences.edit()
            .putLong(KEY_EXPLANATION_TEXTS_PROMPT_TRACKING_STARTED_AT_EPOCH_MILLIS, nowEpochMillis)
            .putLong(KEY_EXPLANATION_TEXTS_PROMPT_FOREGROUND_USAGE_MILLIS, 0L)
            .putBoolean(KEY_EXPLANATION_TEXTS_PROMPT_HANDLED, false)
            .apply()
        settingsState.value = settingsState.value.copy(
            explanationTextsPromptTrackingStartedAtEpochMillis = nowEpochMillis,
            explanationTextsPromptForegroundUsageMillis = 0L,
            explanationTextsPromptHandled = false,
        )
    }

    override suspend fun markExplanationTextsPromptHandled() {
        preferences.edit()
            .putBoolean(KEY_EXPLANATION_TEXTS_PROMPT_HANDLED, true)
            .apply()
        settingsState.value = settingsState.value.copy(explanationTextsPromptHandled = true)
    }

    override suspend fun recordExplanationTextsPromptForegroundUsage(durationMillis: Long) {
        if (durationMillis <= 0L) return

        val updatedUsageMillis = settingsState.value.explanationTextsPromptForegroundUsageMillis + durationMillis
        preferences.edit()
            .putLong(KEY_EXPLANATION_TEXTS_PROMPT_FOREGROUND_USAGE_MILLIS, updatedUsageMillis)
            .apply()
        settingsState.value = settingsState.value.copy(
            explanationTextsPromptForegroundUsageMillis = updatedUsageMillis,
        )
    }

    private fun readSettings(): AppSettings {
        val storedFormat = CsvExportFormat.fromStoredValue(
            preferences.getString(KEY_CSV_EXPORT_FORMAT, null),
        )
        val legacySeparator = CsvSeparator.fromStoredValue(
            preferences.getString(KEY_CSV_SEPARATOR, null),
        )
        val storedCloudSyncDetailMode = CloudSyncDetailMode.fromStoredValue(
            preferences.getString(KEY_CLOUD_SYNC_DETAIL_MODE, null),
        )
        val storedBackgroundSyncMode = BackgroundSyncMode.fromStoredValue(
            preferences.getString(KEY_BACKGROUND_SYNC_MODE, null),
        )
        val storedDisplayMode = DisplayMode.fromStoredValue(
            preferences.getString(KEY_DISPLAY_MODE, null),
        )
        val showExplanationTexts = preferences.getBoolean(KEY_SHOW_EXPLANATION_TEXTS, true)
        val explanationTextsPromptTiming = ExplanationTextsPromptTiming.fromStoredValue(
            preferences.getString(KEY_EXPLANATION_TEXTS_PROMPT_TIMING, null),
        ) ?: ExplanationTextsPromptTiming.STANDARD
        val explanationTextsPromptTrackingStartedAtEpochMillis =
            ensureExplanationTextsPromptTrackingStartedAtEpochMillis()
        val explanationTextsPromptForegroundUsageMillis =
            preferences.getLong(KEY_EXPLANATION_TEXTS_PROMPT_FOREGROUND_USAGE_MILLIS, 0L)
        val explanationTextsPromptHandled =
            preferences.getBoolean(KEY_EXPLANATION_TEXTS_PROMPT_HANDLED, false)
        return AppSettings(
            csvExportFormat = storedFormat ?: legacySeparator?.toLegacyExportFormat() ?: CsvExportFormat.SYSTEM_DEFAULT,
            cloudSyncDetailMode = storedCloudSyncDetailMode ?: CloudSyncDetailMode.MISSING_ONLY,
            backgroundSyncMode = storedBackgroundSyncMode ?: BackgroundSyncMode.DISABLED,
            displayMode = storedDisplayMode ?: DisplayMode.AUTOMATIC,
            showExplanationTexts = showExplanationTexts,
            explanationTextsPromptTiming = explanationTextsPromptTiming,
            explanationTextsPromptTrackingStartedAtEpochMillis = explanationTextsPromptTrackingStartedAtEpochMillis,
            explanationTextsPromptForegroundUsageMillis = explanationTextsPromptForegroundUsageMillis,
            explanationTextsPromptHandled = explanationTextsPromptHandled,
        )
    }

    private fun ensureExplanationTextsPromptTrackingStartedAtEpochMillis(): Long {
        val storedValue = preferences.getLong(KEY_EXPLANATION_TEXTS_PROMPT_TRACKING_STARTED_AT_EPOCH_MILLIS, 0L)
        if (storedValue > 0L) return storedValue

        val nowEpochMillis = resolveInitialPromptTrackingStartEpochMillis()
        preferences.edit()
            .putLong(KEY_EXPLANATION_TEXTS_PROMPT_TRACKING_STARTED_AT_EPOCH_MILLIS, nowEpochMillis)
            .apply()
        return nowEpochMillis
    }

    private fun resolveInitialPromptTrackingStartEpochMillis(): Long =
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime
        }
            .getOrNull()
            ?.takeIf { it > 0L }
            ?: System.currentTimeMillis()

    private companion object {
        private const val PREFERENCES_NAME = "app_settings"
        private const val KEY_CSV_EXPORT_FORMAT = "csv_export_format"
        private const val KEY_CSV_SEPARATOR = "csv_separator"
        private const val KEY_CLOUD_SYNC_DETAIL_MODE = "cloud_sync_detail_mode"
        private const val KEY_BACKGROUND_SYNC_MODE = "background_sync_mode"
        private const val KEY_DISPLAY_MODE = "display_mode"
        private const val KEY_SHOW_EXPLANATION_TEXTS = "show_explanation_texts"
        private const val KEY_EXPLANATION_TEXTS_PROMPT_TIMING = "explanation_texts_prompt_timing"
        private const val KEY_EXPLANATION_TEXTS_PROMPT_TRACKING_STARTED_AT_EPOCH_MILLIS =
            "explanation_texts_prompt_tracking_started_at_epoch_millis"
        private const val KEY_EXPLANATION_TEXTS_PROMPT_FOREGROUND_USAGE_MILLIS =
            "explanation_texts_prompt_foreground_usage_millis"
        private const val KEY_EXPLANATION_TEXTS_PROMPT_HANDLED = "explanation_texts_prompt_handled"
    }
}
