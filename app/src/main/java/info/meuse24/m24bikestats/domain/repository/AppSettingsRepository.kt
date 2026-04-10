package info.meuse24.m24bikestats.domain.repository

import info.meuse24.m24bikestats.domain.model.AppSettings
import info.meuse24.m24bikestats.domain.model.CsvExportFormat
import info.meuse24.m24bikestats.domain.model.DisplayMode
import info.meuse24.m24bikestats.domain.model.ExplanationTextsPromptTiming
import kotlinx.coroutines.flow.Flow

interface AppSettingsRepository {
    fun observeSettings(): Flow<AppSettings>
    suspend fun getSettings(): AppSettings
    suspend fun updateCsvExportFormat(format: CsvExportFormat)
    suspend fun updateDisplayMode(mode: DisplayMode)
    suspend fun updateShowExplanationTexts(show: Boolean)
    suspend fun updateExplanationTextsPromptTiming(timing: ExplanationTextsPromptTiming)
    suspend fun resetExplanationTextsPrompt()
    suspend fun markExplanationTextsPromptHandled()
    suspend fun recordExplanationTextsPromptForegroundUsage(durationMillis: Long)
    suspend fun markInitialSyncCompleted(atEpochMillis: Long)
    suspend fun resetInitialSyncFlag()
    suspend fun updateLatestCachedActivityStartTime(epochMillis: Long)
    suspend fun resetLatestCachedActivityStartTime()
}
