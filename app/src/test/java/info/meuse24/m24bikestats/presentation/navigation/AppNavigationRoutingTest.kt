package info.meuse24.m24bikestats.presentation.navigation

import info.meuse24.m24bikestats.R
import info.meuse24.m24bikestats.presentation.navigation.model.DrawerDestination
import info.meuse24.m24bikestats.presentation.navigation.model.MainDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNavigationRoutingTest {

    @Test
    fun `null route maps to home destination`() {
        assertEquals(null, null.toMainDestination())
    }

    @Test
    fun `unknown route maps to home destination`() {
        assertEquals(null, "foobar".toMainDestination())
    }

    @Test
    fun `activity detail route maps to activities destination`() {
        assertEquals(
            MainDestination.ACTIVITIES,
            "activity/abc-123".toMainDestination(),
        )
    }

    @Test
    fun `bike detail route maps to bike destination`() {
        assertEquals(
            MainDestination.BIKE,
            "bike/bike-42".toMainDestination(),
        )
    }

    @Test
    fun `map route has correct top bar title`() {
        assertEquals(R.string.nav_map, "map".toTopBarTitleRes())
    }

    @Test
    fun `drawer routes expose matching top bar title`() {
        assertEquals(
            null,
            DrawerDestination.EXPORT.route.toMainDestination(),
        )
        assertEquals(
            DrawerDestination.EXPORT.labelRes,
            DrawerDestination.EXPORT.route.toTopBarTitleRes(),
        )
        assertEquals(
            null,
            DrawerDestination.SETUP.route.toMainDestination(),
        )
        assertEquals(
            DrawerDestination.SETUP.labelRes,
            DrawerDestination.SETUP.route.toTopBarTitleRes(),
        )
        assertEquals(
            null,
            DrawerDestination.HELP.route.toMainDestination(),
        )
        assertEquals(
            DrawerDestination.HELP.labelRes,
            DrawerDestination.HELP.route.toTopBarTitleRes(),
        )
        assertEquals(
            DrawerDestination.API_TEST.labelRes,
            DrawerDestination.API_TEST.route.toTopBarTitleRes(),
        )
    }

    @Test
    fun `unknown route falls back to home top bar title resource`() {
        assertEquals(R.string.nav_home, "foobar".toTopBarTitleRes())
    }

    @Test
    fun `detail routes hide shell top bar`() {
        assertFalse("activity/abc".shouldShowShellTopBar())
        assertFalse("bike/bike-1".shouldShowShellTopBar())
        assertTrue(null.shouldShowShellTopBar())
        assertTrue(MainDestination.HOME.route.shouldShowShellTopBar())
    }

    @Test
    fun `pdf export action is only shown on home route`() {
        assertTrue(MainDestination.HOME.route.shouldShowPdfExportAction())
        assertFalse(MainDestination.ACTIVITIES.route.shouldShowPdfExportAction())
        assertFalse(MainDestination.BIKE.route.shouldShowPdfExportAction())
        assertFalse(MainDestination.STATISTICS.route.shouldShowPdfExportAction())
        assertFalse(DrawerDestination.EXPORT.route.shouldShowPdfExportAction())
    }

    @Test
    fun `logout is an action and not a navigation route`() {
        assertEquals(null, DrawerDestination.LOGOUT.route)
        assertFalse(DrawerDestination.LOGOUT.isNavigationDestination)
    }

    @Test
    fun `debug destinations are excluded from release drawer entries`() {
        assertTrue(DrawerDestination.availableEntries(includeDebugTools = true).contains(DrawerDestination.API_TEST))
        assertFalse(DrawerDestination.availableEntries(includeDebugTools = false).contains(DrawerDestination.API_TEST))
    }

    @Test
    fun `home stays the only primary destination without overview back affordance`() {
        assertFalse(MainDestination.HOME.route.canNavigateToOverview())
        assertTrue(MainDestination.ACTIVITIES.route.canNavigateToOverview())
        assertTrue(MainDestination.BIKE.route.canNavigateToOverview())
        assertTrue(MainDestination.STATISTICS.route.canNavigateToOverview())
    }

    @Test
    fun `all non home shell routes can navigate back to overview`() {
        assertFalse(MainDestination.HOME.route.canNavigateToOverview())
        assertTrue(MainDestination.ACTIVITIES.route.canNavigateToOverview())
        assertTrue(MainDestination.BIKE.route.canNavigateToOverview())
        assertTrue(MainDestination.STATISTICS.route.canNavigateToOverview())
        assertFalse(DrawerDestination.EXPORT.route.canNavigateToOverview())
        assertFalse(DrawerDestination.SETUP.route.canNavigateToOverview())
        assertFalse(DrawerDestination.HELP.route.canNavigateToOverview())
        assertFalse(DrawerDestination.INFO.route.canNavigateToOverview())
        assertFalse(DrawerDestination.API_TEST.route.canNavigateToOverview())
    }

    @Test
    fun `drawer destinations are ordered by utility support diagnostics and logout`() {
        assertEquals(
            listOf(
                DrawerDestination.EXPORT,
                DrawerDestination.SETUP,
                DrawerDestination.HELP,
                DrawerDestination.INFO,
                DrawerDestination.API_TEST,
                DrawerDestination.LOGOUT,
            ),
            DrawerDestination.availableEntries(includeDebugTools = true),
        )
    }
}
