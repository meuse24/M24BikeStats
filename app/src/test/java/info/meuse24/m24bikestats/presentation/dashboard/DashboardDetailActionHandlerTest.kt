package info.meuse24.m24bikestats.presentation.dashboard

import info.meuse24.m24bikestats.auth.OidcCertificateInfoProvider
import info.meuse24.m24bikestats.auth.OidcCertificateInfoUiModel
import info.meuse24.m24bikestats.auth.OidcDiscoveryInfoProvider
import info.meuse24.m24bikestats.auth.OidcDiscoveryInfoUiModel
import info.meuse24.m24bikestats.auth.OidcUserInfoProvider
import info.meuse24.m24bikestats.auth.OidcUserInfoUiModel
import info.meuse24.m24bikestats.domain.model.ActivityDetailCacheOverview
import info.meuse24.m24bikestats.domain.model.BoschActivity
import info.meuse24.m24bikestats.domain.model.BoschActivityDetail
import info.meuse24.m24bikestats.domain.model.BoschActivityPage
import info.meuse24.m24bikestats.domain.model.BoschBike
import info.meuse24.m24bikestats.domain.model.CloudSyncDetailMode
import info.meuse24.m24bikestats.domain.repository.AuthRepository
import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemCacheStatusRepository
import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemRepository
import info.meuse24.m24bikestats.domain.usecase.GetCachedSmartSystemActivityDetailUseCase
import info.meuse24.m24bikestats.domain.usecase.GetCachedSmartSystemActivityUseCase
import info.meuse24.m24bikestats.domain.usecase.GetCachedSmartSystemBikeUseCase
import info.meuse24.m24bikestats.domain.usecase.ObserveCachedSmartSystemActivityDetailUseCase
import info.meuse24.m24bikestats.domain.usecase.ObserveCachedSmartSystemBikeDetailUseCase
import info.meuse24.m24bikestats.domain.usecase.RefreshSmartSystemActivityDetailUseCase
import info.meuse24.m24bikestats.domain.usecase.RefreshSmartSystemBikeDetailUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardDetailActionHandlerTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `latest bike detail load wins over canceled earlier oidc request`() = runTest {
        val repository = HandlerFakeRepository().apply {
            putBike(bike(id = "bike-1"))
        }
        val certificateProvider = SequencedCertificateInfoProvider(
            first = CompletableDeferred<OidcCertificateInfoUiModel?>(),
            second = certificateInfo(
                keyId = "kid-second",
                tokenKeyId = "token-second",
            ),
        )
        val handler = createHandler(
            repository = repository,
            certificateProvider = certificateProvider,
            userInfoProvider = ImmediateUserInfoProvider(),
            discoveryInfoProvider = ImmediateDiscoveryInfoProvider(),
        )
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
        var state = DashboardUiState()

        handler.loadBikeDetail(
            scope = scope,
            bikeId = "bike-1",
            force = false,
            currentState = { state },
            updateState = { transform -> state = transform(state) },
        )
        advanceUntilIdle()
        assertTrue(certificateProvider.callCount >= 1)

        handler.loadBikeDetail(
            scope = scope,
            bikeId = "bike-1",
            force = true,
            currentState = { state },
            updateState = { transform -> state = transform(state) },
        )
        advanceUntilIdle()

        assertHasOidcKeyId(state, "kid-second")

        certificateProvider.first.complete(certificateInfo(keyId = "kid-first", tokenKeyId = "token-first"))
        advanceUntilIdle()

        assertHasOidcKeyId(state, "kid-second")
        assertFalse(detailValues(state).contains("kid-first"))
        scope.cancel()
    }

    private fun createHandler(
        repository: HandlerFakeRepository,
        certificateProvider: OidcCertificateInfoProvider,
        userInfoProvider: OidcUserInfoProvider,
        discoveryInfoProvider: OidcDiscoveryInfoProvider,
    ) = DashboardDetailActionHandler(
        observeCachedActivityDetail = ObserveCachedSmartSystemActivityDetailUseCase(repository),
        observeCachedBikeDetail = ObserveCachedSmartSystemBikeDetailUseCase(repository),
        getCachedActivity = GetCachedSmartSystemActivityUseCase(repository),
        getCachedActivityDetail = GetCachedSmartSystemActivityDetailUseCase(repository),
        getCachedBike = GetCachedSmartSystemBikeUseCase(repository),
        refreshActivityDetailUseCase = RefreshSmartSystemActivityDetailUseCase(repository, repository, TestAuthRepository()),
        refreshBikeDetailUseCase = RefreshSmartSystemBikeDetailUseCase(repository, repository, TestAuthRepository()),
        oidcCertificateInfoProvider = certificateProvider,
        oidcUserInfoProvider = userInfoProvider,
        oidcDiscoveryInfoProvider = discoveryInfoProvider,
        uiModelMapper = DashboardUiModelMapper(TestDashboardStringResolver()),
        stringResolver = TestDashboardStringResolver(),
    )

    private fun assertHasOidcKeyId(state: DashboardUiState, expectedKeyId: String) {
        val detail = state.selectedBikeDetail
        assertNotNull(detail)
        assertTrue("detail values: ${detailValues(state)}", detailValues(state).contains(expectedKeyId))
    }

    private fun detailValues(state: DashboardUiState): List<String> =
        state.selectedBikeDetail!!.sections
            .flatMap { section -> section.rows.map { it.second } }

    private fun bike(id: String) = BoschBike(
        id = id,
        createdAt = null,
        language = "de",
        driveUnit = null,
        remoteControl = null,
        headUnit = null,
        batteries = emptyList(),
    )

    private fun certificateInfo(
        keyId: String,
        tokenKeyId: String,
    ) = OidcCertificateInfoUiModel(
        tokenKeyId = tokenKeyId,
        keyId = keyId,
        matchesCurrentToken = true,
        keyType = null,
        algorithm = null,
        usage = null,
        subject = null,
        issuer = null,
        validFrom = null,
        validUntil = null,
        sha1Thumbprint = null,
        sha256Thumbprint = null,
        certificateChainEntries = 1,
    )
}

