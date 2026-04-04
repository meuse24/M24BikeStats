package info.meuse24.m24bikestats.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BackgroundSyncModeTest {

    @Test
    fun `from stored value resolves matching enum`() {
        assertEquals(BackgroundSyncMode.DAILY_UNMETERED, BackgroundSyncMode.fromStoredValue("DAILY_UNMETERED"))
        assertEquals(BackgroundSyncMode.DAILY_CONNECTED, BackgroundSyncMode.fromStoredValue("DAILY_CONNECTED"))
    }

    @Test
    fun `from stored value returns null for unknown value`() {
        assertNull(BackgroundSyncMode.fromStoredValue("INVALID"))
        assertNull(BackgroundSyncMode.fromStoredValue(null))
    }
}
