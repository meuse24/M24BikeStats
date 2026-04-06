package info.meuse24.m24bikestats.presentation.apitest

import info.meuse24.m24bikestats.api.BoschEndpoint
import org.junit.Assert.assertEquals
import org.junit.Test

class ApiTestRunAllRequestsTest {

    @Test
    fun `run all requests include all confirmed endpoints once`() {
        val requests = buildRunAllRequests(
            activityId = "activity-id",
            bikeId = "bike-id",
        )

        assertEquals(
            BoschEndpoint.entries.map { it.name },
            requests.map { it.debugName }
        )
    }

    @Test
    fun `additional activity requests include prev next and last links once`() {
        val requests = buildAdditionalActivityRequests(
            """
            HTTP 200 OK

            {
              "links": {
                "prev": "https://api.bosch-ebike.com/activity/smart-system/v1/activities?limit=20&offset=0",
                "self": "https://api.bosch-ebike.com/activity/smart-system/v1/activities?limit=20&offset=0",
                "next": "https://api.bosch-ebike.com/activity/smart-system/v1/activities?limit=20&offset=20",
                "last": "https://api.bosch-ebike.com/activity/smart-system/v1/activities?limit=20&offset=440"
              }
            }
            """.trimIndent()
        )

        assertEquals(
            listOf(
                "SMART_ACTIVITIES_PREV_LINK_OFFSET_0",
                "SMART_ACTIVITIES_NEXT_LINK_OFFSET_20",
                "SMART_ACTIVITIES_LAST_LINK_OFFSET_440",
            ),
            requests.map { it.debugName }
        )
        assertEquals(
            listOf(
                "https://api.bosch-ebike.com/activity/smart-system/v1/activities?limit=20&offset=0",
                "https://api.bosch-ebike.com/activity/smart-system/v1/activities?limit=20&offset=20",
                "https://api.bosch-ebike.com/activity/smart-system/v1/activities?limit=20&offset=440",
            ),
            requests.map { it.url }
        )
    }

    @Test
    fun `additional activity requests skip duplicate links`() {
        val requests = buildAdditionalActivityRequests(
            """
            HTTP 200 OK

            {
              "links": {
                "prev": "https://api.bosch-ebike.com/activity/smart-system/v1/activities?limit=20&offset=0",
                "next": "https://api.bosch-ebike.com/activity/smart-system/v1/activities?limit=20&offset=20",
                "last": "https://api.bosch-ebike.com/activity/smart-system/v1/activities?limit=20&offset=20"
              }
            }
            """.trimIndent()
        )

        assertEquals(
            listOf("SMART_ACTIVITIES_PREV_LINK_OFFSET_0", "SMART_ACTIVITIES_NEXT_LINK_OFFSET_20"),
            requests.map { it.debugName }
        )
    }
}