private class TestDashboardStringResolver : DashboardStringResolver {
    override fun get(resId: Int, args: Array<out Any>): String =
        buildString {
            append("res-")
            append(resId)
            if (args.isNotEmpty()) {
                append(':')
                append(args.joinToString(","))
            }
        }
}

private class SequencedCertificateInfoProvider(
    val first: CompletableDeferred<OidcCertificateInfoUiModel?>,
    private val second: OidcCertificateInfoUiModel?,
) : OidcCertificateInfoProvider {
    var callCount: Int = 0
        private set

    override suspend fun loadCurrentCertificate(): OidcCertificateInfoUiModel? {
        callCount += 1
        return if (callCount == 1) first.await() else second
    }
}

private class ImmediateUserInfoProvider : OidcUserInfoProvider {
    override suspend fun loadCurrentUserInfo(): OidcUserInfoUiModel? =
        null
}

private class ImmediateDiscoveryInfoProvider : OidcDiscoveryInfoProvider {
    override suspend fun loadCurrentDiscovery(): OidcDiscoveryInfoUiModel? =
        null
}

private class TestAuthRepository : AuthRepository {
    override fun getAccessToken(): String? = "token"
    override suspend fun getValidAccessToken(): Result<String> = Result.success("token")
    override fun isAuthenticated(): Boolean = true
    override fun clearTokens() = Unit
}

private class HandlerFakeRepository :
    BoschSmartSystemRepository,
    BoschSmartSystemCacheStatusRepository {
    private val bikeFlows = mutableMapOf<String, MutableStateFlow<BoschBike?>>()

    fun putBike(bike: BoschBike) {
        bikeFlows.getOrPut(bike.id) { MutableStateFlow(null) }.value = bike
    }

    override fun observeCachedActivities(): Flow<List<BoschActivity>> = emptyFlow()
    override fun observeCachedBikes(): Flow<List<BoschBike>> = emptyFlow()
    override fun observeCachedActivityDetail(activityId: String): Flow<BoschActivityDetail?> = emptyFlow()
    override fun observeCachedBike(bikeId: String): Flow<BoschBike?> =
        bikeFlows.getOrPut(bikeId) { MutableStateFlow(null) }.asStateFlow()

    override suspend fun getCachedActivities(): List<BoschActivity> = emptyList()
    override suspend fun getCachedActivity(activityId: String): BoschActivity? = null
    override suspend fun getCachedActivityDetail(activityId: String): BoschActivityDetail? = null
    override suspend fun getCachedBike(bikeId: String): BoschBike? = bikeFlows[bikeId]?.value
    override suspend fun getActivities(accessToken: String, limit: Int, offset: Int): Result<BoschActivityPage> =
        Result.failure(IllegalStateException("not used"))
    override suspend fun getActivityDetail(accessToken: String, activityId: String): Result<BoschActivityDetail> =
        Result.failure(IllegalStateException("not used"))
    override suspend fun getBikes(accessToken: String): Result<List<BoschBike>> =
        Result.failure(IllegalStateException("not used"))

    override suspend fun getBikeDetail(accessToken: String, bikeId: String): Result<BoschBike> =
        getCachedBike(bikeId)?.let(Result.Companion::success)
            ?: Result.failure(IllegalStateException("missing bike"))

    override fun observeCachedActivityDetailCacheOverview(): Flow<ActivityDetailCacheOverview> = emptyFlow()
    override suspend fun getCachedActivityTotalCount(): Int? = 0
    override suspend fun hasFreshActivities(maxAgeMillis: Long): Boolean = true
    override suspend fun hasFreshActivityDetail(activityId: String, maxAgeMillis: Long): Boolean = true
    override suspend fun hasFreshBikes(maxAgeMillis: Long): Boolean = true
    override suspend fun hasFreshBikeDetail(bikeId: String, maxAgeMillis: Long): Boolean = false
    override suspend fun getActivityIdsNeedingDetailSync(
        detailMode: CloudSyncDetailMode,
        staleThresholdEpochMillis: Long,
    ): List<String> = emptyList()
}
