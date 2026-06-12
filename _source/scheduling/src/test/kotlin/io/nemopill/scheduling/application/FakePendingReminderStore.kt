package io.nemopill.scheduling.application

import io.nemopill.core.id.DoseId
import java.time.Instant

/**
 * In-memory [PendingReminderStore] fake (T-012). Models the port's `DoseId -> Instant` map
 * contract: [savePendingReminder] overwrites by `DoseId`, [pendingReminders] returns a snapshot,
 * [clear] removes one entry. Reused across the `ReArmDemoReminderUseCaseTest` (AC-002) and the
 * amended `ScheduleDemoReminderUseCaseTest` (AC-004).
 */
class FakePendingReminderStore : PendingReminderStore {
    private val entries = mutableMapOf<DoseId, Instant>()

    var saveCallCount = 0
        private set

    override suspend fun savePendingReminder(
        doseId: DoseId,
        at: Instant,
    ) {
        saveCallCount++
        entries[doseId] = at
    }

    override suspend fun pendingReminders(): Map<DoseId, Instant> = entries.toMap()

    override suspend fun clear(doseId: DoseId) {
        entries.remove(doseId)
    }
}
