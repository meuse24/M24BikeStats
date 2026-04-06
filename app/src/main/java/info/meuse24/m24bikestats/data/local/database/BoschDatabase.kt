package info.meuse24.m24bikestats.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import info.meuse24.m24bikestats.data.local.dao.BikeDao
import info.meuse24.m24bikestats.data.local.dao.ActivityDetailDao
import info.meuse24.m24bikestats.data.local.dao.ActivityDao
import info.meuse24.m24bikestats.data.local.entity.ActivityCacheStateEntity
import info.meuse24.m24bikestats.data.local.entity.ActivityDetailEntity
import info.meuse24.m24bikestats.data.local.entity.ActivityDetailPointEntity
import info.meuse24.m24bikestats.data.local.entity.ActivityEntity
import info.meuse24.m24bikestats.data.local.entity.BikeAssistModeEntity
import info.meuse24.m24bikestats.data.local.entity.BikeBatteryEntity
import info.meuse24.m24bikestats.data.local.entity.BikeCacheStateEntity
import info.meuse24.m24bikestats.data.local.entity.BikeEntity

@Database(
    entities = [
        ActivityEntity::class,
        ActivityCacheStateEntity::class,
        ActivityDetailEntity::class,
        ActivityDetailPointEntity::class,
        BikeEntity::class,
        BikeCacheStateEntity::class,
        BikeBatteryEntity::class,
        BikeAssistModeEntity::class,
    ],
    version = 7,
    exportSchema = true,
)
abstract class BoschDatabase : RoomDatabase() {
    abstract fun activityDao(): ActivityDao
    abstract fun activityDetailDao(): ActivityDetailDao
    abstract fun bikeDao(): BikeDao
}
