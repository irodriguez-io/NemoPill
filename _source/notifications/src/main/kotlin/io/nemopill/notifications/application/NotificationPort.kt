package io.nemopill.notifications.application

import io.nemopill.core.event.ReminderVariant
import io.nemopill.core.id.DoseId

/**
 * The `:notifications` outbound port (file 04 § NotificationPort). Side-effecting and
 * channel-aware; **no business logic** — it renders what the Application layer tells it to.
 *
 * T-009 exposes only [presentReminder] for the on-time Reminder. The `presentMissed` /
 * `presentBatchedSummary` surfaces (BR-010 / M-004) are out of scope. The single implementation
 * is `NotificationManagerNotificationAdapter` in `:notifications::infrastructure`.
 */
interface NotificationPort {
    suspend fun presentReminder(
        doseId: DoseId,
        variant: ReminderVariant,
    )
}
