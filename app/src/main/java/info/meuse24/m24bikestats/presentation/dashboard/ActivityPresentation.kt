package info.meuse24.m24bikestats.presentation.dashboard

private const val DAY_IN_MILLIS = 24L * 60L * 60L * 1000L

internal fun filterAndSortActivities(
    activities: List<ActivityCardUiModel>,
    searchQuery: String,
    dateRangeFilter: ActivityDateRangeFilter,
    sortOption: ActivitySortOption,
    nowMillis: Long = System.currentTimeMillis(),
): List<ActivityCardUiModel> {
    val normalizedQuery = searchQuery.trim().lowercase()

    val filtered = activities.filter { activity ->
        val matchesQuery = if (normalizedQuery.isBlank()) {
            true
        } else {
            activity.title.lowercase().contains(normalizedQuery) ||
                activity.dateLabel.lowercase().contains(normalizedQuery) ||
                activity.distanceLabel.lowercase().contains(normalizedQuery)
        }

        if (!matchesQuery) return@filter false

        when (dateRangeFilter) {
            ActivityDateRangeFilter.ALL -> true
            ActivityDateRangeFilter.LAST_30_DAYS -> {
                val startedAt = activity.startedAtEpochMillis ?: return@filter false
                startedAt >= nowMillis - (30L * DAY_IN_MILLIS)
            }
            ActivityDateRangeFilter.LAST_12_MONTHS -> {
                val startedAt = activity.startedAtEpochMillis ?: return@filter false
                startedAt >= nowMillis - (365L * DAY_IN_MILLIS)
            }
        }
    }

    return when (sortOption) {
        ActivitySortOption.NEWEST_FIRST -> filtered.sortedWith(
            compareByDescending<ActivityCardUiModel> { it.startedAtEpochMillis ?: Long.MIN_VALUE }
                .thenByDescending { it.distanceMeters }
        )
        ActivitySortOption.OLDEST_FIRST -> filtered.sortedWith(
            compareBy<ActivityCardUiModel> { it.startedAtEpochMillis ?: Long.MAX_VALUE }
                .thenByDescending { it.distanceMeters }
        )
        ActivitySortOption.LONGEST_DISTANCE -> filtered.sortedWith(
            compareByDescending<ActivityCardUiModel> { it.distanceMeters }
                .thenByDescending { it.startedAtEpochMillis ?: Long.MIN_VALUE }
        )
        ActivitySortOption.LONGEST_DURATION -> filtered.sortedWith(
            compareByDescending<ActivityCardUiModel> { it.durationSeconds }
                .thenByDescending { it.startedAtEpochMillis ?: Long.MIN_VALUE }
        )
    }
}
