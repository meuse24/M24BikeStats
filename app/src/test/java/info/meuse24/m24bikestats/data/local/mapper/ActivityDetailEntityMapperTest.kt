package info.meuse24.m24bikestats.data.local.mapper

import info.meuse24.m24bikestats.data.local.entity.ActivityDetailEntity
import info.meuse24.m24bikestats.data.local.entity.ActivityDetailPointEntity
import info.meuse24.m24bikestats.data.local.model.CachedActivityDetail
import info.meuse24.m24bikestats.domain.model.BoschActivityDetail
import info.meuse24.m24bikestats.domain.model.BoschActivityDetailPoint
import org.junit.Assert.assertEquals
import org.junit.Test

class ActivityDetailEntityMapperTest {

    @Test
    fun `detail maps to entity with gps count`() {
        val detail = BoschActivityDetail(
            activityId = "activity-1",
            points = listOf(
                BoschActivityDetailPoint(0.0, 500.0, 20.0, 80.0, 47.0, 9.0, 210.0),
                BoschActivityDetailPoint(50.0, 505.0, 21.0, 82.0, 0.0, 0.0, 220.0),
                BoschActivityDetailPoint(100.0, 510.0, 22.0, 84.0, 47.1, 9.1, 230.0),
            ),
        )

        val entity = detail.toEntity(updatedAtEpochMillis = 1234L)
        val points = detail.toPointEntities()

        assertEquals(3, entity.pointCount)
        assertEquals(2, entity.gpsPointCount)
        assertEquals(3, points.size)
        assertEquals(0, points.first().pointIndex)
        assertEquals(100.0, points.last().distanceMeters)
    }

    @Test
    fun `cached detail maps back sorted by point index`() {
        val cached = CachedActivityDetail(
            detail = ActivityDetailEntity(
                activityId = "activity-1",
                pointCount = 2,
                gpsPointCount = 2,
                updatedAtEpochMillis = 1234L,
            ),
            points = listOf(
                ActivityDetailPointEntity("activity-1", 1, 100.0, 510.0, 22.0, 84.0, 47.1, 9.1, 230.0),
                ActivityDetailPointEntity("activity-1", 0, 0.0, 500.0, 20.0, 80.0, 47.0, 9.0, 210.0),
            ),
        )

        val domain = cached.toDomain()

        assertEquals("activity-1", domain.activityId)
        assertEquals(2, domain.points.size)
        assertEquals(0.0, domain.points.first().distanceMeters)
        assertEquals(100.0, domain.points.last().distanceMeters)
    }
}
