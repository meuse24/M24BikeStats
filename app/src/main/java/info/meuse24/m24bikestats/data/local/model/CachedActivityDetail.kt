package info.meuse24.m24bikestats.data.local.model

import androidx.room.Embedded
import androidx.room.Relation
import info.meuse24.m24bikestats.data.local.entity.ActivityDetailEntity
import info.meuse24.m24bikestats.data.local.entity.ActivityDetailPointEntity

data class CachedActivityDetail(
    @Embedded val detail: ActivityDetailEntity,
    @Relation(
        parentColumn = "activityId",
        entityColumn = "activityId",
    )
    val points: List<ActivityDetailPointEntity>,
)
