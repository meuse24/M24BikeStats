package info.meuse24.m24bikestats.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsTest {

    @Test
    fun `suggestion becomes due only after install age and usage thresholds are met`() {
        val nowEpochMillis = 1_000_000_000L
        val settings = AppSettings(
            showExplanationTexts = true,
            explanationTextsPromptTiming = ExplanationTextsPromptTiming.STANDARD,
            explanationTextsPromptTrackingStartedAtEpochMillis =
                nowEpochMillis - ExplanationTextsPromptTiming.STANDARD.minInstallAgeMillis,
            explanationTextsPromptForegroundUsageMillis =
                ExplanationTextsPromptTiming.STANDARD.minForegroundUsageMillis - 10L * 60L * 1000L,
        )

        assertFalse(
            settings.shouldSuggestHidingExplanationTexts(
                nowEpochMillis = nowEpochMillis,
                currentForegroundUsageMillis = 9L * 60L * 1000L,
            ),
        )
        assertTrue(
            settings.shouldSuggestHidingExplanationTexts(
                nowEpochMillis = nowEpochMillis,
                currentForegroundUsageMillis = 10L * 60L * 1000L,
            ),
        )
    }

    @Test
    fun `remaining suggestion delay follows the slower of install age and usage progress`() {
        val nowEpochMillis = 2_000_000_000L
        val settings = AppSettings(
            showExplanationTexts = true,
            explanationTextsPromptTiming = ExplanationTextsPromptTiming.EARLY,
            explanationTextsPromptTrackingStartedAtEpochMillis =
                nowEpochMillis - ExplanationTextsPromptTiming.EARLY.minInstallAgeMillis + 60L * 60L * 1000L,
            explanationTextsPromptForegroundUsageMillis =
                ExplanationTextsPromptTiming.EARLY.minForegroundUsageMillis - 5L * 60L * 1000L,
        )

        assertEquals(
            60L * 60L * 1000L,
            settings.remainingMillisUntilExplanationTextsSuggestion(
                nowEpochMillis = nowEpochMillis,
                currentForegroundUsageMillis = 0L,
            ),
        )
    }

    @Test
    fun `remaining suggestion stays positive when only install age is missing`() {
        val nowEpochMillis = 3_000_000_000L
        val settings = AppSettings(
            showExplanationTexts = true,
            explanationTextsPromptTiming = ExplanationTextsPromptTiming.STANDARD,
            explanationTextsPromptTrackingStartedAtEpochMillis =
                nowEpochMillis - ExplanationTextsPromptTiming.STANDARD.minInstallAgeMillis + 15L * 60L * 1000L,
            explanationTextsPromptForegroundUsageMillis =
                ExplanationTextsPromptTiming.STANDARD.minForegroundUsageMillis,
        )

        assertEquals(
            15L * 60L * 1000L,
            settings.remainingMillisUntilExplanationTextsSuggestion(nowEpochMillis = nowEpochMillis),
        )
    }

    @Test
    fun `remaining suggestion becomes zero when current foreground session closes the final gap`() {
        val nowEpochMillis = 4_000_000_000L
        val settings = AppSettings(
            showExplanationTexts = true,
            explanationTextsPromptTiming = ExplanationTextsPromptTiming.EARLY,
            explanationTextsPromptTrackingStartedAtEpochMillis =
                nowEpochMillis - ExplanationTextsPromptTiming.EARLY.minInstallAgeMillis,
            explanationTextsPromptForegroundUsageMillis =
                ExplanationTextsPromptTiming.EARLY.minForegroundUsageMillis - 2_000L,
        )

        assertEquals(
            0L,
            settings.remainingMillisUntilExplanationTextsSuggestion(
                nowEpochMillis = nowEpochMillis,
                currentForegroundUsageMillis = 2_000L,
            ),
        )
        assertTrue(
            settings.shouldSuggestHidingExplanationTexts(
                nowEpochMillis = nowEpochMillis,
                currentForegroundUsageMillis = 2_000L,
            ),
        )
    }

    @Test
    fun `never timing hidden texts and handled prompts suppress suggestions`() {
        val disabled = AppSettings(
            showExplanationTexts = true,
            explanationTextsPromptTiming = ExplanationTextsPromptTiming.NEVER,
            explanationTextsPromptTrackingStartedAtEpochMillis = 1L,
        )
        val hidden = AppSettings(
            showExplanationTexts = false,
            explanationTextsPromptTiming = ExplanationTextsPromptTiming.EARLY,
            explanationTextsPromptTrackingStartedAtEpochMillis = 1L,
        )
        val handled = AppSettings(
            showExplanationTexts = true,
            explanationTextsPromptTiming = ExplanationTextsPromptTiming.EARLY,
            explanationTextsPromptTrackingStartedAtEpochMillis = 1L,
            explanationTextsPromptHandled = true,
        )

        assertNull(disabled.remainingMillisUntilExplanationTextsSuggestion())
        assertNull(hidden.remainingMillisUntilExplanationTextsSuggestion())
        assertNull(handled.remainingMillisUntilExplanationTextsSuggestion())
        assertFalse(disabled.shouldSuggestHidingExplanationTexts())
        assertFalse(hidden.shouldSuggestHidingExplanationTexts())
        assertFalse(handled.shouldSuggestHidingExplanationTexts())
    }
}
