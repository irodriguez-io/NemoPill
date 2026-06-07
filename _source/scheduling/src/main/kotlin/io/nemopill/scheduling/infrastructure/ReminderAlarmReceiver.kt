package io.nemopill.scheduling.infrastructure

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import io.nemopill.core.event.DomainEventPublisher
import io.nemopill.core.event.ReminderFired
import io.nemopill.core.event.ReminderVariant
import io.nemopill.core.id.DoseId
import io.nemopill.core.port.ClockPort
import io.nemopill.scheduling.EXTRA_DOSE_ID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.cancellation.CancellationException

/**
 * Alarm entry point for a Dose's Reminder. Lives in `:scheduling::infrastructure` (file 04) so
 * [AlarmManagerSchedulerAdapter] can target it without a `:scheduling -> :app` dependency.
 * Declared `exported="false"` in `:app`'s manifest (file 05 / file 06 § F-005 trust boundary).
 *
 * T-009 replaces the T-008 no-op stub with the cross-Bounded-Context seam: on fire, the receiver
 * reads the aggregate [DoseId] from the Intent extra (the only entry-point parse — file 06 §
 * AlarmManager PendingIntent boundary) and publishes a [ReminderFired] `ON_TIME` event onto the
 * `:core` [DomainEventPublisher]. `:scheduling` never imports `:notifications`; the seam is the
 * `:core` event bus, which a `:notifications` listener consumes (file 04 § Cross-module
 * communication — the load-bearing boundary this slice proves).
 *
 * **Walking-skeleton scope (file 06 § F-005, demo slice):** the real `FireReminderUseCase`
 * precondition checks (steps 2–3: `Dose.status == pending`, the BR-010 1-hour window) require
 * Room and a persisted `Dose` and are out of M-002-early scope. This receiver publishes the
 * `ON_TIME` event unconditionally for the single hardcoded demo Dose; the precondition-checked
 * fire path is M-004.
 *
 * **Injection (T-009 ADR):** a manifest `BroadcastReceiver` is OS-instantiated and cannot be
 * constructor-injected. `@AndroidEntryPoint` field injection is unavailable here because its
 * `super.onReceive(...)` hook targets `BroadcastReceiver.onReceive`, which is abstract and so
 * cannot be called from Kotlin source (the Hilt superclass rewrite happens post-compile). Instead
 * the dependencies are pulled from the application's SingletonComponent via [EntryPointAccessors]
 * and the [ReminderAlarmEntryPoint] below. This requires the Hilt Gradle plugin + KSP in
 * `:scheduling`, refining the T-008 ADR-083 minimal-Hilt-placement choice for the receiver case.
 *
 * **`goAsync()` lifetime (file 06 § Timeout rule):** the publish runs on a bounded coroutine scope
 * under a short [PUBLISH_TIMEOUT_MS] cap, well under the OS ~10-second receiver lifetime;
 * `PendingResult.finish()` runs in `finally`. Log lines are static and non-PII (ADR-031 / ADR-049
 * rule (i)).
 */
class ReminderAlarmReceiver : BroadcastReceiver() {
    /** Hilt access to the SingletonComponent singletons this receiver needs. */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ReminderAlarmEntryPoint {
        fun domainEventPublisher(): DomainEventPublisher

        fun clockPort(): ClockPort
    }

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val rawDoseId = intent.getStringExtra(EXTRA_DOSE_ID)
        if (rawDoseId == null) {
            Log.w(TAG, "Reminder alarm received without a dose id; ignoring")
            return
        }

        val entryPoint =
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                ReminderAlarmEntryPoint::class.java,
            )
        val publisher = entryPoint.domainEventPublisher()
        val clock = entryPoint.clockPort()

        val pending = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope.launch {
            try {
                withTimeout(PUBLISH_TIMEOUT_MS) {
                    publisher.publish(
                        ReminderFired(DoseId(rawDoseId), clock.now(), ReminderVariant.ON_TIME),
                    )
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                Log.w(TAG, "Failed to publish ReminderFired")
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "NemoPill.scheduling.ReminderAlarmReceiver"

        /** Well under the OS ~10-second receiver lifetime (file 06 § Timeout rule, file 12). */
        const val PUBLISH_TIMEOUT_MS = 5_000L
    }
}
