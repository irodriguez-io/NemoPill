package io.nemopill.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.nemopill.notifications.application.NotificationPort
import io.nemopill.notifications.infrastructure.NotificationManagerNotificationAdapter
import javax.inject.Singleton

/**
 * The project's second feature `@Module` (after `SchedulingModule`). Binds the `:notifications`
 * outbound port to its `NotificationManagerCompat`-backed adapter (file 04 § NotificationPort).
 *
 * `@Binds` only: [NotificationManagerNotificationAdapter] is constructor-injected (its
 * `@ApplicationContext` Context, the `ReminderNotificationBuilder`, and the
 * `ConfirmActionPendingIntentFactory` all resolve from the SingletonComponent), and it reads
 * `NotificationManagerCompat.from(context)` internally — so no extra `@Provides` is needed.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationsModule {
    @Binds
    @Singleton
    abstract fun bindNotificationPort(impl: NotificationManagerNotificationAdapter): NotificationPort
}
