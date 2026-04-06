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

    @Test
    fun `frequency rows merge overflow tours into one bucket`() {
        assertEquals(
            listOf(
                StatisticsFrequencyRow(toursPerWeek = 1, weekCount = 2, isOverflow = false),
                StatisticsFrequencyRow(toursPerWeek = 3, weekCount = 2, isOverflow = true),
            ),
            mapOf(1 to 2, 3 to 1, 5 to 1).toFrequencyRows(),
        )
    }
}
