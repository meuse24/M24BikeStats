package info.meuse24.m24bikestats.data.local.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BoschDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        BoschDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrate2To6_preservesActivitiesAndAddsNewTables() {
        helper.createDatabase(TEST_DB, 2).apply {
            execSQL(
                """
                INSERT INTO activities (
                    id, title, startTime, startTimeEpoch, endTime, timeZone,
                    durationWithoutStopsSeconds, bikeId, startOdometerMeters, distanceMeters,
                    averageSpeedKmh, maxSpeedKmh, averageCadenceRpm, maxCadenceRpm,
                    averageRiderPowerWatts, maxRiderPowerWatts, elevationGainMeters,
                    elevationLossMeters, caloriesBurned
                ) VALUES (
                    'activity-1', 'Ride', '2026-04-03T10:00:00Z', 1712138400000, NULL, NULL,
                    1200, 'bike-1', NULL, 1234,
                    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL
                )
                """.trimIndent()
            )
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DB,
            6,
            true,
            *BoschDatabaseMigrations.ALL,
        ).apply {
            query("SELECT COUNT(*) FROM activities").use { cursor ->
                cursor.moveToFirst()
                assertEquals(1, cursor.getInt(0))
            }
            query("SELECT COUNT(*) FROM bike_cache_state").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
            query("SELECT updatedAtEpochMillis FROM bikes").use { cursor ->
                assertEquals(0, cursor.count)
            }
            close()
        }
    }

    @Test
    fun migrate5To6_addsBikeCacheStateAndUpdatedAtEpochMillis() {
        helper.createDatabase(TEST_DB, 5).apply {
            execSQL(
                """
                INSERT INTO bikes (
                    id, createdAt, language, driveUnitSerialNumber, driveUnitPartNumber,
                    driveUnitProductName, driveUnitOdometerMeters,
                    driveUnitRearWheelCircumferenceMillimeters,
                    driveUnitMaximumAssistanceSpeedKmh, driveUnitWalkAssistEnabled,
                    driveUnitWalkAssistMaximumSpeedKmh, driveUnitTotalPowerOnHours,
                    driveUnitSupportPowerOnHours, remoteControlSerialNumber,
                    remoteControlPartNumber, remoteControlProductName,
                    headUnitSerialNumber, headUnitPartNumber, headUnitProductName
                ) VALUES (
                    'bike-1', '2026-04-03T10:00:00Z', 'de', 'du-1', NULL,
                    'CX', 12345.0, NULL, 25.0, 1,
                    NULL, 10, 8, NULL,
                    NULL, NULL,
                    NULL, NULL, 'Kiox 300'
                )
                """.trimIndent()
            )
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DB,
            6,
            true,
            BoschDatabaseMigrations.MIGRATION_5_6,
        ).apply {
            query("SELECT updatedAtEpochMillis FROM bikes WHERE id = 'bike-1'").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0L, cursor.getLong(0))
            }
            query("SELECT COUNT(*) FROM bike_cache_state").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
            close()
        }
    }

    private companion object {
        const val TEST_DB = "bosch-migration-test"
    }
}
