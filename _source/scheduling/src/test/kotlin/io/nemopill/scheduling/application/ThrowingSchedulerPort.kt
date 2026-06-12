package io.nemopill.scheduling.application

import io.nemopill.core.id.DoseId
import java.time.Instant

/**
 * Fake [SchedulerPort] whose `scheduleReminder` throws, exercising the use-case unexpected-failure
 * tier (the catch-and-map-to-[io.nemopill.core.result.Result.Err.Unexpected] policy). Shared across
 * `ScheduleDemoReminderUseCaseTest` and `ReArmDemoReminderUseCaseTest`.
 */
class ThrowingSchedulerPort : SchedulerPort {
    override suspend fun scheduleReminder(
        doseId: DoseId,
        at: Instant,
    ): Unit = throw IllegalStateException("simulated adapter failure")

    override suspend fun cancel(doseId: DoseId) = Unit

    override suspend fun cancelAll() = Unit
}
