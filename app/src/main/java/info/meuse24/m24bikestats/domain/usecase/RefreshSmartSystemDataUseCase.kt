package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.auth.OidcDiscoveryInfoProvider
import info.meuse24.m24bikestats.auth.OidcUserInfoProvider
import info.meuse24.m24bikestats.domain.model.BoschActivity
import info.meuse24.m24bikestats.domain.model.CloudSyncDetailMode
import info.meuse24.m24bikestats.domain.model.SmartSystemCloudSyncPhase
import info.meuse24.m24bikestats.domain.model.SmartSystemCloudSyncProgress
import info.meuse24.m24bikestats.domain.model.SmartSystemCloudSyncSummary
import info.meuse24.m24bikestats.domain.repository.AppSettingsRepository
import info.meuse24.m24bikestats.domain.repository.AuthRepository
import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemCacheStatusRepository
import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemRepository
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

class RefreshSmartSystemDataUseCase(
    private val repository: BoschSmartSystemRepository,
    private val cacheStatusRepository: BoschSmartSystemCacheStatusRepository,
    private val authRepository: AuthRepository,
    private val appSettingsRepository: AppSettingsRepository,
    private val oidcUserInfoProvider: OidcUserInfoProvider,
    private val oidcDiscoveryInfoProvider: OidcDiscoveryInfoProvider,
) {
    suspend operator fun invoke(
        onProgress: (SmartSystemCloudSyncProgress) -> Unit = {},
    ): Result<SmartSystemCloudSyncSummary> = withValidAccessToken(authRepository) { token ->
        onProgress(
            SmartSystemCloudSyncProgress(
                phase = SmartSystemCloudSyncPhase.BIKES,
                processedCount = 0,
                totalCount = 1,
            ),
        )
        val bikes = repository.getBikes(token)
            .getOrElse { return@withValidAccessToken Result.failure(it) }
        onProgress(
            SmartSystemCloudSyncProgress(
                phase = SmartSystemCloudSyncPhase.BIKES,
                processedCount = 1,
                totalCount = 1,
            ),
        )

        runCatching { oidcUserInfoProvider.loadCurrentUserInfo() }
        runCatching { oidcDiscoveryInfoProvider.loadCurrentDiscovery() }

        val cachedActivities = repository.getCachedActivities()
        val knownActivityIds = cachedActivities
            .asSequence()
            .map(BoschActivity::id)
            .toMutableSet()
        val checkpointEpochMillis = appSettingsRepository.getSettings().latestCachedActivityStartTimeMillis
            .takeIf { it > 0L }
            ?: cachedActivities.mapNotNull(BoschActivity::startTimeEpochMillisOrNull).maxOrNull()
            ?: 0L

        var offset = 0
        var loadedNewActivityCount = 0
        var pageCount = 0
        var reachedCheckpoint = false

        while (pageCount < MAX_DELTA_PAGES && !reachedCheckpoint) {
            coroutineContext.ensureActive()
            val page = repository.getActivities(
                accessToken = token,
                limit = PAGE_SIZE,
                offset = offset,
            ).getOrElse { return@withValidAccessToken Result.failure(it) }
            if (page.items.isEmpty()) break

            pageCount += 1
            var pageEncounteredCheckpoint = false
            val newItemsOnPage = buildList {
                for (item in page.items) {
                    val isKnown = item.id in knownActivityIds
                    val isAtOrBeforeCheckpoint = checkpointEpochMillis <= 0L ||
                        item.startTimeEpochMillisOrNull()?.let { startTime -> startTime <= checkpointEpochMillis } == true
                    if (isKnown && isAtOrBeforeCheckpoint) {
                        pageEncounteredCheckpoint = true
                        break
                    }
                    if (!isKnown) {
                        add(item)
                    }
                }
            }

            if (newItemsOnPage.isNotEmpty()) {
                knownActivityIds += newItemsOnPage.map(BoschActivity::id)
                loadedNewActivityCount += newItemsOnPage.size
            }

            reachedCheckpoint = pageEncounteredCheckpoint || page.items.size < PAGE_SIZE
            val progressTotal = if (reachedCheckpoint || pageCount >= MAX_DELTA_PAGES) {
                loadedNewActivityCount
            } else {
                loadedNewActivityCount + PAGE_SIZE
            }
            onProgress(
                SmartSystemCloudSyncProgress(
                    phase = SmartSystemCloudSyncPhase.ACTIVITIES,
                    processedCount = loadedNewActivityCount,
                    totalCount = progressTotal,
                ),
            )
            offset = page.offset + page.items.size
        }

        val detailCandidates = cacheStatusRepository.getActivityIdsNeedingDetailSync(
            detailMode = CloudSyncDetailMode.MISSING_ONLY,
        )
        if (detailCandidates.isNotEmpty()) {
            onProgress(
                SmartSystemCloudSyncProgress(
                    phase = SmartSystemCloudSyncPhase.ACTIVITY_DETAILS,
                    processedCount = 0,
                    totalCount = detailCandidates.size,
                ),
            )
        }

        detailCandidates.forEachIndexed { index, activityId ->
            coroutineContext.ensureActive()
            repository.getActivityDetail(token, activityId)
                .getOrElse { return@withValidAccessToken Result.failure(it) }
            onProgress(
                SmartSystemCloudSyncProgress(
                    phase = SmartSystemCloudSyncPhase.ACTIVITY_DETAILS,
                    processedCount = index + 1,
                    totalCount = detailCandidates.size,
                ),
            )
        }

        val latestActivityStartTimeMillis = repository.getCachedActivities()
            .mapNotNull(BoschActivity::startTimeEpochMillisOrNull)
            .maxOrNull()
            ?: 0L
        appSettingsRepository.updateLatestCachedActivityStartTime(latestActivityStartTimeMillis)

        Result.success(
            SmartSystemCloudSyncSummary(
                activityCount = knownActivityIds.size,
                bikeCount = bikes.size,
            ),
        )
    }

    private companion object {
        private const val PAGE_SIZE = 20
        private const val MAX_DELTA_PAGES = 5
    }
}
