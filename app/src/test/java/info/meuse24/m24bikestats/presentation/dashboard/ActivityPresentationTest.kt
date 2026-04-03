package info.meuse24.m24bikestats.presentation.dashboard

import org.junit.Assert.assertEquals
import org.junit.Test

class ActivityPresentationTest {

    @Test
    fun `filters activities to last 30 days`() {
        val now = 1_000_000_000L
        val activities = listOf(
            testActivityCard(id = "recent", startedAtEpochMillis = now - (10L * 24L * 60L * 60L * 1000L)),
            testActivityCard(id = "old", startedAtEpochMillis = now - (60L * 24L * 60L * 60L * 1000L)),
        )

        val result = filterAndSortActivities(
            activities = activities,
            searchQuery = "",
            dateRangeFilter = ActivityDateRangeFilter.LAST_30_DAYS,
            sortOption = ActivitySortOption.NEWEST_FIRST,
            nowMillis = now,
        )

        assertEquals(listOf("recent"), result.map { it.id })
    }

    @Test
    fun `sorts by longest distance descending`() {
        val activities = listOf(
            testActivityCard(id = "short", distanceMeters = 1000, durationSeconds = 100),
            testActivityCard(id = "long", distanceMeters = 5000, durationSeconds = 200),
            testActivityCard(id = "mid", distanceMeters = 3000, durationSeconds = 300),
        )

        val result = filterAndSortActivities(
            activities = activities,
            searchQuery = "",
            dateRangeFilter = ActivityDateRangeFilter.ALL,
            sortOption = ActivitySortOption.LONGEST_DISTANCE,
        )

        assertEquals(listOf("long", "mid", "short"), result.map { it.id })
    }

    @Test
    fun `sorts by oldest first`() {
        val activities = listOf(
            testActivityCard(id = "newest", startedAtEpochMillis = 3000L),
            testActivityCard(id = "oldest", startedAtEpochMillis = 1000L),
            testActivityCard(id = "middle", startedAtEpochMillis = 2000L),
        )

        val result = filterAndSortActivities(
            activities = activities,
            searchQuery = "",
            dateRangeFilter = ActivityDateRangeFilter.ALL,
            sortOption = ActivitySortOption.OLDEST_FIRST,
        )

        assertEquals(listOf("oldest", "middle", "newest"), result.map { it.id })
    }

    @Test
    fun `filters activities by search query over title`() {
        val activities = listOf(
            testActivityCard(id = "a", title = "Abendrunde"),
            testActivityCard(id = "b", title = "Pendelfahrt"),
        )

        val result = filterAndSortActivities(
            activities = activities,
            searchQuery = "abend",
            dateRangeFilter = ActivityDateRangeFilter.ALL,
            sortOption = ActivitySortOption.NEWEST_FIRST,
        )

        assertEquals(listOf("a"), result.map { it.id })
    }

    private fun testActivityCard(
        id: String,
        title: String = id,
        startedAtEpochMillis: Long = 1000L,
        distanceMeters: Int = 1000,
        durationSeconds: Int = 100,
    ) = ActivityCardUiModel(
        id = id,
        title = title,
        startedAt = id,
        startedAtEpochMillis = startedAtEpochMillis,
        distanceMeters = distanceMeters,
        durationSeconds = durationSeconds,
        dateLabel = id,
        distanceLabel = "$distanceMeters m",
        durationLabel = "$durationSeconds s",
        speedLabel = "speed",
        powerLabel = null,
        elevationLabel = null,
        caloriesLabel = null,
    )
}
