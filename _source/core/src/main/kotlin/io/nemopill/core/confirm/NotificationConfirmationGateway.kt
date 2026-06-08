package io.nemopill.core.confirm

import io.nemopill.core.id.DoseId
import io.nemopill.core.result.Result

/**
 * The **receiver → use-case seam** (T-010 ADR — resolves the surfaced file-04 ↔ file-06/13
 * tension). File 04 § Gradle dependency graph forbids feature modules from depending on each
 * other; file 06 § F-006 / file 13's "PendingIntent → notification-action receiver" row place
 * `ConfirmFromNotificationReceiver` in `:notifications` yet have it "invoke
 * `ConfirmDoseFromReminderUseCase` in `:adherence-tracking::application`". A class in
 * `:notifications` cannot reference a type in `:adherence-tracking` without an illegal
 * cross-feature Gradle dependency.
 *
 * The resolution (recommended option (C)) is a neutral `:core` contract: `:notifications` depends
 * only on this `:core` port (it already depends on `:core`); `:adherence-tracking` provides the
 * implementation that delegates to its `ConfirmDoseFromReminderUseCase`; `:app` binds port → impl
 * in DI. This respects the strict graph (both feature modules depend only on `:core`), preserves
 * the **command** semantics of F-006 ("invokes the use case") rather than pushing a command
 * through the past-tense `DomainEventPublisher`, and is directly precedented by
 * [io.nemopill.core.event.DomainEventPublisher] itself living in `:core` as the cross-cutting
 * contract.
 *
 * Pure interface — no Android imports (it lives in the `:core` shared kernel). The typed
 * [ConfirmationStatus] parse happens in the receiver before this is called (file 05 § Input
 * validation); an unknown payload never reaches here.
 */
interface NotificationConfirmationGateway {
    suspend fun confirm(
        doseId: DoseId,
        status: ConfirmationStatus,
    ): Result<Unit, Result.Err>
}
