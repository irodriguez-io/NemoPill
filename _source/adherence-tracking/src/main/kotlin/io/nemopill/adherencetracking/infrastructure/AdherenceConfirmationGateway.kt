package io.nemopill.adherencetracking.infrastructure

import io.nemopill.adherencetracking.application.ConfirmDoseFromReminderUseCase
import io.nemopill.core.confirm.ConfirmationStatus
import io.nemopill.core.confirm.NotificationConfirmationGateway
import io.nemopill.core.id.DoseId
import io.nemopill.core.result.Result
import javax.inject.Inject

/**
 * `:adherence-tracking`'s implementation of the `:core` [NotificationConfirmationGateway] seam
 * (T-010 ADR — the resolution of the file-04 ↔ file-06/13 receiver→use-case tension). The
 * `:notifications` `ConfirmFromNotificationReceiver` depends only on the `:core` port; this
 * adapter — bound to that port in `:app` DI — delegates the typed `(doseId, status)` command to
 * [ConfirmDoseFromReminderUseCase]. Neither feature module imports the other; the seam is the
 * `:core` contract.
 *
 * A thin driving adapter: it adds no behavior beyond delegation, so the two-tier error handling
 * and the `Confirmation` write all live in the use case / repository.
 */
class AdherenceConfirmationGateway
    @Inject
    constructor(
        private val confirmDoseFromReminder: ConfirmDoseFromReminderUseCase,
    ) : NotificationConfirmationGateway {
        override suspend fun confirm(
            doseId: DoseId,
            status: ConfirmationStatus,
        ): Result<Unit, Result.Err> = confirmDoseFromReminder(doseId, status)
    }
