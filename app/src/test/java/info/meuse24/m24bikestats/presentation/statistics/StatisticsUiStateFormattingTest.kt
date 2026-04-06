package info.meuse24.m24bikestats.presentation.statistics

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class StatisticsUiStateFormattingTest {

    @Test
    fun `formats readable distance and hours with locale`() {
        assertEquals("12.3 km", 12.34.toReadableDistance(Locale.US))
        assertEquals("1,3 h", 1.25.toReadableHours(Locale.GERMAN))
    }

    @Test
    fun `period stats expose duration in hours`() {
        val period = PeriodStats(
            label = "Apr 26",
            dateRangeLabel = "01.04.26 - 30.04.26",
            startEpochMillis = 0L,
            tourCount = 2,
            distanceKm = 42.0,
            durationMinutes = 90,
        )

        assertEquals(1.5, period.durationHours, 0.0)
    }
}
