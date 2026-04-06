package info.meuse24.m24bikestats.data.export

import android.content.Context
import android.graphics.pdf.PdfDocument
import info.meuse24.m24bikestats.R
import info.meuse24.m24bikestats.domain.model.BoschAssistMode
import info.meuse24.m24bikestats.domain.model.BoschBattery
import info.meuse24.m24bikestats.domain.model.BoschBike
import info.meuse24.m24bikestats.domain.model.BoschComponent
import info.meuse24.m24bikestats.domain.model.BoschDriveUnit
import info.meuse24.m24bikestats.domain.model.PdfReportData
import info.meuse24.m24bikestats.domain.repository.PdfReportFileExporter
import java.io.File
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class PdfReportGenerator(
    private val context: Context,
    private val stringResolver: PdfStringResolver,
    private val appVersion: String,
    private val localeProvider: () -> Locale = Locale::getDefault,
    private val zoneIdProvider: () -> ZoneId = ZoneId::systemDefault,
) : PdfReportFileExporter {
    override fun generate(
        reportData: PdfReportData,
        fileName: String,
    ): File {
        val locale = localeProvider()
        val zoneId = zoneIdProvider()
        val colors = PdfColorScheme()
        val document = PdfDocument()
        val builder = PdfPageBuilder(
            document = document,
            typography = PdfTypography(colors),
            colors = colors,
            footerText = "${s(R.string.pdf_footer)} • v$appVersion • ${formatDateTime(reportData.generatedAt, locale, zoneId)}",
        )

        builder.startPage()
        builder.drawCoverTitle(context.getString(R.string.app_name), s(R.string.pdf_section_cover_subtitle))
        builder.drawMetricTiles(
            items = listOf(
                s(R.string.statistics_summary_tours) to reportData.activitySummary.totalTours.toString(),
                s(R.string.statistics_summary_distance) to formatDistance(reportData.activitySummary.totalDistanceKm, locale),
                s(R.string.statistics_summary_duration) to formatHours(reportData.activitySummary.totalDurationHours, locale),
                s(R.string.pdf_label_period) to formatPeriod(
                    reportData.activitySummary.earliestActivityDate,
                    reportData.activitySummary.latestActivityDate,
                    locale,
                    zoneId,
                ),
            ),
            columns = 2,
        )
        builder.drawLabelValueRow(s(R.string.pdf_label_generated), formatDateTime(reportData.generatedAt, locale, zoneId))
        builder.drawLabelValueRow(s(R.string.dashboard_label_oidc_user_email), reportData.userInfo?.email.orDash())
        builder.drawLabelValueRow(s(R.string.dashboard_label_oidc_user_username), reportData.userInfo?.username.orDash())

        builder.startPage()
        builder.drawSectionHeader(s(R.string.pdf_section_account))
        builder.drawLabelValueRow(s(R.string.dashboard_label_oidc_user_email), reportData.userInfo?.email.orDash())
        builder.drawLabelValueRow(s(R.string.dashboard_label_oidc_user_username), reportData.userInfo?.username.orDash())
        builder.drawLabelValueRow(s(R.string.dashboard_label_oidc_user_subject), reportData.userInfo?.subject.orDash())
        builder.space(8f)
        builder.drawSectionHeader(s(R.string.pdf_label_oauth_endpoints))
        builder.drawLabelValueRow(s(R.string.dashboard_label_oidc_discovery_issuer), reportData.discoveryInfo?.issuer.orDash())
        builder.drawLabelValueRow(
            s(R.string.dashboard_label_oidc_authorization_endpoint),
            reportData.discoveryInfo?.authorizationEndpoint.orDash(),
        )
        builder.drawLabelValueRow(s(R.string.dashboard_label_oidc_token_endpoint), reportData.discoveryInfo?.tokenEndpoint.orDash())
        builder.drawLabelValueRow(s(R.string.dashboard_label_oidc_userinfo_endpoint), reportData.discoveryInfo?.userInfoEndpoint.orDash())

        builder.startPage()
        builder.drawSectionHeader(s(R.string.pdf_section_bikes))
        if (reportData.bikes.isEmpty()) {
            builder.drawBodyText(s(R.string.pdf_value_not_available))
        } else {
            reportData.bikes.forEachIndexed { index, bike ->
                if (index > 0) builder.space(12f)
                drawBikeSection(builder, bike, locale)
            }
        }

        builder.startPage()
        builder.drawSectionHeader(s(R.string.pdf_section_activities))
        builder.drawMetricTiles(
            items = listOf(
                s(R.string.statistics_summary_tours) to reportData.activitySummary.totalTours.toString(),
                s(R.string.statistics_summary_distance) to formatDistance(reportData.activitySummary.totalDistanceKm, locale),
                s(R.string.statistics_summary_duration) to formatHours(reportData.activitySummary.totalDurationHours, locale),
                s(R.string.statistics_summary_avg_distance) to formatDistance(reportData.activitySummary.avgDistanceKm, locale),
                s(R.string.statistics_summary_avg_duration) to formatHours(reportData.activitySummary.avgDurationHours, locale),
                s(R.string.pdf_label_period) to formatPeriod(
                    reportData.activitySummary.earliestActivityDate,
                    reportData.activitySummary.latestActivityDate,
                    locale,
                    zoneId,
                ),
                s(R.string.statistics_highlights_avg_speed) to reportData.activitySummary.avgTravelSpeedKmh?.let {
                    formatSpeed(it, locale)
                }.orDash(),
                s(R.string.statistics_highlights_total_elevation) to formatMeters(reportData.activitySummary.totalElevationGainM, locale),
                s(R.string.statistics_highlights_total_calories) to reportData.activitySummary.totalCaloriesBurned?.let {
                    formatCalories(it, locale)
                }.orDash(),
            ),
            columns = 3,
        )

        builder.startPage()
        builder.drawSectionHeader(s(R.string.pdf_section_statistics))
        builder.drawBarLineChart(
            periods = reportData.statistics.monthlyPeriods,
            avgDistanceKm = reportData.activitySummary.avgDistanceKm,
            avgDurationHours = reportData.activitySummary.avgDurationHours,
            distanceLegend = s(R.string.statistics_legend_distance),
            durationLegend = s(R.string.statistics_legend_duration),
        )
        builder.drawMetricTiles(
            items = listOf(
                s(R.string.statistics_highlights_longest_tour) to formatDistance(reportData.statistics.highlights.longestTourKm, locale),
                s(R.string.statistics_highlights_max_speed) to reportData.statistics.highlights.maxSpeedKmh?.let {
                    formatSpeed(it, locale)
                }.orDash(),
                s(R.string.statistics_highlights_max_power) to reportData.statistics.highlights.maxRiderPowerWatts?.let {
                    formatWatts(it, locale)
                }.orDash(),
                s(R.string.statistics_highlights_favorite_day_pdf) to reportData.statistics.highlights.favoriteDayOfWeek?.toLocalizedDay(locale).orDash(),
            ),
            columns = 2,
        )

        builder.startPage()
        builder.drawSectionHeader(s(R.string.pdf_section_rhythm))
        builder.drawHorizontalBarChart(
            rows = DayOfWeek.entries.map { it.toLocalizedDay(locale) to (reportData.statistics.dayOfWeekDistribution[it] ?: 0) },
            highlightLabel = reportData.statistics.highlights.favoriteDayOfWeek?.toLocalizedDay(locale),
        )
        val frequencyRows = reportData.statistics.weeklyFrequencyHistogram.toPdfFrequencyRows()
        val highlightedIndex = frequencyRows.withIndex().maxByOrNull { it.value.second }?.index
        builder.drawFrequencyTable(
            rows = frequencyRows.map { (label, weeks) -> label to s(R.string.statistics_highlights_freq_weeks, arrayOf(weeks)) },
            highlightedRowIndex = highlightedIndex,
        )
        reportData.statistics.activeWeeksRatio?.let { ratio ->
            builder.drawProgressBar(
                label = s(R.string.statistics_highlights_active_weeks, arrayOf((ratio * 100).toInt())),
                ratio = ratio,
            )
        }

        builder.finish()

        val exportDir = File(context.cacheDir, "shared_exports").apply { mkdirs() }
        val safeName = fileName
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9._-]+"), "-")
            .trim('-')
            .ifBlank { "m24-bericht.pdf" }
        val file = File(exportDir, safeName)
        file.outputStream().use(document::writeTo)
        document.close()
        return file
    }

    private fun drawBikeSection(
        builder: PdfPageBuilder,
        bike: BoschBike,
        locale: Locale,
    ) {
        builder.drawBodyText(bike.driveUnit?.productName ?: "${s(R.string.dashboard_bike_summary_title)} ${bike.id}")
        drawDriveUnit(builder, bike.driveUnit, locale)
        drawBatteries(builder, bike.batteries, locale)
        drawComponent(builder, s(R.string.dashboard_section_remote), bike.remoteControl)
        drawComponent(builder, s(R.string.dashboard_section_head_unit), bike.headUnit)
    }

    private fun drawDriveUnit(
        builder: PdfPageBuilder,
        driveUnit: BoschDriveUnit?,
        locale: Locale,
    ) {
        builder.drawSectionHeader(s(R.string.dashboard_section_drive_unit))
        if (driveUnit == null) {
            builder.drawBodyText(s(R.string.pdf_value_not_available))
            return
        }
        builder.drawLabelValueRow(s(R.string.dashboard_label_product), driveUnit.productName.orDash())
        builder.drawLabelValueRow(s(R.string.dashboard_label_serial_number), driveUnit.serialNumber.orDash())
        builder.drawLabelValueRow(
            s(R.string.dashboard_label_odometer),
            driveUnit.odometerMeters?.div(1000.0)?.let { formatDistance(it, locale) }.orDash(),
        )
        builder.drawLabelValueRow(
            s(R.string.dashboard_label_total_power_on_hours),
            driveUnit.totalPowerOnHours?.let { "$it h" }.orDash(),
        )
        builder.drawLabelValueRow(
            s(R.string.dashboard_label_max_assist),
            driveUnit.maximumAssistanceSpeedKmh?.let { formatSpeed(it, locale) }.orDash(),
        )
        builder.drawLabelValueRow(
            s(R.string.dashboard_label_assist_modes),
            driveUnit.activeAssistModes.takeIf { it.isNotEmpty() }?.joinToString(", ") { it.toReadableAssistMode(locale) }.orDash(),
        )
    }

    private fun drawBatteries(
        builder: PdfPageBuilder,
        batteries: List<BoschBattery>,
        locale: Locale,
    ) {
        builder.drawSectionHeader(s(R.string.dashboard_section_batteries))
        if (batteries.isEmpty()) {
            builder.drawBodyText(s(R.string.pdf_value_not_available))
            return
        }
        batteries.forEachIndexed { index, battery ->
            builder.drawBodyText(s(R.string.dashboard_battery_prefix, arrayOf(index + 1)))
            builder.drawLabelValueRow(s(R.string.dashboard_label_product), battery.productName.orDash())
            builder.drawLabelValueRow(
                s(R.string.dashboard_battery_prefix_total_cycles, arrayOf(index + 1)),
                battery.totalChargeCycles?.toInt()?.toString().orDash(),
            )
            builder.drawLabelValueRow(
                s(R.string.dashboard_battery_prefix_on_bike_cycles, arrayOf(index + 1)),
                battery.onBikeChargeCycles?.toInt()?.toString().orDash(),
            )
            builder.drawLabelValueRow(
                s(R.string.dashboard_battery_prefix_off_bike_cycles, arrayOf(index + 1)),
                battery.offBikeChargeCycles?.toInt()?.toString().orDash(),
            )
            builder.drawLabelValueRow(
                s(R.string.dashboard_battery_prefix_energy, arrayOf(index + 1)),
                battery.deliveredWhOverLifetime?.let { "$it Wh" }.orDash(),
            )
        }
    }

    private fun drawComponent(
        builder: PdfPageBuilder,
        title: String,
        component: BoschComponent?,
    ) {
        builder.drawSectionHeader(title)
        if (component == null) {
            builder.drawBodyText(s(R.string.pdf_value_not_available))
            return
        }
        builder.drawLabelValueRow(s(R.string.dashboard_label_product), component.productName.orDash())
        builder.drawLabelValueRow(s(R.string.dashboard_label_serial_number), component.serialNumber.orDash())
    }

    private fun Map<Int, Int>.toPdfFrequencyRows(): List<Pair<String, Int>> {
        val overflowCount = entries.filter { it.key >= 3 }.sumOf { it.value }
        val visibleRows = entries.filter { it.key < 3 }.sortedBy { it.key }.map { (toursPerWeek, weekCount) ->
            val label = when (toursPerWeek) {
                0 -> s(R.string.statistics_highlights_freq_row_zero)
                1 -> s(R.string.statistics_highlights_freq_row_one)
                else -> s(R.string.statistics_highlights_freq_row_n, arrayOf(toursPerWeek))
            }
            label to weekCount
        }
        return buildList {
            addAll(visibleRows)
            if (overflowCount > 0) {
                add(s(R.string.statistics_highlights_freq_row_overflow, arrayOf(3)) to overflowCount)
            }
        }
    }

    private fun BoschAssistMode.toReadableAssistMode(locale: Locale): String =
        reachableRangeKm?.let { "$name (${formatDistance(it, locale)})" } ?: name

    private fun DayOfWeek.toLocalizedDay(locale: Locale): String =
        getDisplayName(java.time.format.TextStyle.FULL, locale)

    private fun formatDateTime(
        instant: Instant,
        locale: Locale,
        zoneId: ZoneId,
    ): String =
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
            .withLocale(locale)
            .format(instant.atZone(zoneId))

    private fun formatPeriod(
        start: Instant?,
        end: Instant?,
        locale: Locale,
        zoneId: ZoneId,
    ): String {
        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
        val startLabel = start?.atZone(zoneId)?.toLocalDate()?.format(formatter)
        val endLabel = end?.atZone(zoneId)?.toLocalDate()?.format(formatter)
        return listOfNotNull(startLabel, endLabel).takeIf { it.isNotEmpty() }?.joinToString(" - ").orDash()
    }

    private fun formatDistance(distanceKm: Double, locale: Locale): String =
        String.format(locale, "%.1f km", distanceKm)

    private fun formatHours(hours: Double, locale: Locale): String =
        String.format(locale, "%.1f h", hours)

    private fun formatSpeed(speedKmh: Double, locale: Locale): String =
        String.format(locale, "%.1f km/h", speedKmh)

    private fun formatWatts(watts: Double, locale: Locale): String =
        String.format(locale, "%.0f W", watts)

    private fun formatMeters(meters: Int, locale: Locale): String =
        String.format(locale, "%,d m", meters)

    private fun formatCalories(calories: Double, locale: Locale): String =
        String.format(locale, "%,.0f kcal", calories)

    private fun String?.orDash(): String = this?.takeIf { it.isNotBlank() } ?: s(R.string.pdf_value_not_available)

    private fun s(resId: Int, args: Array<out Any> = emptyArray()): String =
        stringResolver.get(resId, args)
}
