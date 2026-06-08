package io.nemopill.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.nemopill.adherencetracking.infrastructure.ConfirmationDao
import io.nemopill.app.NemoPillDatabase
import javax.inject.Singleton

private const val DATABASE_NAME = "nemopill.db"

/**
 * Provides the single [NemoPillDatabase] instance (one per process — `@Singleton`, file 04) and
 * the bindings the persistence graph needs. `Room.databaseBuilder(...)` with **no**
 * `fallbackToDestructiveMigration` (file 06 § Section 2 — never outside test builds).
 *
 * [provideRoomDatabase] exposes the same singleton as the base `RoomDatabase` type so
 * `RoomConfirmationRepository` (which lives in `:adherence-tracking` and cannot import the
 * `:app`-resident concrete `NemoPillDatabase`) can be constructor-injected with it (T-010 ADR —
 * base-type injection across the module boundary).
 */
@Module
@InstallIn(SingletonComponent::class)
object PersistenceModule {
    @Provides
    @Singleton
    fun provideNemoPillDatabase(
        @ApplicationContext context: Context,
    ): NemoPillDatabase = Room.databaseBuilder(context, NemoPillDatabase::class.java, DATABASE_NAME).build()

    @Provides
    fun provideConfirmationDao(database: NemoPillDatabase): ConfirmationDao = database.confirmationDao()

    @Provides
    fun provideRoomDatabase(database: NemoPillDatabase): RoomDatabase = database
}
