package io.nemopill.adherencetracking.infrastructure

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Test-only Room database hosting the `confirmation` slice, used by
 * [RoomConfirmationRepositoryRobolectricTest] to exercise the real [ConfirmationDao],
 * [ConfirmationConverters], and the unique-`doseId` index against an in-memory SQLite.
 *
 * The production aggregate `NemoPillDatabase` lives in `:app` (the only module that may depend on
 * the feature modules whose DAOs it aggregates — file 04 graph), so a library-module test cannot
 * reference it; this test-scoped `@Database` stands in for the `confirmation` portion. Room KSP
 * codegen for it runs in this module's test source set (`kspTest(room.compiler)`).
 */
@Database(entities = [ConfirmationEntity::class], version = 1, exportSchema = false)
@TypeConverters(ConfirmationConverters::class)
abstract class TestConfirmationDatabase : RoomDatabase() {
    abstract fun confirmationDao(): ConfirmationDao
}
