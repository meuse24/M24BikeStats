package info.meuse24.m24bikestats.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import info.meuse24.m24bikestats.data.local.entity.BikeAssistModeEntity
import info.meuse24.m24bikestats.data.local.entity.BikeBatteryEntity
import info.meuse24.m24bikestats.data.local.entity.BikeCacheStateEntity
import info.meuse24.m24bikestats.data.local.entity.BikeEntity
import info.meuse24.m24bikestats.data.local.model.CachedBike
import kotlinx.coroutines.flow.Flow

@Dao
interface BikeDao {
    @Transaction
    @Query("SELECT * FROM bikes ORDER BY createdAt DESC, id ASC")
    fun observeAll(): Flow<List<CachedBike>>

    @Transaction
    @Query("SELECT * FROM bikes WHERE id = :bikeId LIMIT 1")
    suspend fun getById(bikeId: String): CachedBike?

    @Transaction
    @Query("SELECT * FROM bikes WHERE id = :bikeId LIMIT 1")
    fun observeById(bikeId: String): Flow<CachedBike?>

    @Query("SELECT updatedAtEpochMillis FROM bikes WHERE id = :bikeId LIMIT 1")
    suspend fun getUpdatedAtEpochMillis(bikeId: String): Long?

    @Query("SELECT updatedAtEpochMillis FROM bike_cache_state WHERE id = 0 LIMIT 1")
    suspend fun getCacheUpdatedAtEpochMillis(): Long?

    @Query("DELETE FROM bikes")
    suspend fun clearAll()

    @Query("DELETE FROM bike_cache_state")
    suspend fun clearCacheState()

    @Query("DELETE FROM bike_batteries WHERE bikeId = :bikeId")
    suspend fun deleteBatteriesByBikeId(bikeId: String)

    @Query("DELETE FROM bike_assist_modes WHERE bikeId = :bikeId")
    suspend fun deleteAssistModesByBikeId(bikeId: String)

    @Query("DELETE FROM bikes WHERE id = :bikeId")
    suspend fun deleteBikeById(bikeId: String)

    @Upsert
    suspend fun upsertBike(bike: BikeEntity)

    @Upsert
    suspend fun upsertBikes(bikes: List<BikeEntity>)

    @Upsert
    suspend fun upsertCacheState(cacheState: BikeCacheStateEntity)

    @Upsert
    suspend fun upsertBatteries(batteries: List<BikeBatteryEntity>)

    @Upsert
    suspend fun upsertAssistModes(assistModes: List<BikeAssistModeEntity>)

    @Transaction
    suspend fun replaceAll(
        bikes: List<BikeEntity>,
        batteries: List<BikeBatteryEntity>,
        assistModes: List<BikeAssistModeEntity>,
        cacheState: BikeCacheStateEntity,
    ) {
        clearAll()
        clearCacheState()
        if (bikes.isNotEmpty()) upsertBikes(bikes)
        if (batteries.isNotEmpty()) upsertBatteries(batteries)
        if (assistModes.isNotEmpty()) upsertAssistModes(assistModes)
        upsertCacheState(cacheState)
    }

    @Transaction
    suspend fun replaceBike(
        bike: BikeEntity,
        batteries: List<BikeBatteryEntity>,
        assistModes: List<BikeAssistModeEntity>,
    ) {
        deleteBikeById(bike.id)
        upsertBike(bike)
        if (batteries.isNotEmpty()) upsertBatteries(batteries)
        if (assistModes.isNotEmpty()) upsertAssistModes(assistModes)
    }
}
