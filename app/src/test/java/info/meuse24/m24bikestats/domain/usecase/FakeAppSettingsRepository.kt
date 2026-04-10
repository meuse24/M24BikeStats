package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.model.AppSettings
import info.meuse24.m24bikestats.domain.model.CsvExportFormat
import info.meuse24.m24bikestats.domain.model.DisplayMode
import info.meuse24.m24bikestats.domain.model.ExplanationTextsPromptTiming
import info.meuse24.m24bikestats.domain.repository.AppSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeAppSettingsRepository(
    initialFormat: CsvExportFormat = CsvExportFormat.SYSTEM_DEFAULT,
    initialDisplayMode: DisplayMode = DisplayMode.AUTOMATIC,
    initialShowExplanationTexts: Boolean = true,
    initialExplanationTextsPromptTiming: ExplanationTextsPromptTiming = ExplanationTextsPromptTiming.STANDARD,
    initialExplanationTextsPromptTrackingStartedAtEpochMillis: Long = 1L,
    initialExplanationTextsPromptForegroundUsageMillis: Long = 0L,
    initialExplanationTextsPromptHandled: Boolean = false,
    initialInitialSyncCompletedAtEpochMillis: Long = 0L,
    initialLatestCachedActivityStartTimeMillis: Long = 0L,
) : AppSettingsRepository {
    private val settingsState = MutableStateFlow(
        AppSettings(
            csvExportFormat = initialFormat,
            displayMode = initialDisplayMode,
            showExplanationTexts = initialShowExplanationTexts,
            explanationTextsPromptTiming = initialExplanationTextsPromptTiming,
            explanationTextsPromptTrackingStartedAtEpochMillis = initialExplanationTextsPromptTrackingStartedAtEpochMillis,
            explanationTextsPromptForegroundUsageMillis = initialExplanationTextsPromptForegroundUsageMillis,
            explanationTextsPromptHandled = initialExplanationTextsPromptHandled,
            initialSyncCompletedAtEpochMillis = initialInitialSyncCompletedAtEpochMillis,
            latestCachedActivityStartTimeMillis = initialLatestCachedActivityStartTimeMillis,
        )
    )

    override fun observeSettings(): Flow<AppSettings> = settingsState.asStateFlow()

    override suspend fun getSettings(): AppSettings = settingsState.value

    override suspend fun updateCsvExportFormat(format: CsvExportFormat) {
        settingsState.value = settingsState.value.copy(csvExportFormat = format)
    }

    override suspend fun updateDisplayMode(mode: DisplayMode) {
        settingsState.value = settingsState.value.copy(displayMode = mode)
    }

    override suspend fun updateShowExplanationTexts(show: Boolean) {
        settingsState.value = settingsState.value.copy(
            showExplanationTexts = show,
            explanationTextsPromptHandled = if (show) settingsState.value.explanationTextsPromptHandled else true,
        )
    }

    override suspend fun updateExplanationTextsPromptTiming(timing: ExplanationTextsPromptTiming) {
        settingsState.value = settingsState.value.copy(explanationTextsPromptTiming = timing)
    }

    override suspend fun resetExplanationTextsPrompt() {
        settingsState.value = settingsState.value.copy(
            explanationTextsPromptTrackingStartedAtEpochMillis = System.currentTimeMillis(),
            explanationTextsPromptForegroundUsageMillis = 0L,
            explanationTextsPromptHandled = false,
        )
    }

    override suspend fun markExplanationTextsPromptHandled() {
        settingsState.value = settingsState.value.copy(explanationTextsPromptHandled = true)
    }

    override suspend fun recordExplanationTextsPromptForegroundUsage(durationMillis: Long) {
        if (durationMillis <= 0L) return
        settingsState.value = settingsState.value.copy(
            explanationTextsPromptForegroundUsageMillis =
                settingsState.value.explanationTextsPromptForegroundUsageMillis + durationMillis,
        )
    }

    override suspend fun markInitialSyncCompleted(atEpochMillis: Long) {
        settingsState.value = settingsState.value.copy(initialSyncCompletedAtEpochMillis = atEpochMillis)
    }

    override suspend fun resetInitialSyncFlag() {
        settingsState.value = settingsState.value.copy(initialSyncCompletedAtEpochMillis = 0L)
    }

    override suspend fun updateLatestCachedActivityStartTime(epochMillis: Long) {
        settingsState.value = settingsState.value.copy(latestCachedActivityStartTimeMillis = epochMillis)
    }

    override suspend fun resetLatestCachedActivityStartTime() {
        settingsState.value = settingsState.value.copy(latestCachedActivityStartTimeMillis = 0L)
    }
}
