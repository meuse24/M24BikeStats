package info.meuse24.m24bikestats.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import info.meuse24.m24bikestats.data.local.database.BoschDatabase
import info.meuse24.m24bikestats.data.remote.BoschApiDataSource
import info.meuse24.m24bikestats.domain.model.BoschApiRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BoschSmartSystemRepositoryImplTest {

    private lateinit var database: BoschDatabase
    private lateinit var repository: BoschSmartSystemRepositoryImpl
    private lateinit var api: FakeBoschApiDataSource

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            BoschDatabase::class.java,
        ).allowMainThreadQueries().build()
        api = FakeBoschApiDataSource()
        repository = BoschSmartSystemRepositoryImpl(
            apiClient = api,
            activityDao = database.activityDao(),
            activityDetailDao = database.activityDetailDao(),
            bikeDao = database.bikeDao(),
            nowMillis = { 10_000L },
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun getActivities_persistsPageAndExposesCachedFlow() = runBlocking {
        api.activitiesResponse = """
            {"pagination":{"total":2,"offset":0,"limit":20},"activitySummaries":[
              {"id":"a1","title":"Morgenrunde","startTime":"2026-04-03T10:00:00Z","durationWithoutStops":1200,"distance":1234},
              {"id":"a2","title":"Abendrunde","startTime":"2026-04-02T18:00:00Z","durationWithoutStops":1800,"distance":2345}
            ]}
        """.trimIndent()

        val page = repository.getActivities(accessToken = "token", limit = 20, offset = 0).getOrThrow()
        val cached = repository.observeCachedActivities().first()

        assertEquals(2, page.items.size)
        assertEquals(2, cached.size)
        assertEquals("a1", cached.first().id)
        assertEquals(2, repository.getCachedActivityTotalCount())
    }

    @Test
    fun getActivityDetail_persistsDetailAndPoints() = runBlocking {
        api.activitiesResponse = """
            {"pagination":{"total":1,"offset":0,"limit":20},"activitySummaries":[
              {"id":"a1","title":"Morgenrunde","startTime":"2026-04-03T10:00:00Z","durationWithoutStops":1200,"distance":1234}
            ]}
        """.trimIndent()
        repository.getActivities(accessToken = "token", limit = 20, offset = 0).getOrThrow()

        api.activityDetailResponse = """
            {"activityDetails":[
              {"distance":0.0,"altitude":500.0,"speed":20.0,"cadence":80.0,"latitude":47.1,"longitude":9.1,"riderPower":210.0},
              {"distance":100.0,"altitude":505.0,"speed":22.0,"cadence":82.0,"latitude":47.2,"longitude":9.2,"riderPower":220.0}
            ]}
        """.trimIndent()

        val detail = repository.getActivityDetail(accessToken = "token", activityId = "a1").getOrThrow()
        val cached = repository.observeCachedActivityDetail("a1").first()

        assertEquals(2, detail.points.size)
        assertNotNull(cached)
        assertEquals(2, cached!!.points.size)
        assertEquals(47.1, cached.points.first().latitude)
    }

    @Test
    fun getBikes_persistsBikeAndRelations() = runBlocking {
        api.bikesResponse = """
            {"bikes":[
              {
                "id":"bike-1",
                "createdAt":"2026-04-03T10:00:00Z",
                "language":"de",
                "driveUnit":{
                  "serialNumber":"du-1",
                  "productName":"Performance Line CX",
                  "odometer":12345.0,
                  "maximumAssistanceSpeed":25.0,
                  "activeAssistModes":[{"name":"Tour+","reachableRange":75.0}]
                },
                "batteries":[{"serialNumber":"bat-1","productName":"PowerTube","deliveredWhOverLifetime":20000,"chargeCycles":{"total":50.0,"onBike":40.0,"offBike":10.0}}],
                "headUnit":{"productName":"Kiox 300"}
              }
            ]}
        """.trimIndent()

        val bikes = repository.getBikes(accessToken = "token").getOrThrow()
        val cached = repository.observeCachedBikes().first()

        assertEquals(1, bikes.size)
        assertEquals(1, cached.size)
        assertEquals("bike-1", cached.first().id)
        assertEquals("PowerTube", cached.first().batteries.first().productName)
        assertEquals("Tour+", cached.first().driveUnit?.activeAssistModes?.first()?.name)
    }

    @Test
    fun getBikeDetail_persistsSingleBikeResponse() = runBlocking {
        api.bikeDetailResponse = """
            {
              "id":"bike-1",
              "createdAt":"2026-04-03T10:00:00Z",
              "language":"de",
              "driveUnit":{
                "serialNumber":"du-1",
                "productName":"Performance Line CX",
                "odometer":12345.0,
                "maximumAssistanceSpeed":25.0,
                "activeAssistModes":[{"name":"Tour+","reachableRange":75.0}]
              },
              "batteries":[{"serialNumber":"bat-1","partNumber":"bat-1-pn","productName":"PowerTube","deliveredWhOverLifetime":20000,"chargeCycles":{"total":50.0,"onBike":40.0,"offBike":10.0}}],
              "headUnit":{"productName":"Kiox 300"}
            }
        """.trimIndent()
        api.bikePassResponse = """
            {
              "bikePasses":[
                {
                  "bikeId":"bike-1",
                  "frameNumber":"FRAME-123",
                  "frameNumberPosition":"bottom bracket",
                  "description":"orange fork",
                  "createdAt":"2026-04-01T10:00:00Z",
                  "updatedAt":"2026-04-02T10:00:00Z"
                }
              ],
              "theftReportLogs":[
                {
                  "theftReportLogId":"log-1",
                  "bikeId":"bike-1",
                  "createdAt":"2026-04-03T10:00:00Z",
                  "theftCaseEnteredAt":"2026-04-03T11:00:00Z",
                  "description":"reported"
                }
              ]
            }
        """.trimIndent()
        api.serviceBookResponse = """
            {
              "serviceRecords":[
                {
                  "id":"service-1",
                  "type":"DIGITAL_SERVICE",
                  "attributes":{
                    "bikeId":"bike-1",
                    "createdAt":"2026-04-04T10:00:00Z",
                    "odometerValue":150000,
                    "bikeDealer":{"name":"Dealer One","city":"Vienna"},
                    "details":{
                      "toolVersion":"5.4.0",
                      "batteryMeasurement":{
                        "measurement":{
                          "fullChargeCycles":52,
                          "measuredEnergyCapacity":710,
                          "nominalEnergyCapacity":750,
                          "measuredCapacityPercentage":95
                        }
                      },
                      "softwareUpdate":{
                        "client":{"type":"DIAGNOSTIC_TOOL","version":"2026.4"},
                        "isForcedUpdate":true,
                        "bike":{"updatedComponents":[{"productName":"Drive Unit"},{"productName":"ABS"}]}
                      }
                    }
                  }
                }
              ]
            }
        """.trimIndent()
        api.registrationsResponse = """
            {
              "registrations":[
                {
                  "bikeId":"bike-1",
                  "registrationType":"BIKE_REGISTRATION",
                  "createdAt":"2026-04-05T10:00:00Z"
                },
                {
                  "bikeId":"other-bike",
                  "registrationType":"COMPONENT_REGISTRATION",
                  "createdAt":"2026-04-05T10:00:00Z",
                  "componentType":"BATTERY",
                  "partNumber":"bat-1-pn",
                  "serialNumber":"bat-1"
                }
              ]
            }
        """.trimIndent()

        val bike = repository.getBikeDetail(accessToken = "token", bikeId = "bike-1").getOrThrow()
        val cached = repository.observeCachedBike("bike-1").first()

        assertEquals("bike-1", bike.id)
        assertEquals("FRAME-123", bike.bikePass!!.frameNumber)
        assertEquals(1, bike.theftReportLogs.size)
        assertEquals(1, bike.serviceRecords.size)
        assertEquals(2, bike.registrations.size)
        assertNotNull(cached)
        assertEquals("Performance Line CX", cached!!.driveUnit?.productName)
        assertEquals("PowerTube", cached.batteries.first().productName)
        assertEquals("FRAME-123", cached.bikePass!!.frameNumber)
        assertEquals("Dealer One", cached.serviceRecords.first().bikeDealerName)
        assertEquals(2, cached.registrations.size)
    }

    @Test
    fun getBikeDetail_keepsCachedBikePassAndServiceRecordsWhenSupplementaryCallsFail() = runBlocking {
        api.bikeDetailResponse = """
            {
              "id":"bike-1",
              "createdAt":"2026-04-03T10:00:00Z",
              "language":"de",
              "driveUnit":{"productName":"Performance Line CX"},
              "batteries":[{"serialNumber":"bat-1","partNumber":"bat-1-pn","productName":"PowerTube"}]
            }
        """.trimIndent()
        api.bikePassResponse = """
            {
              "bikePasses":[
                {
                  "bikeId":"bike-1",
                  "frameNumber":"FRAME-123",
                  "createdAt":"2026-04-01T10:00:00Z"
                }
              ],
              "theftReportLogs":[
                {
                  "theftReportLogId":"log-1",
                  "bikeId":"bike-1",
                  "createdAt":"2026-04-03T10:00:00Z",
                  "description":"reported"
                }
              ]
            }
        """.trimIndent()
        api.serviceBookResponse = """
            {
              "serviceRecords":[
                {
                  "id":"service-1",
                  "type":"DIGITAL_SERVICE",
                  "attributes":{
                    "bikeId":"bike-1",
                    "createdAt":"2026-04-04T10:00:00Z",
                    "details":{"version":1}
                  }
                }
              ]
            }
        """.trimIndent()
        api.registrationsResponse = """
            {
              "registrations":[
                {
                  "bikeId":"bike-1",
                  "registrationType":"COMPONENT_REGISTRATION",
                  "createdAt":"2026-04-05T10:00:00Z",
                  "componentType":"BATTERY",
                  "partNumber":"bat-1-pn",
                  "serialNumber":"bat-1"
                }
              ]
            }
        """.trimIndent()

        repository.getBikeDetail(accessToken = "token", bikeId = "bike-1").getOrThrow()

        api.failBikePassRequest = true
        api.failServiceBookRequest = true
        api.failRegistrationsRequest = true
        api.bikeDetailResponse = """
            {
              "id":"bike-1",
              "createdAt":"2026-04-03T10:00:00Z",
              "language":"de",
              "driveUnit":{"productName":"Updated Drive Unit"},
              "batteries":[{"serialNumber":"bat-1","partNumber":"bat-1-pn","productName":"PowerTube"}]
            }
        """.trimIndent()

        val bike = repository.getBikeDetail(accessToken = "token", bikeId = "bike-1").getOrThrow()
        val cached = repository.observeCachedBike("bike-1").first()

        assertEquals("Updated Drive Unit", bike.driveUnit!!.productName)
        assertEquals("FRAME-123", bike.bikePass!!.frameNumber)
        assertEquals(1, bike.theftReportLogs.size)
        assertEquals(1, bike.serviceRecords.size)
        assertEquals(1, bike.registrations.size)
        assertNotNull(cached)
        assertEquals("FRAME-123", cached!!.bikePass!!.frameNumber)
        assertEquals(1, cached.theftReportLogs.size)
        assertEquals(1, cached.serviceRecords.size)
        assertEquals(1, cached.registrations.size)
    }

    @Test
    fun getBikeDetail_clearsCachedSupplementaryDataWhenSupplementaryCallsSucceedWithEmptyPayload() = runBlocking {
        api.bikeDetailResponse = """
            {
              "id":"bike-1",
              "createdAt":"2026-04-03T10:00:00Z",
              "language":"de",
              "driveUnit":{"productName":"Performance Line CX"}
            }
        """.trimIndent()
        api.bikePassResponse = """
            {
              "bikePasses":[{"bikeId":"bike-1","frameNumber":"FRAME-123","createdAt":"2026-04-01T10:00:00Z"}],
              "theftReportLogs":[{"theftReportLogId":"log-1","bikeId":"bike-1","createdAt":"2026-04-03T10:00:00Z"}]
            }
        """.trimIndent()
        api.serviceBookResponse = """
            {
              "serviceRecords":[
                {
                  "id":"service-1",
                  "type":"DIGITAL_SERVICE",
                  "attributes":{"bikeId":"bike-1","createdAt":"2026-04-04T10:00:00Z","details":{"version":1}}
                }
              ]
            }
        """.trimIndent()
        api.registrationsResponse = """
            {
              "registrations":[
                {
                  "bikeId":"bike-1",
                  "registrationType":"COMPONENT_REGISTRATION",
                  "createdAt":"2026-04-05T10:00:00Z",
                  "componentType":"BATTERY",
                  "partNumber":"bat-1-pn",
                  "serialNumber":"bat-1"
                }
              ]
            }
        """.trimIndent()

        repository.getBikeDetail(accessToken = "token", bikeId = "bike-1").getOrThrow()

        api.bikePassResponse = """{"bikePasses":[],"theftReportLogs":[]}"""
        api.serviceBookResponse = """{"serviceRecords":[]}"""
        api.registrationsResponse = """{"registrations":[]}"""

        val bike = repository.getBikeDetail(accessToken = "token", bikeId = "bike-1").getOrThrow()
        val cached = repository.observeCachedBike("bike-1").first()

        assertNull(bike.bikePass)
        assertEquals(0, bike.theftReportLogs.size)
        assertEquals(0, bike.serviceRecords.size)
        assertEquals(0, bike.registrations.size)
        assertNotNull(cached)
        assertNull(cached!!.bikePass)
        assertEquals(0, cached.theftReportLogs.size)
        assertEquals(0, cached.serviceRecords.size)
        assertEquals(0, cached.registrations.size)
    }
}

