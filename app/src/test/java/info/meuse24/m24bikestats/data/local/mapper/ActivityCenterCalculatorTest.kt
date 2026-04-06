package info.meuse24.m24bikestats.data.local.mapper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActivityCenterCalculatorTest {

    @Test
    fun `calculate returns null for empty list`() {
        assertNull(ActivityCenterCalculator.calculate(emptyList()))
    }

    @Test
    fun `calculate returns null if all coordinates are zero`() {
        val points = listOf(0.0 to 0.0, 0.0 to 0.0)
        assertNull(ActivityCenterCalculator.calculate(points))
    }

    @Test
    fun `calculate returns midpoint for valid list`() {
        val points = listOf(50.0 to 10.0, 52.0 to 12.0)
        val expected = 51.0 to 11.0
        assertEquals(expected, ActivityCenterCalculator.calculate(points))
    }

    @Test
    fun `calculate handles single point`() {
        val points = listOf(51.0 to 11.0)
        val expected = 51.0 to 11.0
        assertEquals(expected, ActivityCenterCalculator.calculate(points))
    }
}
