package io.nemopill.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.nemopill.core.event.DomainEventPublisher
import io.nemopill.core.event.InProcessEventBus
import javax.inject.Singleton

/**
 * Binds the `:core` cross-module event seam (file 04 § DomainEventPublisher). One
 * [InProcessEventBus] per process — an `@Singleton`, so every publisher (the
 * `ReminderAlarmReceiver`) and every subscriber (the `ReminderFiredListener`) share the same bus
 * and its replay buffer.
 *
 * `@Provides` (not `@Binds`): [InProcessEventBus] is a `:core` `kotlin("jvm")` type with no
 * `@Inject` constructor — keeping Hilt out of the shared kernel — so it is constructed here,
 * mirroring how `SchedulingModule` provides `SystemClockAdapter`.
 */
@Module
@InstallIn(SingletonComponent::class)
object CoreEventModule {
    @Provides
    @Singleton
    fun provideDomainEventPublisher(): DomainEventPublisher = InProcessEventBus()
}
