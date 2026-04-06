package info.meuse24.m24bikestats.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BoschBatteryHealthTest {

    @Test
    fun `estimate health derives soh risk and stress from battery and drive unit`() {
        val health = BoschBattery(
            serialNumber = null,
            partNumber = null,
            productName = "PowerTube 750",
            deliveredWhOverLifetime = 51554,
            totalChargeCycles = 83.8,
            onBikeChargeCycles = 79.2,
            offBikeChargeCycles = 4.5,
        ).estimateHealth(
            BoschDriveUnit(
                serialNumber = null,
                partNumber = null,
                productName = null,
                odometerMeters = null,
                rearWheelCircumferenceMillimeters = null,
                maximumAssistanceSpeedKmh = null,
                walkAssistEnabled = null,
                walkAssistMaximumSpeedKmh = null,
                activeAssistModes = emptyList(),
                totalPowerOnHours = 867,
                supportPowerOnHours = 867,
            ),
        )

        assertEquals(750, health.nominalCapacityWh)
        assertEquals(82, health.healthPercent)
        assertEquals(BoschBatteryHealthBand.GOOD, health.healthBand)
        assertEquals(BoschBatteryCycleRisk.LOW, health.cycleRisk)
        assertEquals(BoschBatteryStressLevel.GENTLE, health.stressLevel)
    }

    @Test
    fun `estimate health keeps soh null when nominal capacity cannot be inferred`() {
        val health = BoschBattery(
            serialNumber = null,
            partNumber = null,
            productName = "Custom Battery",
            deliveredWhOverLifetime = 24000,
            totalChargeCycles = 48.0,
            onBikeChargeCycles = null,
            offBikeChargeCycles = null,
        ).estimateHealth(driveUnit = null)

        assertNull(health.nominalCapacityWh)
        assertNull(health.healthPercent)
        assertEquals(BoschBatteryCycleRisk.LOW, health.cycleRisk)
    }
}
