package io.nemopill.scheduling.application

import io.nemopill.core.port.ClockPort
import io.nemopill.core.result.Result
import io.nemopill.scheduling.DEMO_DOSE_ID
import io.nemopill.scheduling.DEMO_REMINDER_OFFSET
import java.time.Instant
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/** Static, non-PII failure message for the unexpected-failure tier (ADR-031, ADR-049 rule (i)). */
private const val UNEXPECTED_SCHEDULE_FAILURE = "Failed to schedule the demo reminder"

/**
 * The project's first Application use case (file 04 § Application).
 *
 * Reads `now` from [ClockPort], computes `now + 10 min`, and schedules the demo Reminder
 * through [SchedulerPort], returning the scheduled [Instant] on success. All I/O is via
 * ports; this `suspend` function is the only side-effecting layer (the Domain stays pure
 * and non-suspending).
 *
 * Two-tier error policy (file 04): an adapter-boundary exception is caught and mapped to a
 * [Result.Err.Unexpected] with a static, non-PII message. The `CancellationException`
 * rethrow keeps structured concurrency correct — and is T-008's first production `throw`,
 * which the ADR-049 rule (i) Konsist check exercises.
 */
class ScheduleDemoReminderUseCase
    @Inject
    constructor(
        private val clock: ClockPort,
        private val scheduler: SchedulerPort,
    ) {
        suspend operator fun invoke(): Result<Instant, Result.Err> =
            try {
                val target = clock.now().plus(DEMO_REMINDER_OFFSET)
                scheduler.scheduleReminder(DEMO_DOSE_ID, target)
                Result.Ok(target)
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                Result.Err.Unexpected(UNEXPECTED_SCHEDULE_FAILURE)
            }
    }
