package io.nemopill.app

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.nemopill.adherencetracking.infrastructure.ConfirmationConverters
import io.nemopill.adherencetracking.infrastructure.ConfirmationDao
import io.nemopill.adherencetracking.infrastructure.ConfirmationEntity

/**
 * The project's **single Room database** (file 04 § Architecture Risks; file 06 § Section 1 —
 * "`NemoPillDatabase` — single Room instance"). The single-instance design is load-bearing: the
 * Dose-aggregate transactional invariant (file 04) requires one `withTransaction` block to span
 * `:scheduling`'s `Dose` and `:adherence-tracking`'s `Confirmation`, which is only possible if all
 * feature DAOs live on one database.
 *
 * The `@Database` lives in `:app` because `:app` is the only module that may depend on the feature
 * modules whose DAOs / entities it aggregates (file 04 Gradle graph). It is in the `io.nemopill.app`
 * root package (not a `persistence` sub-package) so the exported schema directory is exactly
 * `app/schemas/io.nemopill.app.NemoPillDatabase/` per AC-005 (Room names the schema directory by
 * the database's fully-qualified class name).
 *
 * T-010 registers the first entity ([ConfirmationEntity]); later milestones add `Medication` /
 * `DoseSchedule` / `Dose` with a `Migration(from, to)` and a new committed schema JSON per file 06
 * § Section 2 (strict version integer; no destructive migrations on `main` —
 * `fallbackToDestructiveMigration` is never enabled outside test builds).
 *
 * `exportSchema = true` writes the schema to `app/schemas/io.nemopill.app.NemoPillDatabase/<v>.json`
 * (the `room.schemaLocation` KSP arg in this module's build script); `1.json` is committed with
 * this slice.
 */
@Database(
    entities = [ConfirmationEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(ConfirmationConverters::class)
abstract class NemoPillDatabase : RoomDatabase() {
    abstract fun confirmationDao(): ConfirmationDao
}
