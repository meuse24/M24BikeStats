package info.meuse24.m24bikestats.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object BoschDatabaseMigrations {
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE bikes ADD COLUMN updatedAtEpochMillis INTEGER NOT NULL DEFAULT 0")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `bike_cache_state` (
                    `id` INTEGER NOT NULL,
                    `updatedAtEpochMillis` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
        }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE bikes ADD COLUMN oemId TEXT")
            db.execSQL("ALTER TABLE bikes ADD COLUMN serviceDueDate TEXT")
            db.execSQL("ALTER TABLE bikes ADD COLUMN serviceDueOdometerMeters REAL")
            db.execSQL("ALTER TABLE bikes ADD COLUMN connectModuleSerialNumber TEXT")
            db.execSQL("ALTER TABLE bikes ADD COLUMN connectModulePartNumber TEXT")
            db.execSQL("ALTER TABLE bikes ADD COLUMN connectModuleProductName TEXT")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `bike_abs_components` (
                    `bikeId` TEXT NOT NULL,
                    `absIndex` INTEGER NOT NULL,
                    `serialNumber` TEXT,
                    `partNumber` TEXT,
                    `productName` TEXT,
                    PRIMARY KEY(`bikeId`, `absIndex`),
                    FOREIGN KEY(`bikeId`) REFERENCES `bikes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_bike_abs_components_bikeId` ON `bike_abs_components` (`bikeId`)"
            )
        }
    }

    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `bike_passes` (
                    `bikeId` TEXT NOT NULL,
                    `frameNumber` TEXT,
                    `frameNumberPosition` TEXT,
                    `description` TEXT,
                    `createdAt` TEXT,
                    `updatedAt` TEXT,
                    PRIMARY KEY(`bikeId`),
                    FOREIGN KEY(`bikeId`) REFERENCES `bikes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_bike_passes_bikeId` ON `bike_passes` (`bikeId`)")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `bike_theft_report_logs` (
                    `bikeId` TEXT NOT NULL,
                    `theftReportLogId` TEXT NOT NULL,
                    `createdAt` TEXT,
                    `expiresAtEpochMillis` INTEGER,
                    `timeZone` TEXT,
                    `theftCaseEnteredAt` TEXT,
                    `riderPortalLink` TEXT,
                    `description` TEXT,
                    `locationDetectedAt` TEXT,
                    `locationLatitude` REAL,
                    `locationLongitude` REAL,
                    `locationHorizontalAccuracyMeters` REAL,
                    `locationAddress` TEXT,
                    `locationDescription` TEXT,
                    PRIMARY KEY(`bikeId`, `theftReportLogId`),
                    FOREIGN KEY(`bikeId`) REFERENCES `bikes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_bike_theft_report_logs_bikeId` ON `bike_theft_report_logs` (`bikeId`)"
            )
        }
    }

    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `bike_service_records` (
                    `bikeId` TEXT NOT NULL,
                    `serviceRecordId` TEXT NOT NULL,
                    `type` TEXT NOT NULL,
                    `createdAt` TEXT NOT NULL,
                    `odometerValueMeters` INTEGER,
                    `bikeDealerName` TEXT,
                    `bikeDealerCity` TEXT,
                    `toolVersion` TEXT,
                    `batteryFullChargeCycles` INTEGER,
                    `batteryMeasuredEnergyCapacityWh` INTEGER,
                    `batteryNominalEnergyCapacityWh` INTEGER,
                    `batteryMeasuredCapacityPercentage` INTEGER,
                    `batteryOnBikeMeasurement` INTEGER,
                    `softwareUpdateClientType` TEXT,
                    `softwareUpdateClientVersion` TEXT,
                    `softwareUpdateForced` INTEGER,
                    `softwareUpdateUpdatedComponentsCount` INTEGER,
                    `softwareUpdateUpdatedComponentNames` TEXT,
                    PRIMARY KEY(`bikeId`, `serviceRecordId`),
                    FOREIGN KEY(`bikeId`) REFERENCES `bikes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_bike_service_records_bikeId` ON `bike_service_records` (`bikeId`)"
            )
        }
    }

    val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `bike_registrations` (
                    `bikeId` TEXT NOT NULL,
                    `registrationKey` TEXT NOT NULL,
                    `registrationType` TEXT NOT NULL,
                    `createdAt` TEXT NOT NULL,
                    `componentType` TEXT,
                    `partNumber` TEXT,
                    `serialNumber` TEXT,
                    PRIMARY KEY(`bikeId`, `registrationKey`),
                    FOREIGN KEY(`bikeId`) REFERENCES `bikes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_bike_registrations_bikeId` ON `bike_registrations` (`bikeId`)"
            )
        }
    }

    val ALL = arrayOf(
        MIGRATION_5_6,
        MIGRATION_7_8,
        MIGRATION_8_9,
        MIGRATION_9_10,
        MIGRATION_10_11,
    )
}
