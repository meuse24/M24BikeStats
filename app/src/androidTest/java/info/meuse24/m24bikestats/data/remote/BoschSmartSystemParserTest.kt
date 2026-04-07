package info.meuse24.m24bikestats.data.remote

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BoschSmartSystemParserTest {

    private val parser = BoschSmartSystemParser()
    private val bodyExtractor = BoschJsonBodyExtractor()

    @Test
    fun extractorReturnsJsonPayloadAfterHttpHeaderBlock() {
        val payload = bodyExtractor.extract("HTTP 200 OK\n\n{\"bikes\":[]}")

        assertEquals("{\"bikes\":[]}", payload)
    }

    @Test
    fun extractorReturnsNullForNonJsonContent() {
        val payload = bodyExtractor.extract("HTTP 500 FAIL\n\n<html>error</html>")

        assertNull(payload)
    }

    @Test
    fun parserReadsActivitiesPageIncludingPagination() {
        val page = parser.parseActivitiesPage(
            json = """
                {"pagination":{"total":2,"offset":20,"limit":20},"activitySummaries":[
                  {"id":"a1","title":"Morgenrunde","startTime":"2026-04-03T10:00:00Z","durationWithoutStops":1200,"distance":1234},
                  {"id":"a2","title":"Abendrunde","startTime":"2026-04-02T18:00:00Z","durationWithoutStops":1800,"distance":2345}
                ]}
            """.trimIndent(),
            limit = 20,
            offset = 20,
        )

        assertEquals(2, page.items.size)
        assertEquals(2, page.total)
        assertEquals(20, page.offset)
        assertEquals("a1", page.items.first().id)
    }

    @Test
    fun parserReadsActivityDetailPoints() {
        val detail = parser.parseActivityDetail(
            activityId = "a1",
            json = """
                {"activityDetails":[
                  {"distance":0.0,"altitude":500.0,"speed":20.0,"cadence":80.0,"latitude":47.1,"longitude":9.1,"riderPower":210.0}
                ]}
            """.trimIndent(),
        )

        assertEquals("a1", detail.activityId)
        assertEquals(1, detail.points.size)
        assertEquals(47.1, detail.points.first().latitude)
    }

    @Test
    fun parserReadsBikeWithAssistModesAndBatteryData() {
        val bikes = parser.parseBikes(
            """
                {"bikes":[
                  {
                    "id":"bike-1",
                    "createdAt":"2026-04-03T10:00:00Z",
                    "language":"de",
                    "oemId":"OEM-1",
                    "serviceDue":{"date":"2026-06-01T10:00:00Z","odometer":200000},
                    "driveUnit":{
                      "serialNumber":"du-1",
                      "productName":"Performance Line CX",
                      "odometer":12345.0,
                      "maximumAssistanceSpeed":25.0,
                      "activeAssistModes":[{"name":"Tour+","reachableRange":75.0}]
                    },
                    "connectModule":{"productName":"ConnectModule"},
                    "antiLockBrakeSystems":[{"productName":"eBike ABS"}],
                    "batteries":[{"serialNumber":"bat-1","productName":"PowerTube","deliveredWhOverLifetime":20000,"chargeCycles":{"total":50.0,"onBike":40.0,"offBike":10.0}}],
                    "headUnit":{"productName":"Kiox 300"}
                  }
                ]}
            """.trimIndent()
        )

        assertEquals(1, bikes.size)
        assertEquals("bike-1", bikes.first().id)
        assertEquals("OEM-1", bikes.first().oemId)
        assertEquals("ConnectModule", bikes.first().connectModule?.productName)
        assertEquals("eBike ABS", bikes.first().antiLockBrakeSystems.first().productName)
        assertEquals("PowerTube", bikes.first().batteries.first().productName)
        assertEquals("Tour+", bikes.first().driveUnit?.activeAssistModes?.first()?.name)
    }

    @Test
    fun parserReadsBikePassAndTheftLogs() {
        val bikePassData = parser.parseBikePassData(
            json = """
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
                      "riderPortalLink":"https://example.com/theft/1",
                      "description":"reported",
                      "location":{
                        "detectedAt":"2026-04-03T12:00:00Z",
                        "latitude":47.1,
                        "longitude":9.1,
                        "horizontalAccuracy":15,
                        "address":"Test Street 1",
                        "description":"behind stairs"
                      }
                    }
                  ]
                }
            """.trimIndent(),
            bikeId = "bike-1",
        )

        assertEquals("FRAME-123", bikePassData.bikePass!!.frameNumber)
        assertEquals(1, bikePassData.theftReportLogs.size)
        assertEquals("https://example.com/theft/1", bikePassData.theftReportLogs.first().riderPortalLink)
        assertEquals("Test Street 1", bikePassData.theftReportLogs.first().location!!.address)
    }

    @Test
    fun parserReadsServiceBookRecords() {
        val serviceRecords = parser.parseServiceRecords(
            json = """
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
                              "measuredCapacityPercentage":95,
                              "onBikeMeasurement":false
                            }
                          },
                          "softwareUpdate":{
                            "client":{"type":"DIAGNOSTIC_TOOL","version":"2026.4"},
                            "isForcedUpdate":true,
                            "bike":{
                              "updatedComponents":[
                                {"productName":"Drive Unit"},
                                {"productName":"ABS"}
                              ]
                            }
                          }
                        }
                      }
                    }
                  ]
                }
            """.trimIndent(),
            bikeId = "bike-1",
        )

        assertEquals(1, serviceRecords.size)
        assertEquals("DIGITAL_SERVICE", serviceRecords.first().type)
        assertEquals("Dealer One", serviceRecords.first().bikeDealerName)
        assertEquals(95, serviceRecords.first().batteryMeasurement!!.measuredCapacityPercentage)
        assertEquals(2, serviceRecords.first().softwareUpdate!!.updatedComponentsCount)
    }

    @Test
    fun parserReadsRegistrations() {
        val registrations = parser.parseRegistrations(
            """
                {
                  "registrations":[
                    {
                      "bikeId":"bike-1",
                      "registrationType":"BIKE_REGISTRATION",
                      "createdAt":"2026-04-05T10:00:00Z"
                    },
                    {
                      "bikeId":"bike-1",
                      "registrationType":"COMPONENT_REGISTRATION",
                      "createdAt":"2026-04-05T10:00:00Z",
                      "componentType":"BATTERY",
                      "partNumber":"bat-pn",
                      "serialNumber":"bat-1"
                    }
                  ]
                }
            """.trimIndent()
        )

        assertEquals(2, registrations.size)
        assertEquals("BIKE_REGISTRATION", registrations.first().registrationType)
        assertEquals("BATTERY", registrations.last().componentType)
        assertEquals("bat-pn", registrations.last().partNumber)
    }
}


