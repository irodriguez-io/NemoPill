package io.nemopill.notifications.application

import io.nemopill.core.event.ReminderVariant
import io.nemopill.core.id.DoseId
import io.nemopill.core.result.Result
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/** Static, non-PII failure message for the unexpected-failure tier (ADR-031, ADR-049 rule (i)). */
private const val UNEXPECTED_PRESENT_FAILURE = "Failed to present the reminder notification"

/**
 * The `:notifications` first Application use case (file 04 § Application). Renders a Reminder by
 * delegating to [NotificationPort]; all I/O lives behind the port.
 *
 * Two-tier error policy (file 04): an adapter-boundary exception is caught and mapped to a
 * [Result.Err.Unexpected] with a static, non-PII message. The `CancellationException` rethrow
 * keeps structured concurrency correct (ADR-049 rule (i) — the `throw` carries no message).
 *
 * The demo Reminder copy is hardcoded, non-PII placeholder content resolved in the infrastructure
 * builder (file 11 § notification copy); this use case carries no Patient data.
 */
class PresentReminderUseCase
    @Inject
    constructor(
        private val notifications: NotificationPort,
    ) {
        suspend operator fun invoke(
            doseId: DoseId,
            variant: ReminderVariant,
        ): Result<Unit, Result.Err> =
            try {
                notifications.presentReminder(doseId, variant)
                Result.Ok(Unit)
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                Result.Err.Unexpected(UNEXPECTED_PRESENT_FAILURE)
            }
    }
