package io.nemopill.scheduling.application

import io.nemopill.core.port.ClockPort
import io.nemopill.core.result.Result
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/** Static, non-PII failure message for the unexpected-failure tier (ADR-031, ADR-049 rule (i)). */
private const val UNEXPECTED_REARM_FAILURE = "Failed to re-arm the demo reminder"

/**
 * The boot-survival re-arm use case (T-012, file 06 § F-011 happy path; M-002 Done-When item (6)).
 *
 * `AlarmManager` alarms are cleared on reboot, so on `BOOT_COMPLETED` the boot receiver invokes this
 * use case to re-register every persisted pending Reminder whose target is **still in the future**.
 * A past-due target (`target <= now`) is **left un-armed** — the F-008 missed-sweep (fire-late /
 * mark-missed) is M-004, not this slice. Re-arming through [SchedulerPort] is idempotent by `DoseId`
 * (BR-004 / file 04 § SchedulerPort), so this is safe to invoke on every boot and never duplicates.
 *
 * **Walking-skeleton scope (file 06 § F-011, demo slice):** M-002 holds the single hardcoded
 * `DEMO_DOSE_ID` entry, so iterating the store re-arms exactly that demo Reminder. The full
 * multi-Dose F-011 / F-012 boot re-registration over the BR-009 horizon, the F-008 wake-sweep, and
 * the BR-010 1-hour-late-window re-arm are **M-004**.
 *
 * Two-tier error policy (file 04): an adapter-boundary exception is caught and mapped to a
 * [Result.Err.Unexpected] with a static, non-PII message; the `CancellationException` rethrow keeps
 * structured concurrency correct. The boot receiver bounds the call with `withTimeout` and always
 * runs `goAsync()`'s `finish()`.
 */
class ReArmDemoReminderUseCase
    @Inject
    constructor(
        private val clock: ClockPort,
        private val scheduler: SchedulerPort,
        private val pendingReminderStore: PendingReminderStore,
    ) {
        suspend operator fun invoke(): Result<Unit, Result.Err> =
            try {
                val now = clock.now()
                pendingReminderStore
                    .pendingReminders()
                    .filterValues { target -> target.isAfter(now) }
                    .forEach { (doseId, target) -> scheduler.scheduleReminder(doseId, target) }
                Result.Ok(Unit)
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                Result.Err.Unexpected(UNEXPECTED_REARM_FAILURE)
            }
    }
