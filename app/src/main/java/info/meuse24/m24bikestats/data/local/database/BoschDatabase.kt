package info.meuse24.m24bikestats.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import info.meuse24.m24bikestats.data.local.dao.ActivityDao
import info.meuse24.m24bikestats.data.local.entity.ActivityEntity

@Database(
    entities = [ActivityEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class BoschDatabase : RoomDatabase() {
    abstract fun activityDao(): ActivityDao
}
