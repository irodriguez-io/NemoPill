package io.nemopill.app

import android.app.Application
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import io.nemopill.app.di.AppStartupEntryPoint
import io.nemopill.notifications.infrastructure.NotificationChannels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application entry point and Hilt root. At cold start it (1) creates the `reminder_on_time`
 * notification channel and (2) starts the `ReminderFiredListener` subscription on an
 * application-scoped coroutine, so the listener is alive to receive bus events for the process
 * lifetime.
 *
 * The `InProcessEventBus` replay buffer covers the cold-start race where the alarm-fired receiver
 * publishes `ReminderFired` before the subscriber has fully registered (file 06 § Ordering rule).
 *
 * The listener is pulled from the SingletonComponent via [EntryPointAccessors] rather than an
 * `@Inject lateinit var` field — see [AppStartupEntryPoint] for why (Dagger 2.51.1 cannot read
 * Kotlin 2.1.0 metadata on the field-injection validation path). Kept minimal and idempotent per
 * the T-009 packet.
 */
@HiltAndroidApp
class NemoPillApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.ensureReminderOnTimeChannel(this)

        val listener =
            EntryPointAccessors
                .fromApplication(this, AppStartupEntryPoint::class.java)
                .reminderFiredListener()
        applicationScope.launch { listener.start() }
    }
}
