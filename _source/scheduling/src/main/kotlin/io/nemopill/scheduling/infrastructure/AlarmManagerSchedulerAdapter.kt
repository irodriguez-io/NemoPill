package io.nemopill.scheduling.infrastructure

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import io.nemopill.core.id.DoseId
import io.nemopill.scheduling.DEMO_DOSE_ID
import io.nemopill.scheduling.REQUEST_CODE_BASE
import io.nemopill.scheduling.SHOW_INTENT_REQUEST_CODE
import io.nemopill.scheduling.application.SchedulerPort
import java.time.Instant
import javax.inject.Inject

/**
 * Real [SchedulerPort] backed by [AlarmManager.setAlarmClock] — the most reliable Doze
 * bypass for exact Reminder firing (file 04 § Exact-time scheduling; retires file 01
 * assumption (1) for the scheduling leg; BR-004).
 *
 * Idempotent by [DoseId]: the operation PendingIntent uses a stable request code derived
 * from the `doseId` plus `FLAG_UPDATE_CURRENT`, so re-scheduling the same Dose replaces
 * the existing alarm rather than duplicating it (BR-004 exactly-once).
 *
 * Every PendingIntent — both the `operation` and the optional `showIntent` — is built with
 * `FLAG_IMMUTABLE` (ADR-023 security guardrail), blocking intent-redirection on Android 12+.
 * The `showIntent` resolves the app's launcher Intent at runtime rather than referencing
 * `:app`'s `MainActivity` directly, preserving the `:scheduling`-must-not-import-`:app`
 * module boundary.
 */
class AlarmManagerSchedulerAdapter
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val alarmManager: AlarmManager,
    ) : SchedulerPort {
        override suspend fun scheduleReminder(
            doseId: DoseId,
            at: Instant,
        ) {
            val info = AlarmManager.AlarmClockInfo(at.toEpochMilli(), buildShowIntent())
            alarmManager.setAlarmClock(info, operationFor(doseId))
        }

        override suspend fun cancel(doseId: DoseId) {
            alarmManager.cancel(operationFor(doseId))
        }

        override suspend fun cancelAll() {
            // The T-008 demo slice schedules only the single demo Dose, so cancelling it is a
            // sufficient reset. Full multi-Dose cancellation lands with Dose materialization.
            cancel(DEMO_DOSE_ID)
        }

        private fun operationFor(doseId: DoseId): PendingIntent {
            val intent =
                Intent(context, ReminderAlarmReceiver::class.java)
                    .putExtra(EXTRA_DOSE_ID, doseId.value)
            return PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_BASE + doseId.value.hashCode(),
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }

        private fun buildShowIntent(): PendingIntent? {
            val launch =
                context.packageManager.getLaunchIntentForPackage(context.packageName)
                    ?: return null
            return PendingIntent.getActivity(
                context,
                SHOW_INTENT_REQUEST_CODE,
                launch,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }

        private companion object {
            /** Aggregate ID only (ADR-031); the receiver reads it when notification rendering lands. */
            const val EXTRA_DOSE_ID = "io.nemopill.scheduling.extra.DOSE_ID"
        }
    }
