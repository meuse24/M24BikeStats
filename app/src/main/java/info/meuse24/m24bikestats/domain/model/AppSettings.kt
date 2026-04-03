package info.meuse24.m24bikestats.domain.model

import java.text.DecimalFormatSymbols
import java.util.Locale

data class AppSettings(
    val csvSeparator: CsvSeparator = CsvSeparatorDefaults.forLocale(Locale.getDefault()),
)

enum class CsvSeparator(
    val character: Char,
    val label: String,
    val description: String,
) {
    SEMICOLON(
        character = ';',
        label = "Semikolon (;)",
        description = "Typisch für Sprachen mit Dezimalkomma, z. B. Deutsch.",
    ),
    COMMA(
        character = ',',
        label = "Komma (,)",
        description = "Typisch für Sprachen mit Dezimalpunkt, z. B. Englisch.",
    ),
    ;

    companion object {
        fun fromStoredValue(value: String?): CsvSeparator? =
            entries.firstOrNull { separator -> separator.name == value }
    }
}

object CsvSeparatorDefaults {
    fun forLocale(locale: Locale): CsvSeparator {
        val decimalSeparator = DecimalFormatSymbols.getInstance(locale).decimalSeparator
        return if (decimalSeparator == ',') CsvSeparator.SEMICOLON else CsvSeparator.COMMA
    }
}