private class FakeBoschApiDataSource : BoschApiDataSource {
    var activitiesResponse: String = """{"pagination":{"total":0,"offset":0,"limit":20},"activitySummaries":[]}"""
    var activityDetailResponse: String = """{"activityDetails":[]}"""
    var bikesResponse: String = """{"bikes":[]}"""
    var bikeDetailResponse: String = """{"id":"bike-1"}"""
    var bikePassResponse: String = """{"bikePasses":[],"theftReportLogs":[]}"""
    var serviceBookResponse: String = """{"serviceRecords":[]}"""
    var registrationsResponse: String = """{"registrations":[]}"""
    var failBikePassRequest: Boolean = false
    var failServiceBookRequest: Boolean = false
    var failRegistrationsRequest: Boolean = false

    override suspend fun get(request: BoschApiRequest, accessToken: String): String {
        val body = when {
            request.path.startsWith("/activity/smart-system/v1/activities?") -> activitiesResponse
            request.path.contains("/activity/smart-system/v1/activities/") -> activityDetailResponse
            request.path == "/bike-profile/smart-system/v1/bikes" -> bikesResponse
            request.path.startsWith("/bike-profile/smart-system/v1/bikes/") -> bikeDetailResponse
            request.path.startsWith("/bike-pass/smart-system/v1/bike-passes?bikeId=") -> {
                if (failBikePassRequest) error("Bike pass request failed")
                bikePassResponse
            }
            request.path.startsWith("/service-book/smart-system/v1/service-records?bikeId=") -> {
                if (failServiceBookRequest) error("Service book request failed")
                serviceBookResponse
            }
            request.path == "/bike-registration/smart-system/v1/registrations" -> {
                if (failRegistrationsRequest) error("Registrations request failed")
                registrationsResponse
            }
            else -> error("Unhandled request path: ${request.path}")
        }
        return "HTTP 200 OK\n\n$body"
    }
}
