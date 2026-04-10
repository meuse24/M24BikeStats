package info.meuse24.m24bikestats.data.repository

import info.meuse24.m24bikestats.data.local.dao.ActivityDetailDao
import info.meuse24.m24bikestats.data.local.dao.ActivityDao
import info.meuse24.m24bikestats.data.local.dao.BikeDao
import info.meuse24.m24bikestats.data.local.entity.ActivityCacheStateEntity
import info.meuse24.m24bikestats.data.local.entity.BikeCacheStateEntity
import info.meuse24.m24bikestats.data.local.mapper.ActivityCenterCalculator
import info.meuse24.m24bikestats.data.local.mapper.toAbsEntities
import info.meuse24.m24bikestats.data.local.mapper.toAssistModeEntities
import info.meuse24.m24bikestats.data.local.mapper.toBatteryEntities
import info.meuse24.m24bikestats.data.local.mapper.toBikePassEntity
import info.meuse24.m24bikestats.data.local.mapper.toDomain
import info.meuse24.m24bikestats.data.local.mapper.toEntity
import info.meuse24.m24bikestats.data.local.mapper.toPointEntities
import info.meuse24.m24bikestats.data.local.mapper.toRegistrationEntities
import info.meuse24.m24bikestats.data.local.mapper.toServiceRecordEntities
import info.meuse24.m24bikestats.data.local.mapper.toTheftReportLogEntities
import info.meuse24.m24bikestats.data.local.model.CachedBike
import info.meuse24.m24bikestats.data.remote.BoschApiDataSource
import info.meuse24.m24bikestats.data.remote.BoschJsonBodyExtractor
import info.meuse24.m24bikestats.data.remote.BoschSmartSystemParser
import info.meuse24.m24bikestats.api.BoschEndpoint
import info.meuse24.m24bikestats.domain.model.BoschApiRequest
import info.meuse24.m24bikestats.domain.model.BoschActivity
import info.meuse24.m24bikestats.domain.model.BoschActivityDetail
import info.meuse24.m24bikestats.domain.model.BoschActivityPage
import info.meuse24.m24bikestats.domain.model.BoschBike
import info.meuse24.m24bikestats.domain.model.BoschRegistration
import info.meuse24.m24bikestats.domain.model.CloudSyncDetailMode
import info.meuse24.m24bikestats.domain.model.ActivityDetailCacheOverview
import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemCacheStatusRepository
import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BoschSmartSystemRepositoryImpl(
    private val apiClient: BoschApiDataSource,
    private val activityDao: ActivityDao,
    private val activityDetailDao: ActivityDetailDao,
    private val bikeDao: BikeDao,
    private val parser: BoschSmartSystemParser = BoschSmartSystemParser(),
    private val jsonBodyExtractor: BoschJsonBodyExtractor = BoschJsonBodyExtractor(),
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : BoschSmartSystemRepository, BoschSmartSystemCacheStatusRepository {

    override fun observeCachedActivities(): Flow<List<BoschActivity>> =
        activityDao.observeAll().map { activities ->
            activities.map { it.toDomain() }
        }

    override fun observeCachedBikes(): Flow<List<BoschBike>> =
        bikeDao.observeAll().map { bikes -> bikes.map(CachedBike::toDomain) }

    override fun observeCachedActivityDetailCacheOverview(): Flow<ActivityDetailCacheOverview> =
        activityDetailDao.observeCacheOverview().map { overview ->
            ActivityDetailCacheOverview(
                detailedActivityCount = overview.detailedActivityCount,
                detailPointCount = overview.detailPointCount,
                gpsPointCount = overview.gpsPointCount,
            )
        }

    override fun observeActivityCacheUpdatedAtEpochMillis(): Flow<Long?> =
        activityDao.observeCacheUpdatedAtEpochMillis()

    override fun observeBikeCacheUpdatedAtEpochMillis(): Flow<Long?> =
        bikeDao.observeCacheUpdatedAtEpochMillis()

    override fun observeActivityDetailCacheUpdatedAtEpochMillis(): Flow<Long?> =
        activityDetailDao.observeMaxUpdatedAtEpochMillis()

    override fun observeCachedActivityDetail(activityId: String): Flow<BoschActivityDetail?> =
        activityDetailDao.observeByActivityId(activityId).map { detail ->
            detail?.toDomain()
        }

    override fun observeCachedBike(bikeId: String): Flow<BoschBike?> =
        bikeDao.observeById(bikeId).map { bike -> bike?.toDomain() }

    override suspend fun getCachedActivities(): List<BoschActivity> =
        activityDao.getAll().map { it.toDomain() }

    override suspend fun getCachedActivityTotalCount(): Int? =
        activityDao.getCachedTotalCount()

    override suspend fun getCachedActivity(activityId: String): BoschActivity? =
        activityDao.getById(activityId)?.toDomain()

    override suspend fun getCachedActivityDetail(activityId: String): BoschActivityDetail? =
        activityDetailDao.getByActivityId(activityId)?.toDomain()

    override suspend fun getCachedBike(bikeId: String): BoschBike? =
        bikeDao.getById(bikeId)?.toDomain()

    override suspend fun hasFreshActivities(maxAgeMillis: Long): Boolean =
        isFresh(activityDao.getCacheUpdatedAtEpochMillis(), maxAgeMillis)

    override suspend fun hasFreshActivityDetail(activityId: String, maxAgeMillis: Long): Boolean =
        isFresh(activityDetailDao.getUpdatedAtEpochMillis(activityId), maxAgeMillis)

    override suspend fun hasFreshBikes(maxAgeMillis: Long): Boolean =
        isFresh(bikeDao.getCacheUpdatedAtEpochMillis(), maxAgeMillis)

    override suspend fun hasFreshBikeDetail(bikeId: String, maxAgeMillis: Long): Boolean =
        isFresh(bikeDao.getUpdatedAtEpochMillis(bikeId), maxAgeMillis)

    override suspend fun getActivityIdsNeedingDetailSync(
        detailMode: CloudSyncDetailMode,
    ): List<String> {
        val metadataById = activityDetailDao.getAllMetadata().associateBy { it.activityId }
        return activityDao.getAll().map { it.id }.filter { activityId ->
            val metadata = metadataById[activityId]
            when (detailMode) {
                CloudSyncDetailMode.MISSING_ONLY -> metadata == null
            }
        }
    }

    override suspend fun getActivities(
        accessToken: String,
        limit: Int,
        offset: Int,
    ): Result<BoschActivityPage> =
        runCatching {
            val updatedAtEpochMillis = currentTimeMillis()
            val response = apiClient.get(
                BoschApiRequest(
                    label = BoschEndpoint.SMART_ACTIVITIES.label,
                    baseUrl = BoschEndpoint.SMART_ACTIVITIES.baseUrl,
                    path = "/activity/smart-system/v1/activities?limit=$limit&offset=$offset",
                ),
                accessToken
            )
            val json = jsonBodyExtractor.extract(response) ?: error("Keine Aktivitätendaten erhalten")
            parser.parseActivitiesPage(json, limit, offset).also { page ->
                activityDao.upsertAllPreservingCenter(
                    page.items.map { it.toEntity() }
                )
                activityDao.upsertCacheState(
                    ActivityCacheStateEntity(
                        totalCount = page.total,
                        updatedAtEpochMillis = updatedAtEpochMillis,
                    )
                )
            }
        }

    override suspend fun getBikes(accessToken: String): Result<List<BoschBike>> =
        runCatching {
            val updatedAtEpochMillis = currentTimeMillis()
            val response = apiClient.get(BoschEndpoint.SMART_BIKES.toRequest(), accessToken)
            val json = jsonBodyExtractor.extract(response) ?: error("Keine Bike-Daten erhalten")
            parser.parseBikes(json).also { bikes ->
                bikeDao.replaceAll(
                    bikes = bikes.map { it.toEntity(updatedAtEpochMillis) },
                    batteries = bikes.flatMap { it.toBatteryEntities() },
                    assistModes = bikes.flatMap { it.toAssistModeEntities() },
                    absComponents = bikes.flatMap { it.toAbsEntities() },
                    bikePass = null,
                    theftReportLogs = emptyList(),
                    serviceRecords = emptyList(),
                    registrations = emptyList(),
                    cacheState = BikeCacheStateEntity(updatedAtEpochMillis = updatedAtEpochMillis),
                )
            }
        }

    override suspend fun getActivityDetail(
        accessToken: String,
        activityId: String,
    ): Result<BoschActivityDetail> =
        runCatching {
            val response = apiClient.get(
                BoschEndpoint.SMART_ACTIVITY_DETAIL.toRequest(activityId = activityId),
                accessToken
            )
            val json = jsonBodyExtractor.extract(response) ?: error("Keine Aktivitätsdetaildaten erhalten")
            parser.parseActivityDetail(activityId, json).also { detail ->
                val points = detail.toPointEntities()
                activityDetailDao.replaceDetail(
                    detail = detail.toEntity(updatedAtEpochMillis = currentTimeMillis()),
                    points = points,
                )
                val gpsCoords = points
                    .filter { it.latitude != null && it.longitude != null }
                    .map { it.latitude!! to it.longitude!! }
                val center = ActivityCenterCalculator.calculate(gpsCoords)
                if (center != null) {
                    activityDao.updateCenter(activityId, center.first, center.second)
                } else {
                    activityDao.clearCenter(activityId)
                }
            }
        }

    override suspend fun getBikeDetail(accessToken: String, bikeId: String): Result<BoschBike> =
        runCatching {
            val updatedAtEpochMillis = currentTimeMillis()
            val cachedBike = bikeDao.getById(bikeId)?.toDomain()
            val response = apiClient.get(
                BoschEndpoint.SMART_BIKE_DETAIL.toRequest(bikeId = bikeId),
                accessToken
            )
            val json = jsonBodyExtractor.extract(response) ?: error("Keine Bike-Detaildaten erhalten")
            val bike = parser.parseBikeDetail(json)
            val bikePassResult = runCatching {
                val bikePassResponse = apiClient.get(
                    BoschEndpoint.SMART_BIKE_PASS.toRequest(bikeId = bikeId),
                    accessToken
                )
                val bikePassJson = jsonBodyExtractor.extract(bikePassResponse)
                    ?: error("Keine Bike-Pass-Daten erhalten")
                parser.parseBikePassData(bikePassJson, bikeId)
            }
            val serviceRecordsResult = runCatching {
                val serviceBookResponse = apiClient.get(
                    BoschEndpoint.SMART_SERVICE_RECORDS.toRequest(bikeId = bikeId),
                    accessToken
                )
                val serviceBookJson = jsonBodyExtractor.extract(serviceBookResponse)
                    ?: error("Keine Service-Book-Daten erhalten")
                parser.parseServiceRecords(serviceBookJson, bikeId)
            }
            val registrationsResult = runCatching {
                val registrationsResponse = apiClient.get(
                    BoschEndpoint.SMART_REGISTRATIONS.toRequest(),
                    accessToken,
                )
                val registrationsJson = jsonBodyExtractor.extract(registrationsResponse)
                    ?: error("Keine Registrierungsdaten erhalten")
                parser.parseRegistrations(registrationsJson).filter { registration ->
                    registration.bikeId == bikeId || bike.matchesRegistration(registration)
                }
            }
            bike.copy(
                bikePass = bikePassResult.fold(
                    onSuccess = { it.bikePass },
                    onFailure = { cachedBike?.bikePass },
                ),
                theftReportLogs = bikePassResult.fold(
                    onSuccess = { it.theftReportLogs },
                    onFailure = { cachedBike?.theftReportLogs.orEmpty() },
                ),
                serviceRecords = serviceRecordsResult.getOrElse {
                    cachedBike?.serviceRecords.orEmpty()
                },
                registrations = registrationsResult.getOrElse {
                    cachedBike?.registrations.orEmpty()
                },
            ).also { enrichedBike ->
                bikeDao.replaceBike(
                    bike = enrichedBike.toEntity(updatedAtEpochMillis),
                    batteries = enrichedBike.toBatteryEntities(),
                    assistModes = enrichedBike.toAssistModeEntities(),
                    absComponents = enrichedBike.toAbsEntities(),
                    bikePass = enrichedBike.toBikePassEntity(),
                    theftReportLogs = enrichedBike.toTheftReportLogEntities(),
                    serviceRecords = enrichedBike.toServiceRecordEntities(),
                    registrations = enrichedBike.toRegistrationEntities(),
                )
            }
        }

    private fun currentTimeMillis(): Long = nowMillis()

    private fun isFresh(updatedAtEpochMillis: Long?, maxAgeMillis: Long): Boolean {
        if (updatedAtEpochMillis == null) return false
        return currentTimeMillis() - updatedAtEpochMillis <= maxAgeMillis
    }

    private fun BoschBike.matchesRegistration(registration: BoschRegistration): Boolean {
        val serialNumber = registration.serialNumber ?: return false
        val partNumber = registration.partNumber ?: return false
        return buildList {
            driveUnit?.let { add(it.serialNumber to it.partNumber) }
            connectModule?.let { add(it.serialNumber to it.partNumber) }
            batteries.forEach { add(it.serialNumber to it.partNumber) }
        }.any { (componentSerial, componentPart) ->
            componentSerial == serialNumber && componentPart == partNumber
        }
    }
}
