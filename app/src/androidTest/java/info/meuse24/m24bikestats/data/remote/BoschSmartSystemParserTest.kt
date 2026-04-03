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
    fun `extractor returns json payload after http header block`() {
        val payload = bodyExtractor.extract("HTTP 200 OK\n\n{\"bikes\":[]}")

        assertEquals("{\"bikes\":[]}", payload)
    }

    @Test
    fun `extractor returns null for non json content`() {
        val payload = bodyExtractor.extract("HTTP 500 FAIL\n\n<html>error</html>")

        assertNull(payload)
    }

    @Test
    fun `parser reads activities page including pagination`() {
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
    fun `parser reads activity detail points`() {
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
    fun `parser reads bike with assist modes and battery data`() {
        val bikes = parser.parseBikes(
            """
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
        )

        assertEquals(1, bikes.size)
        assertEquals("bike-1", bikes.first().id)
        assertEquals("PowerTube", bikes.first().batteries.first().productName)
        assertEquals("Tour+", bikes.first().driveUnit?.activeAssistModes?.first()?.name)
    }
}


