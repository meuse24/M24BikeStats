package info.meuse24.m24bikestats.presentation.apitest

import info.meuse24.m24bikestats.domain.model.BoschEndpoint
import org.junit.Assert.assertEquals
import org.junit.Test

class ApiTestSharingTest {

    @Test
    fun `api test share file name uses batch name for run all report`() {
        val fileName = apiTestShareFileName(
            endpoint = BoschEndpoint.SMART_ACTIVITIES,
            content = "=== Bosch Endpoint Batch Test ===\nbody",
        )

        assertEquals("bosch-api-test-run-all.txt", fileName)
    }

    @Test
    fun `api test share file name uses endpoint name for single endpoint`() {
        val fileName = apiTestShareFileName(
            endpoint = BoschEndpoint.SMART_ACTIVITIES,
            content = "{\"activitySummaries\":[]}",
        )

        assertEquals("bosch-api-test-smart_activities.txt", fileName)
    }
}
