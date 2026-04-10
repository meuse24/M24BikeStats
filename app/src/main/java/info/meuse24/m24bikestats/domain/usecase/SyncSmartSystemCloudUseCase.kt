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
import java.time.Instant
import java.time.OffsetDateTime
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

class SyncSmartSystemCloudUseCase(
    private val repository: BoschSmartSystemRepository,
    private val cacheStatusRepository: BoschSmartSystemCacheStatusRepository,
    private val authRepository: AuthRepository,
    private val appSettingsRepository: AppSettingsRepository,
    private val oidcUserInfoProvider: OidcUserInfoProvider,
    private val oidcDiscoveryInfoProvider: OidcDiscoveryInfoProvider,
    private val nowMillis: () -> Long = System::currentTimeMillis,
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

        val knownActivityIds = repository.getCachedActivities()
            .asSequence()
            .map(BoschActivity::id)
            .toMutableSet()
        var offset = 0
        var totalActivityCount = cacheStatusRepository.getCachedActivityTotalCount() ?: knownActivityIds.size

        do {
            coroutineContext.ensureActive()
            val page = repository.getActivities(
                accessToken = token,
                limit = SYNC_PAGE_SIZE,
                offset = offset,
            ).getOrElse { return@withValidAccessToken Result.failure(it) }

            totalActivityCount = page.total
            if (page.items.isEmpty()) break

            knownActivityIds += page.items.map(BoschActivity::id)
            onProgress(
                SmartSystemCloudSyncProgress(
                    phase = SmartSystemCloudSyncPhase.ACTIVITIES,
                    processedCount = knownActivityIds.size.coerceAtMost(totalActivityCount),
                    totalCount = totalActivityCount,
                ),
            )
            offset = page.offset + page.items.size
        } while (offset < totalActivityCount && knownActivityIds.size < totalActivityCount)

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
        appSettingsRepository.markInitialSyncCompleted(nowMillis())

        Result.success(
            SmartSystemCloudSyncSummary(
                activityCount = knownActivityIds.size,
                bikeCount = bikes.size,
            ),
        )
    }

    private companion object {
        private const val SYNC_PAGE_SIZE = 100
    }
}

internal fun BoschActivity.startTimeEpochMillisOrNull(): Long? =
    runCatching { OffsetDateTime.parse(startTime).toInstant().toEpochMilli() }
        .recoverCatching { Instant.parse(startTime).toEpochMilli() }
        .getOrNull()
