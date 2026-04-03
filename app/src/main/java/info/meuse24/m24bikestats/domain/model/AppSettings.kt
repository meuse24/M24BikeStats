package info.meuse24.m24bikestats.domain.model

import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

data class AppSettings(
    val csvExportFormat: CsvExportFormat = CsvExportFormat.SYSTEM_DEFAULT,
)

enum class CsvSeparator(
    val character: Char,
) {
    SEMICOLON(
        character = ';',
    ),
    COMMA(
        character = ',',
    ),
    ;

    companion object {
        fun fromStoredValue(value: String?): CsvSeparator? =
            entries.firstOrNull { separator -> separator.name == value }
    }
}

object CsvSeparatorDefaults {
    fun forLocale(locale: Locale): CsvSeparator = CsvExportFormatDefaults.forLocale(locale).resolve(locale).separator
}

enum class CsvExportFormat {
    SYSTEM_DEFAULT,
    EXCEL_DE,
    STANDARD_INTERNATIONAL,
    ;

    fun resolve(locale: Locale = Locale.getDefault()): CsvDialect = when (this) {
        SYSTEM_DEFAULT -> CsvExportFormatDefaults.forLocale(locale).resolve(locale)
        EXCEL_DE -> CsvDialect(separator = CsvSeparator.SEMICOLON, decimalSeparator = ',')
        STANDARD_INTERNATIONAL -> CsvDialect(separator = CsvSeparator.COMMA, decimalSeparator = '.')
    }

    companion object {
        fun fromStoredValue(value: String?): CsvExportFormat? =
            entries.firstOrNull { format -> format.name == value }
    }
}

data class CsvDialect(
    val separator: CsvSeparator,
    val decimalSeparator: Char,
) {
    fun row(values: List<String>): String =
        values.joinToString(separator.character.toString()) { value -> value.escapeCsv() }

    fun formatDecimal(
        value: Double,
        fractionDigits: Int,
    ): String {
        val formatted = String.format(Locale.US, "%.${fractionDigits}f", value)
        return if (decimalSeparator == '.') formatted else formatted.replace('.', decimalSeparator)
    }

    fun formatIsoDate(value: String): String =
        runCatching { LocalDate.parse(value).format(dateFormatter()) }.getOrDefault(value)

    fun formatIsoDateTime(value: String): String =
        runCatching { OffsetDateTime.parse(value).format(dateTimeFormatter()) }
            .recoverCatching {
                Instant.parse(value).atOffset(ZoneOffset.UTC).format(dateTimeFormatter())
            }
            .getOrDefault(value)

    private fun String.escapeCsv(): String {
        val escaped = replace("\"", "\"\"")
        return "\"$escaped\""
    }

    private fun dateFormatter(): DateTimeFormatter = when (separator) {
        CsvSeparator.SEMICOLON -> DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN)
        CsvSeparator.COMMA -> DateTimeFormatter.ISO_LOCAL_DATE
    }

    private fun dateTimeFormatter(): DateTimeFormatter = when (separator) {
        CsvSeparator.SEMICOLON -> DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss", Locale.GERMAN)
        CsvSeparator.COMMA -> DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.US)
    }
}

object CsvExportFormatDefaults {
    fun forLocale(locale: Locale): CsvExportFormat {
        val decimalSeparator = DecimalFormatSymbols.getInstance(locale).decimalSeparator
        return if (decimalSeparator == ',') CsvExportFormat.EXCEL_DE else CsvExportFormat.STANDARD_INTERNATIONAL
    }
}

fun CsvSeparator.toLegacyExportFormat(): CsvExportFormat = when (this) {
    CsvSeparator.SEMICOLON -> CsvExportFormat.EXCEL_DE
    CsvSeparator.COMMA -> CsvExportFormat.STANDARD_INTERNATIONAL
}
