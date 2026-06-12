package io.nemopill.scheduling.application

import io.nemopill.core.id.DoseId
import java.time.Instant

/**
 * Fake [SchedulerPort] that records calls and models the port's idempotent-by-`doseId` contract
 * (BR-004): re-scheduling the same Dose replaces, never duplicates. Shared across
 * `ScheduleDemoReminderUseCaseTest` (AC-002 / AC-004) and `ReArmDemoReminderUseCaseTest` (AC-002).
 */
class RecordingSchedulerPort : SchedulerPort {
    var scheduleCallCount = 0
        private set
    val activeRegistrations = mutableListOf<Pair<DoseId, Instant>>()

    override suspend fun scheduleReminder(
        doseId: DoseId,
        at: Instant,
    ) {
        scheduleCallCount++
        activeRegistrations.removeAll { it.first == doseId }
        activeRegistrations.add(doseId to at)
    }

    override suspend fun cancel(doseId: DoseId) {
        activeRegistrations.removeAll { it.first == doseId }
    }

    override suspend fun cancelAll() {
        activeRegistrations.clear()
    }
}
