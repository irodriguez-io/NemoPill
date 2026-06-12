package io.nemopill.scheduling.application

import io.nemopill.core.id.DoseId
import java.time.Instant

/**
 * The project's first persisted scheduling state (T-012, file 04 § SchedulerPort / file 06 § F-011).
 *
 * `AlarmManager` exact alarms are **cleared by the OS on reboot**, so the boot-survival leg needs a
 * durable record of each pending Reminder's target time to re-arm it on `BOOT_COMPLETED`. This port
 * persists a `DoseId -> Instant` mapping — **only** a non-PII Dose identifier and an epoch-millis
 * trigger time, never Patient data (ADR-031; `_context/13` `Internal` / trigger-metadata class). The
 * M-002 demo holds at most the single `DEMO_DOSE_ID` entry; the `Map` shape precedents the M-004
 * multi-Dose boot re-registration (F-011 "reads every pending Dose ... for each: schedule") so that
 * path can swap the backing implementation (e.g. to the `Dose`/Room read path) without touching the
 * boot receiver or [ReArmDemoReminderUseCase].
 *
 * The adapter ([io.nemopill.scheduling.infrastructure.SharedPreferencesPendingReminderStore]) lives
 * in `:scheduling::infrastructure` behind this Application port (file 04 layer table;
 * `NoUpwardLayerDependencyRule`). Re-arming through [SchedulerPort] is idempotent by [DoseId]
 * (BR-004), so re-reading and re-scheduling the same target is always safe.
 */
interface PendingReminderStore {
    /** Persist (overwriting any prior entry for [doseId]) the Reminder target time. */
    suspend fun savePendingReminder(
        doseId: DoseId,
        at: Instant,
    )

    /** A snapshot of every persisted pending Reminder. Empty when none have been scheduled. */
    suspend fun pendingReminders(): Map<DoseId, Instant>

    /** Remove the persisted entry for [doseId]. A no-op when absent. */
    suspend fun clear(doseId: DoseId)
}
