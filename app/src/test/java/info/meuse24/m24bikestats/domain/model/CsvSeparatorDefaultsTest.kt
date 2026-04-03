package info.meuse24.m24bikestats.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class CsvSeparatorDefaultsTest {

    @Test
    fun `uses semicolon for locales with decimal comma`() {
        assertEquals(CsvSeparator.SEMICOLON, CsvSeparatorDefaults.forLocale(Locale.GERMAN))
    }

    @Test
    fun `uses comma for locales with decimal point`() {
        assertEquals(CsvSeparator.COMMA, CsvSeparatorDefaults.forLocale(Locale.ENGLISH))
    }
}
