package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.model.AppSettings
import info.meuse24.m24bikestats.domain.model.BackgroundSyncMode
import info.meuse24.m24bikestats.domain.model.CloudSyncDetailMode
import info.meuse24.m24bikestats.domain.model.CsvExportFormat
import info.meuse24.m24bikestats.domain.model.DisplayMode
import info.meuse24.m24bikestats.domain.model.ExplanationTextsPromptTiming
import info.meuse24.m24bikestats.domain.repository.AppSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeAppSettingsRepository(
    initialFormat: CsvExportFormat = CsvExportFormat.SYSTEM_DEFAULT,
    initialCloudSyncDetailMode: CloudSyncDetailMode = CloudSyncDetailMode.MISSING_ONLY,
    initialBackgroundSyncMode: BackgroundSyncMode = BackgroundSyncMode.DISABLED,
    initialDisplayMode: DisplayMode = DisplayMode.AUTOMATIC,
    initialShowExplanationTexts: Boolean = true,
    initialExplanationTextsPromptTiming: ExplanationTextsPromptTiming = ExplanationTextsPromptTiming.STANDARD,
    initialExplanationTextsPromptTrackingStartedAtEpochMillis: Long = 1L,
    initialExplanationTextsPromptForegroundUsageMillis: Long = 0L,
    initialExplanationTextsPromptHandled: Boolean = false,
) : AppSettingsRepository {
    private val settingsState = MutableStateFlow(
        AppSettings(
            csvExportFormat = initialFormat,
            cloudSyncDetailMode = initialCloudSyncDetailMode,
            backgroundSyncMode = initialBackgroundSyncMode,
            displayMode = initialDisplayMode,
            showExplanationTexts = initialShowExplanationTexts,
            explanationTextsPromptTiming = initialExplanationTextsPromptTiming,
            explanationTextsPromptTrackingStartedAtEpochMillis = initialExplanationTextsPromptTrackingStartedAtEpochMillis,
            explanationTextsPromptForegroundUsageMillis = initialExplanationTextsPromptForegroundUsageMillis,
            explanationTextsPromptHandled = initialExplanationTextsPromptHandled,
        )
    )

    override fun observeSettings(): Flow<AppSettings> = settingsState.asStateFlow()

    override suspend fun getSettings(): AppSettings = settingsState.value

    override suspend fun updateCsvExportFormat(format: CsvExportFormat) {
        settingsState.value = settingsState.value.copy(csvExportFormat = format)
    }

    override suspend fun updateCloudSyncDetailMode(mode: CloudSyncDetailMode) {
        settingsState.value = settingsState.value.copy(cloudSyncDetailMode = mode)
    }

    override suspend fun updateBackgroundSyncMode(mode: BackgroundSyncMode) {
        settingsState.value = settingsState.value.copy(backgroundSyncMode = mode)
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
}
