package info.meuse24.m24bikestats.data.export

import android.graphics.pdf.PdfRenderer
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import info.meuse24.m24bikestats.domain.model.BoschBike
import info.meuse24.m24bikestats.domain.model.PdfReportActivitySummary
import info.meuse24.m24bikestats.domain.model.PdfReportData
import info.meuse24.m24bikestats.domain.model.PdfReportDiscoveryInfo
import info.meuse24.m24bikestats.domain.model.PdfReportHighlights
import info.meuse24.m24bikestats.domain.model.PdfReportPeriod
import info.meuse24.m24bikestats.domain.model.PdfReportStatistics
import info.meuse24.m24bikestats.domain.model.PdfReportUserInfo
import java.time.DayOfWeek
import java.time.Instant
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PdfReportGeneratorAndroidTest {

    @Test
    fun generate_writesReadablePdfFile() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val generator = PdfReportGenerator(
            context = context,
            stringResolver = AndroidPdfStringResolver(context),
            appVersion = "test",
        )

        val file = generator.generate(
            reportData = sampleReportData(),
            fileName = "m24-report-test.pdf",
        )
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )

        assertTrue(file.exists())
        context.contentResolver.openFileDescriptor(uri, "r")!!.use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                assertTrue(renderer.pageCount >= 4)
            }
        }
    }

    private fun sampleReportData() = PdfReportData(
        generatedAt = Instant.parse("2026-04-06T08:30:00Z"),
        userInfo = PdfReportUserInfo(
            email = "test@example.com",
            username = "tester",
            subject = "sub-1",
        ),
        discoveryInfo = PdfReportDiscoveryInfo(
            issuer = "https://issuer.example",
            authorizationEndpoint = "https://issuer.example/auth",
            tokenEndpoint = "https://issuer.example/token",
            userInfoEndpoint = "https://issuer.example/userinfo",
        ),
        bikes = listOf(
            BoschBike(
                id = "bike-1",
                createdAt = null,
                language = "de",
                driveUnit = null,
                remoteControl = null,
                headUnit = null,
                batteries = emptyList(),
            )
        ),
        activitySummary = PdfReportActivitySummary(
            totalTours = 12,
            totalDistanceKm = 432.8,
            totalDurationHours = 18.6,
            avgDistanceKm = 36.1,
            avgDurationHours = 1.55,
            earliestActivityDate = Instant.parse("2026-01-03T08:00:00Z"),
            latestActivityDate = Instant.parse("2026-04-05T10:00:00Z"),
            avgTravelSpeedKmh = 23.3,
            totalElevationGainM = 6400,
            totalCaloriesBurned = 8420.0,
        ),
        statistics = PdfReportStatistics(
            monthlyPeriods = listOf(
                PdfReportPeriod(label = "Jan 26", tourCount = 3, distanceKm = 98.0, durationHours = 4.1),
                PdfReportPeriod(label = "Feb 26", tourCount = 2, distanceKm = 76.0, durationHours = 3.2),
                PdfReportPeriod(label = "Mar 26", tourCount = 4, distanceKm = 144.0, durationHours = 6.1),
                PdfReportPeriod(label = "Apr 26", tourCount = 3, distanceKm = 114.8, durationHours = 5.2),
            ),
            yearlyPeriods = listOf(
                PdfReportPeriod(label = "2025", tourCount = 8, distanceKm = 312.0, durationHours = 14.2),
                PdfReportPeriod(label = "2026", tourCount = 4, distanceKm = 120.8, durationHours = 4.8),
            ),
            highlights = PdfReportHighlights(
                longestTourKm = 62.4,
                maxSpeedKmh = 48.3,
                maxRiderPowerWatts = 521.0,
                favoriteDayOfWeek = DayOfWeek.SATURDAY,
            ),
            dayOfWeekDistribution = mapOf(
                DayOfWeek.MONDAY to 1,
                DayOfWeek.WEDNESDAY to 2,
                DayOfWeek.FRIDAY to 3,
                DayOfWeek.SATURDAY to 6,
            ),
            weeklyFrequencyHistogram = mapOf(0 to 2, 1 to 4, 2 to 3, 3 to 1),
            activeWeeksRatio = 0.73,
        ),
        mapPoints = listOf(
            48.2082 to 16.3738,
            47.0707 to 15.4395,
            48.3069 to 14.2858,
            47.8095 to 13.0550,
        ),
    )
}
