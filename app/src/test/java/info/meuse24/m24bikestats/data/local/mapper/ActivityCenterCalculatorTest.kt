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
    fun `calculate returns single point for single-element list`() {
        val points = listOf(51.0 to 11.0)
        assertEquals(51.0 to 11.0, ActivityCenterCalculator.calculate(points))
    }

    @Test
    fun `calculate returns farthest point from start`() {
        // Start: 51.0/10.0 — drei weitere Punkte, der bei 55.0/10.0 ist am weitesten entfernt
        val points = listOf(
            51.0 to 10.0,  // Start
            52.0 to 10.0,
            53.0 to 10.0,
            55.0 to 10.0,  // farthest
        )
        assertEquals(55.0 to 10.0, ActivityCenterCalculator.calculate(points))
    }

    @Test
    fun `calculate returns start point itself for round trip where all others are closer`() {
        // Start und Endpunkt identisch (Rundtour) — farthest ist irgendwo auf der Route
        val points = listOf(
            51.0 to 10.0,  // Start
            52.0 to 11.0,  // farthest
            51.5 to 10.5,
            51.0 to 10.0,  // Endpunkt = Startpunkt
        )
        assertEquals(52.0 to 11.0, ActivityCenterCalculator.calculate(points))
    }

    @Test
    fun `calculate applies longitude cosine correction`() {
        // Bei lat 60° ist 1° Längengrad nur cos(60°)=0.5 weit,
        // daher ist Punkt B (2° nördlich) weiter als Punkt C (3° östlich).
        val start = 60.0 to 10.0
        val north = 62.0 to 10.0  // dLat=2°, dLng=0   → dist² = 4
        val east  = 60.0 to 16.0  // dLat=0,  dLng=6°  → dist² = (6*cos60°)² = (3.0)² = 9 → east gewinnt
        val points = listOf(start, north, east)
        assertEquals(east, ActivityCenterCalculator.calculate(points))
    }

    @Test
    fun `calculate ignores zero coordinates mixed with valid points`() {
        val points = listOf(
            0.0 to 0.0,   // wird gefiltert
            51.0 to 10.0, // Start (erster valider Punkt)
            53.0 to 10.0, // farthest
        )
        assertEquals(53.0 to 10.0, ActivityCenterCalculator.calculate(points))
    }
}
