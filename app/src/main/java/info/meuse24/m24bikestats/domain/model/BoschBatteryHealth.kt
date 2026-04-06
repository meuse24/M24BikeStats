package info.meuse24.m24bikestats.domain.model

import kotlin.math.roundToInt

data class BoschBatteryHealth(
    val nominalCapacityWh: Int?,
    val healthPercent: Int?,
    val healthBand: BoschBatteryHealthBand?,
    val cycleRisk: BoschBatteryCycleRisk?,
    val deliveredWhPerSupportHour: Double?,
    val stressLevel: BoschBatteryStressLevel?,
) {
    val progress: Float?
        get() = healthPercent?.div(100f)
}

enum class BoschBatteryHealthBand {
    OPTIMAL,
    GOOD,
    AGED,
    WORN,
    CRITICAL,
}

enum class BoschBatteryCycleRisk {
    LOW,
    MEDIUM,
    ELEVATED,
}

enum class BoschBatteryStressLevel {
    GENTLE,
    NORMAL,
    INTENSIVE,
}

fun BoschBattery.estimateHealth(driveUnit: BoschDriveUnit?): BoschBatteryHealth {
    val nominalCapacityWh = inferNominalCapacityWh()
    val healthPercent = if (
        deliveredWhOverLifetime != null &&
        totalChargeCycles != null &&
        totalChargeCycles > 0.0 &&
        nominalCapacityWh != null &&
        nominalCapacityWh > 0
    ) {
        ((deliveredWhOverLifetime.toDouble() / (totalChargeCycles * nominalCapacityWh)) * 100.0)
            .roundToInt()
            .coerceIn(0, 100)
    } else {
        null
    }
    val deliveredWhPerSupportHour = if (
        deliveredWhOverLifetime != null &&
        driveUnit?.supportPowerOnHours != null &&
        driveUnit.supportPowerOnHours > 0
    ) {
        deliveredWhOverLifetime.toDouble() / driveUnit.supportPowerOnHours
    } else {
        null
    }

    return BoschBatteryHealth(
        nominalCapacityWh = nominalCapacityWh,
        healthPercent = healthPercent,
        healthBand = healthPercent?.toHealthBand(),
        cycleRisk = totalChargeCycles?.toCycleRisk(),
        deliveredWhPerSupportHour = deliveredWhPerSupportHour,
        stressLevel = deliveredWhPerSupportHour?.toStressLevel(),
    )
}

private fun BoschBattery.inferNominalCapacityWh(): Int? {
    val name = productName?.uppercase().orEmpty()
    if (name.isBlank()) return null

    return SUPPORTED_CAPACITIES.firstOrNull { capacity ->
        Regex("(?<!\\d)$capacity(?!\\d)").containsMatchIn(name)
    }
}

private fun Int.toHealthBand(): BoschBatteryHealthBand = when {
    this >= 90 -> BoschBatteryHealthBand.OPTIMAL
    this >= 80 -> BoschBatteryHealthBand.GOOD
    this >= 70 -> BoschBatteryHealthBand.AGED
    this >= 60 -> BoschBatteryHealthBand.WORN
    else -> BoschBatteryHealthBand.CRITICAL
}

private fun Double.toCycleRisk(): BoschBatteryCycleRisk = when {
    this < 200.0 -> BoschBatteryCycleRisk.LOW
    this <= 400.0 -> BoschBatteryCycleRisk.MEDIUM
    else -> BoschBatteryCycleRisk.ELEVATED
}

private fun Double.toStressLevel(): BoschBatteryStressLevel = when {
    this < 150.0 -> BoschBatteryStressLevel.GENTLE
    this <= 250.0 -> BoschBatteryStressLevel.NORMAL
    else -> BoschBatteryStressLevel.INTENSIVE
}

private val SUPPORTED_CAPACITIES = listOf(750, 625, 545, 500, 400, 300)
