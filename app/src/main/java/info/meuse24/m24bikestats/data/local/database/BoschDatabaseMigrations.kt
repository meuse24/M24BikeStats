package info.meuse24.m24bikestats.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object BoschDatabaseMigrations {

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `activity_details` (
                    `activityId` TEXT NOT NULL,
                    `pointCount` INTEGER NOT NULL,
                    `gpsPointCount` INTEGER NOT NULL,
                    `updatedAtEpochMillis` INTEGER NOT NULL,
                    PRIMARY KEY(`activityId`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `activity_detail_points` (
                    `activityId` TEXT NOT NULL,
                    `pointIndex` INTEGER NOT NULL,
                    `distanceMeters` REAL,
                    `altitudeMeters` REAL,
                    `speedKmh` REAL,
                    `cadenceRpm` REAL,
                    `latitude` REAL,
                    `longitude` REAL,
                    `riderPowerWatts` REAL,
                    PRIMARY KEY(`activityId`, `pointIndex`),
                    FOREIGN KEY(`activityId`) REFERENCES `activity_details`(`activityId`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_activity_detail_points_activityId` ON `activity_detail_points` (`activityId`)"
            )
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("PRAGMA foreign_keys=OFF")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `activity_details_new` (
                    `activityId` TEXT NOT NULL,
                    `pointCount` INTEGER NOT NULL,
                    `gpsPointCount` INTEGER NOT NULL,
                    `updatedAtEpochMillis` INTEGER NOT NULL,
                    PRIMARY KEY(`activityId`),
                    FOREIGN KEY(`activityId`) REFERENCES `activities`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO `activity_details_new` (`activityId`, `pointCount`, `gpsPointCount`, `updatedAtEpochMillis`)
                SELECT `activityId`, `pointCount`, `gpsPointCount`, `updatedAtEpochMillis`
                FROM `activity_details`
                """.trimIndent()
            )
            db.execSQL("DROP TABLE `activity_details`")
            db.execSQL("ALTER TABLE `activity_details_new` RENAME TO `activity_details`")
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_activity_details_activityId` ON `activity_details` (`activityId`)"
            )
            db.execSQL("PRAGMA foreign_keys=ON")
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `activity_cache_state` (
                    `id` INTEGER NOT NULL,
                    `totalCount` INTEGER NOT NULL,
                    `updatedAtEpochMillis` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `bikes` (
                    `id` TEXT NOT NULL,
                    `createdAt` TEXT,
                    `language` TEXT,
                    `driveUnitSerialNumber` TEXT,
                    `driveUnitPartNumber` TEXT,
                    `driveUnitProductName` TEXT,
                    `driveUnitOdometerMeters` REAL,
                    `driveUnitRearWheelCircumferenceMillimeters` REAL,
                    `driveUnitMaximumAssistanceSpeedKmh` REAL,
                    `driveUnitWalkAssistEnabled` INTEGER,
                    `driveUnitWalkAssistMaximumSpeedKmh` REAL,
                    `driveUnitTotalPowerOnHours` INTEGER,
                    `driveUnitSupportPowerOnHours` INTEGER,
                    `remoteControlSerialNumber` TEXT,
                    `remoteControlPartNumber` TEXT,
                    `remoteControlProductName` TEXT,
                    `headUnitSerialNumber` TEXT,
                    `headUnitPartNumber` TEXT,
                    `headUnitProductName` TEXT,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `bike_batteries` (
                    `bikeId` TEXT NOT NULL,
                    `batteryIndex` INTEGER NOT NULL,
                    `serialNumber` TEXT,
                    `partNumber` TEXT,
                    `productName` TEXT,
                    `deliveredWhOverLifetime` INTEGER,
                    `totalChargeCycles` REAL,
                    `onBikeChargeCycles` REAL,
                    `offBikeChargeCycles` REAL,
                    PRIMARY KEY(`bikeId`, `batteryIndex`),
                    FOREIGN KEY(`bikeId`) REFERENCES `bikes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_bike_batteries_bikeId` ON `bike_batteries` (`bikeId`)"
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `bike_assist_modes` (
                    `bikeId` TEXT NOT NULL,
                    `modeIndex` INTEGER NOT NULL,
                    `name` TEXT NOT NULL,
                    `reachableRangeKm` REAL,
                    PRIMARY KEY(`bikeId`, `modeIndex`),
                    FOREIGN KEY(`bikeId`) REFERENCES `bikes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_bike_assist_modes_bikeId` ON `bike_assist_modes` (`bikeId`)"
            )
        }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("PRAGMA foreign_keys=OFF")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `bikes_new` (
                    `id` TEXT NOT NULL,
                    `createdAt` TEXT,
                    `updatedAtEpochMillis` INTEGER NOT NULL,
                    `language` TEXT,
                    `driveUnitSerialNumber` TEXT,
                    `driveUnitPartNumber` TEXT,
                    `driveUnitProductName` TEXT,
                    `driveUnitOdometerMeters` REAL,
                    `driveUnitRearWheelCircumferenceMillimeters` REAL,
                    `driveUnitMaximumAssistanceSpeedKmh` REAL,
                    `driveUnitWalkAssistEnabled` INTEGER,
                    `driveUnitWalkAssistMaximumSpeedKmh` REAL,
                    `driveUnitTotalPowerOnHours` INTEGER,
                    `driveUnitSupportPowerOnHours` INTEGER,
                    `remoteControlSerialNumber` TEXT,
                    `remoteControlPartNumber` TEXT,
                    `remoteControlProductName` TEXT,
                    `headUnitSerialNumber` TEXT,
                    `headUnitPartNumber` TEXT,
                    `headUnitProductName` TEXT,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO `bikes_new` (
                    `id`, `createdAt`, `updatedAtEpochMillis`, `language`,
                    `driveUnitSerialNumber`, `driveUnitPartNumber`, `driveUnitProductName`,
                    `driveUnitOdometerMeters`, `driveUnitRearWheelCircumferenceMillimeters`,
                    `driveUnitMaximumAssistanceSpeedKmh`, `driveUnitWalkAssistEnabled`,
                    `driveUnitWalkAssistMaximumSpeedKmh`, `driveUnitTotalPowerOnHours`,
                    `driveUnitSupportPowerOnHours`, `remoteControlSerialNumber`,
                    `remoteControlPartNumber`, `remoteControlProductName`,
                    `headUnitSerialNumber`, `headUnitPartNumber`, `headUnitProductName`
                )
                SELECT
                    `id`, `createdAt`, 0, `language`,
                    `driveUnitSerialNumber`, `driveUnitPartNumber`, `driveUnitProductName`,
                    `driveUnitOdometerMeters`, `driveUnitRearWheelCircumferenceMillimeters`,
                    `driveUnitMaximumAssistanceSpeedKmh`, `driveUnitWalkAssistEnabled`,
                    `driveUnitWalkAssistMaximumSpeedKmh`, `driveUnitTotalPowerOnHours`,
                    `driveUnitSupportPowerOnHours`, `remoteControlSerialNumber`,
                    `remoteControlPartNumber`, `remoteControlProductName`,
                    `headUnitSerialNumber`, `headUnitPartNumber`, `headUnitProductName`
                FROM `bikes`
                """.trimIndent()
            )
            db.execSQL("DROP TABLE `bikes`")
            db.execSQL("ALTER TABLE `bikes_new` RENAME TO `bikes`")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `bike_cache_state` (
                    `id` INTEGER NOT NULL,
                    `updatedAtEpochMillis` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL("PRAGMA foreign_keys=ON")
        }
    }

    val ALL = arrayOf(
        MIGRATION_2_3,
        MIGRATION_3_4,
        MIGRATION_4_5,
        MIGRATION_5_6,
    )
}
