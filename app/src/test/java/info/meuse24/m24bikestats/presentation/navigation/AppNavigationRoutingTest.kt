package info.meuse24.m24bikestats.presentation.navigation

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
    fun `drawer routes expose matching top bar title`() {
        assertEquals(
            null,
            DrawerDestination.HELP.route.toMainDestination(),
        )
        assertEquals(
            DrawerDestination.HELP.label,
            DrawerDestination.HELP.route.toTopBarTitle(),
        )
        assertEquals(
            DrawerDestination.API_TEST.label,
            DrawerDestination.API_TEST.route.toTopBarTitle(),
        )
    }

    @Test
    fun `detail routes hide shell top bar`() {
        assertFalse("activity/abc".shouldShowShellTopBar())
        assertFalse("bike/bike-1".shouldShowShellTopBar())
        assertTrue(null.shouldShowShellTopBar())
        assertTrue(MainDestination.HOME.route.shouldShowShellTopBar())
    }

    @Test
    fun `refresh action is only shown on primary data routes`() {
        assertTrue(MainDestination.HOME.route.shouldShowRefreshAction())
        assertTrue(MainDestination.ACTIVITIES.route.shouldShowRefreshAction())
        assertTrue(MainDestination.BIKE.route.shouldShowRefreshAction())
        assertFalse(MainDestination.FUNCTIONS.route.shouldShowRefreshAction())
        assertFalse(DrawerDestination.INFO.route.shouldShowRefreshAction())
    }

    @Test
    fun `logout is an action and not a navigation route`() {
        assertEquals(null, DrawerDestination.LOGOUT.route)
        assertFalse(DrawerDestination.LOGOUT.isNavigationDestination)
    }

    @Test
    fun `all non home shell routes can navigate back to overview`() {
        assertFalse(MainDestination.HOME.route.canNavigateToOverview())
        assertTrue(MainDestination.ACTIVITIES.route.canNavigateToOverview())
        assertTrue(MainDestination.BIKE.route.canNavigateToOverview())
        assertTrue(MainDestination.FUNCTIONS.route.canNavigateToOverview())
        assertFalse(DrawerDestination.HELP.route.canNavigateToOverview())
        assertFalse(DrawerDestination.INFO.route.canNavigateToOverview())
        assertFalse(DrawerDestination.API_TEST.route.canNavigateToOverview())
    }
}
