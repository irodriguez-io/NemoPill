package io.nemopill.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.nemopill.adherencetracking.application.ConfirmationRepository
import io.nemopill.adherencetracking.infrastructure.AdherenceConfirmationGateway
import io.nemopill.adherencetracking.infrastructure.RoomConfirmationRepository
import io.nemopill.core.confirm.NotificationConfirmationGateway
import javax.inject.Singleton

/**
 * The project's third feature `@Module` (after `SchedulingModule`, `NotificationsModule`). Binds:
 *  - the `:adherence-tracking` outbound port `ConfirmationRepository` → its Room adapter; and
 *  - the `:core` receiver→use-case seam port [NotificationConfirmationGateway] →
 *    `:adherence-tracking`'s [AdherenceConfirmationGateway] (which delegates to
 *    `ConfirmDoseFromReminderUseCase`) — the binding that satisfies the seam (T-010 ADR). The
 *    `:notifications` `ConfirmFromNotificationReceiver` resolves the port through this binding via
 *    `EntryPointAccessors`.
 *
 * `@Binds` only: [RoomConfirmationRepository] and [AdherenceConfirmationGateway] are
 * constructor-injected (their dependencies — the `RoomDatabase` / `ConfirmationDao` from
 * `PersistenceModule`, the `ClockPort` from `SchedulingModule`, the `DomainEventPublisher` from
 * `CoreEventModule` — all resolve from the SingletonComponent), so no extra `@Provides` is needed.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AdherenceModule {
    @Binds
    @Singleton
    abstract fun bindConfirmationRepository(impl: RoomConfirmationRepository): ConfirmationRepository

    @Binds
    @Singleton
    abstract fun bindNotificationConfirmationGateway(impl: AdherenceConfirmationGateway): NotificationConfirmationGateway
}
