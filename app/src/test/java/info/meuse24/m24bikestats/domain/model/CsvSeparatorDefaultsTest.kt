package info.meuse24.m24bikestats.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class CsvExportFormatDefaultsTest {

    @Test
    fun `uses system default preset for settings`() {
        assertEquals(CsvExportFormat.SYSTEM_DEFAULT, AppSettings().csvExportFormat)
    }

    @Test
    fun `uses excel de for locales with decimal comma`() {
        assertEquals(CsvExportFormat.EXCEL_DE, CsvExportFormatDefaults.forLocale(Locale.GERMAN))
    }

    @Test
    fun `uses standard international for locales with decimal point`() {
        assertEquals(CsvExportFormat.STANDARD_INTERNATIONAL, CsvExportFormatDefaults.forLocale(Locale.ENGLISH))
    }
}
