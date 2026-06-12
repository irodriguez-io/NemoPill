package io.nemopill.app.di

import android.app.AlarmManager
import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.nemopill.core.port.ClockPort
import io.nemopill.core.port.SystemClockAdapter
import io.nemopill.scheduling.application.PendingReminderStore
import io.nemopill.scheduling.application.SchedulerPort
import io.nemopill.scheduling.infrastructure.AlarmManagerSchedulerAdapter
import io.nemopill.scheduling.infrastructure.SharedPreferencesPendingReminderStore
import javax.inject.Singleton

/**
 * The project's first Hilt `@Module` (file 04 § app DI wiring). Binds the scheduling-leg
 * ports to their adapters and provides the framework objects those adapters need.
 *
 * `@Binds` for [SchedulerPort] and the T-012 [PendingReminderStore] (both adapters are
 * constructor-injected); `@Provides` for [ClockPort] (its `:core` adapter is a plain class with no
 * `@Inject`, keeping the shared kernel free of Hilt) and for the [AlarmManager] system service.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SchedulingModule {
    @Binds
    @Singleton
    abstract fun bindSchedulerPort(impl: AlarmManagerSchedulerAdapter): SchedulerPort

    @Binds
    @Singleton
    abstract fun bindPendingReminderStore(impl: SharedPreferencesPendingReminderStore): PendingReminderStore

    companion object {
        @Provides
        @Singleton
        fun provideClockPort(): ClockPort = SystemClockAdapter()

        @Provides
        @Singleton
        fun provideAlarmManager(
            @ApplicationContext context: Context,
        ): AlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }
}
