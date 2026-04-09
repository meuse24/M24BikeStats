package info.meuse24.m24bikestats.presentation.navigation

import info.meuse24.m24bikestats.domain.model.AppSettings
import info.meuse24.m24bikestats.domain.model.ExplanationTextsPromptTiming
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppNavigationPromptTest {

    @Test
    fun `shows immediately when prompt is already due at session start`() {
        val sessionStartEpochMillis = ExplanationTextsPromptTiming.EARLY.minInstallAgeMillis + 1_000_000L
        val settings = AppSettings(
            showExplanationTexts = true,
            explanationTextsPromptTiming = ExplanationTextsPromptTiming.EARLY,
            explanationTextsPromptTrackingStartedAtEpochMillis =
                sessionStartEpochMillis - ExplanationTextsPromptTiming.EARLY.minInstallAgeMillis,
            explanationTextsPromptForegroundUsageMillis =
                ExplanationTextsPromptTiming.EARLY.minForegroundUsageMillis,
        )

        assertEquals(
            0L,
            remainingExplanationTextsPromptDelayForForegroundSession(
                appSettings = settings,
                sessionElapsedMillis = 0L,
                sessionStartEpochMillis = sessionStartEpochMillis,
            ),
        )
    }

    @Test
    fun `uses only a short inline delay when prompt becomes due right after app start`() {
        val sessionStartEpochMillis = ExplanationTextsPromptTiming.EARLY.minInstallAgeMillis + 2_000_000L
        val settings = AppSettings(
            showExplanationTexts = true,
            explanationTextsPromptTiming = ExplanationTextsPromptTiming.EARLY,
            explanationTextsPromptTrackingStartedAtEpochMillis =
                sessionStartEpochMillis - ExplanationTextsPromptTiming.EARLY.minInstallAgeMillis,
            explanationTextsPromptForegroundUsageMillis =
                ExplanationTextsPromptTiming.EARLY.minForegroundUsageMillis - 3_000L,
        )

        assertEquals(
            2_000L,
            remainingExplanationTextsPromptDelayForForegroundSession(
                appSettings = settings,
                sessionElapsedMillis = 1_000L,
                sessionStartEpochMillis = sessionStartEpochMillis,
            ),
        )
    }

    @Test
    fun `does not show during the same session when remaining time is still substantial`() {
        val sessionStartEpochMillis = ExplanationTextsPromptTiming.EARLY.minInstallAgeMillis + 3_000_000L
        val settings = AppSettings(
            showExplanationTexts = true,
            explanationTextsPromptTiming = ExplanationTextsPromptTiming.EARLY,
            explanationTextsPromptTrackingStartedAtEpochMillis =
                sessionStartEpochMillis - ExplanationTextsPromptTiming.EARLY.minInstallAgeMillis,
            explanationTextsPromptForegroundUsageMillis =
                ExplanationTextsPromptTiming.EARLY.minForegroundUsageMillis - 20_000L,
        )

        assertNull(
            remainingExplanationTextsPromptDelayForForegroundSession(
                appSettings = settings,
                sessionElapsedMillis = 0L,
                sessionStartEpochMillis = sessionStartEpochMillis,
            ),
        )
    }

    @Test
    fun `does not appear after the short session start window has passed`() {
        val sessionStartEpochMillis = ExplanationTextsPromptTiming.EARLY.minInstallAgeMillis + 4_000_000L
        val settings = AppSettings(
            showExplanationTexts = true,
            explanationTextsPromptTiming = ExplanationTextsPromptTiming.EARLY,
            explanationTextsPromptTrackingStartedAtEpochMillis =
                sessionStartEpochMillis - ExplanationTextsPromptTiming.EARLY.minInstallAgeMillis,
            explanationTextsPromptForegroundUsageMillis =
                ExplanationTextsPromptTiming.EARLY.minForegroundUsageMillis - 2_000L,
        )

        assertNull(
            remainingExplanationTextsPromptDelayForForegroundSession(
                appSettings = settings,
                sessionElapsedMillis = EXPLANATION_TEXTS_PROMPT_INLINE_DELAY_THRESHOLD_MILLIS + 1L,
                sessionStartEpochMillis = sessionStartEpochMillis,
            ),
        )
    }
}
