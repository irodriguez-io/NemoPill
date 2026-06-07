package io.nemopill.notifications.application

import io.nemopill.core.event.ReminderVariant
import io.nemopill.core.id.DoseId

/**
 * Test [NotificationPort] that records `presentReminder` calls for the listener / use-case tests.
 * Optionally throws to exercise the [io.nemopill.core.result.Result.Err.Unexpected] mapping.
 */
class FakeNotificationPort(
    private val failWith: Throwable? = null,
) : NotificationPort {
    val presentCalls = mutableListOf<Pair<DoseId, ReminderVariant>>()

    override suspend fun presentReminder(
        doseId: DoseId,
        variant: ReminderVariant,
    ) {
        failWith?.let { throw it }
        presentCalls.add(doseId to variant)
    }
}
