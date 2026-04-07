package info.meuse24.m24bikestats.domain.model

import java.time.DayOfWeek
import java.time.Instant

data class PdfReportData(
    val generatedAt: Instant,
    val userInfo: PdfReportUserInfo?,
    val discoveryInfo: PdfReportDiscoveryInfo?,
    val bikes: List<BoschBike>,
    val activitySummary: PdfReportActivitySummary,
    val statistics: PdfReportStatistics,
    val mapPoints: List<Pair<Double, Double>> = emptyList(),
)

data class PdfReportUserInfo(
    val email: String?,
    val username: String?,
    val subject: String?,
)

data class PdfReportDiscoveryInfo(
    val issuer: String?,
    val authorizationEndpoint: String?,
    val tokenEndpoint: String?,
    val userInfoEndpoint: String?,
)

data class PdfReportActivitySummary(
    val totalTours: Int,
    val totalDistanceKm: Double,
    val totalDurationHours: Double,
    val avgDistanceKm: Double,
    val avgDurationHours: Double,
    val earliestActivityDate: Instant?,
    val latestActivityDate: Instant?,
    val avgTravelSpeedKmh: Double?,
    val totalElevationGainM: Int,
    val totalCaloriesBurned: Double?,
)

data class PdfReportStatistics(
    val weeklyPeriods: List<PdfReportPeriod>,
    val monthlyPeriods: List<PdfReportPeriod>,
    val yearlyPeriods: List<PdfReportPeriod>,
    val highlights: PdfReportHighlights,
    val strongestWeek: PdfReportPeriod?,
    val strongestMonth: PdfReportPeriod?,
    val strongestYear: PdfReportPeriod?,
    val dayOfWeekDistribution: Map<DayOfWeek, Int>,
    val weeklyFrequencyHistogram: Map<Int, Int>,
    val activeWeeksRatio: Double?,
)

data class PdfReportPeriod(
    val label: String,
    val dateRangeLabel: String,
    val tourCount: Int,
    val distanceKm: Double,
    val durationHours: Double,
)

data class PdfReportHighlights(
    val longestTourKm: Double,
    val longestRideHours: Double,
    val maxSpeedKmh: Double?,
    val fastestTourAvgSpeedKmh: Double?,
    val maxRiderPowerWatts: Double?,
    val favoriteDayOfWeek: DayOfWeek?,
)
