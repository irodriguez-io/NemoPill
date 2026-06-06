package io.nemopill.scheduling.application

import io.nemopill.core.id.DoseId
import java.time.Instant

/**
 * Registers and cancels a Dose's Reminder with the OS scheduler (file 04 § SchedulerPort).
 *
 * Idempotent by [DoseId] — re-scheduling the same Dose replaces, never duplicates
 * (BR-004 exactly-once). The adapter achieves this with a stable PendingIntent request
 * code derived from the `doseId`. [cancel] supports the BR-006 / BR-007 / BR-012 deletion
 * paths; [cancelAll] supports test resets.
 */
interface SchedulerPort {
    suspend fun scheduleReminder(
        doseId: DoseId,
        at: Instant,
    )

    suspend fun cancel(doseId: DoseId)

    suspend fun cancelAll()
}
