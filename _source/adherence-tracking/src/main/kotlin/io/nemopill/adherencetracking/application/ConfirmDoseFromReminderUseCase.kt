package io.nemopill.adherencetracking.application

import io.nemopill.adherencetracking.domain.Confirmation
import io.nemopill.core.confirm.ConfirmationSource
import io.nemopill.core.confirm.ConfirmationStatus
import io.nemopill.core.event.DomainEventPublisher
import io.nemopill.core.event.DoseConfirmed
import io.nemopill.core.id.ConfirmationId
import io.nemopill.core.id.DoseId
import io.nemopill.core.port.ClockPort
import io.nemopill.core.result.Result
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/** Static, non-PII failure message for the unexpected-failure tier (ADR-031, ADR-049 rule (i)). */
private const val UNEXPECTED_CONFIRM_FAILURE = "Failed to record the dose confirmation"

/**
 * UC-004 — confirm a Dose from the on-time Reminder notification (file 06 § F-006, demo slice).
 * The first `:adherence-tracking` Application use case.
 *
 * Accepts the already-typed `(doseId, status)` from the receiver entry point (BR-005 enum bounds
 * enforced at parse time), constructs a [Confirmation] with `source = NOTIFICATION_ACTION` and
 * `confirmedAt = ClockPort.now()`, persists it via [ConfirmationRepository.recordFromNotification]
 * (idempotent on `doseId`), and on success publishes [DoseConfirmed] (file 06 § F-006 step 7;
 * no cross-module consumer in MVP — file 06 line 35).
 *
 * **Walking-skeleton scope (T-010):** does **not** depend on a `DoseRepository` and does **not**
 * update `Dose.status` — there is no persisted `Dose` in M-002 (no `Medication`, no Dose
 * materialization). The `Dose.status` co-update half of F-006 (step 4 second half) and the
 * cross-module `:scheduling` `DoseRepository` dependency (file 04 § Architecture Risks) are
 * deferred to M-004 / M-005 when real Doses exist. BR-011 (24-hour retroactive window) is
 * **vacuously satisfied** at on-time confirm; its real check needs `Dose.scheduledAt` from Room
 * and is also deferred. Recorded as the demo-vs-full-F-006 gap (T-010 ADR).
 *
 * Two-tier error policy (file 04): an adapter-boundary exception is caught and mapped to
 * [Result.Err.Unexpected] with a static message; the [CancellationException] rethrow keeps
 * structured concurrency correct.
 */
class ConfirmDoseFromReminderUseCase
    @Inject
    constructor(
        private val repository: ConfirmationRepository,
        private val clock: ClockPort,
        private val publisher: DomainEventPublisher,
    ) {
        suspend operator fun invoke(
            doseId: DoseId,
            status: ConfirmationStatus,
        ): Result<Unit, Result.Err> =
            try {
                val now = clock.now()
                val confirmation =
                    Confirmation(
                        confirmationId = ConfirmationId.random(),
                        doseId = doseId,
                        status = status,
                        confirmedAt = now,
                        source = ConfirmationSource.NOTIFICATION_ACTION,
                    )
                when (val recorded = repository.recordFromNotification(confirmation)) {
                    is Result.Ok -> {
                        publisher.publish(
                            DoseConfirmed(
                                doseId = doseId,
                                confirmationId = confirmation.confirmationId,
                                status = status,
                                confirmedAt = now,
                                source = ConfirmationSource.NOTIFICATION_ACTION,
                            ),
                        )
                        Result.Ok(Unit)
                    }
                    is Result.Err -> recorded
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                Result.Err.Unexpected(UNEXPECTED_CONFIRM_FAILURE)
            }
    }
