package info.meuse24.m24bikestats.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import info.meuse24.m24bikestats.data.local.dao.ActivityDetailDao
import info.meuse24.m24bikestats.data.local.dao.ActivityDao
import info.meuse24.m24bikestats.data.local.entity.ActivityDetailEntity
import info.meuse24.m24bikestats.data.local.entity.ActivityDetailPointEntity
import info.meuse24.m24bikestats.data.local.entity.ActivityEntity

@Database(
    entities = [ActivityEntity::class, ActivityDetailEntity::class, ActivityDetailPointEntity::class],
    version = 3,
    exportSchema = true,
)
abstract class BoschDatabase : RoomDatabase() {
    abstract fun activityDao(): ActivityDao
    abstract fun activityDetailDao(): ActivityDetailDao
}
