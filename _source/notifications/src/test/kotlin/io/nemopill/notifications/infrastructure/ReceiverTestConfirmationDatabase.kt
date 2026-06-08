package io.nemopill.notifications.infrastructure

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.nemopill.adherencetracking.infrastructure.ConfirmationConverters
import io.nemopill.adherencetracking.infrastructure.ConfirmationDao
import io.nemopill.adherencetracking.infrastructure.ConfirmationEntity

/**
 * Test-only Room database for the end-to-end receiver test ([ConfirmFromNotificationReceiverRobolectricTest]).
 * Hosts the `:adherence-tracking` `confirmation` slice (reachable via the test-only
 * `:adherence-tracking` dependency) so the receiver → gateway → use case →
 * `RoomConfirmationRepository` chain writes to a real in-memory SQLite. The production aggregate
 * `NemoPillDatabase` lives in `:app` and cannot be referenced from a library-module test; this
 * test `@Database` stands in (Room KSP codegen runs in this module's test source set).
 */
@Database(entities = [ConfirmationEntity::class], version = 1, exportSchema = false)
@TypeConverters(ConfirmationConverters::class)
abstract class ReceiverTestConfirmationDatabase : RoomDatabase() {
    abstract fun confirmationDao(): ConfirmationDao
}
