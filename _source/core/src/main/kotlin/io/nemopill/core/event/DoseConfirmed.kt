package io.nemopill.core.event

import io.nemopill.core.confirm.ConfirmationSource
import io.nemopill.core.confirm.ConfirmationStatus
import io.nemopill.core.id.ConfirmationId
import io.nemopill.core.id.DoseId
import java.time.Instant

/**
 * A Dose has been confirmed (file 02 § Domain Events row `DoseConfirmed`; file 06 § F-006 step 7).
 *
 * Emitted by `:adherence-tracking::application::ConfirmDoseFromReminderUseCase` on a successful
 * `Confirmation` write. **No cross-module consumer in T-010** (file 06 line 35: "No cross-module
 * consumer in MVP"); the on-screen Adherence counter (M-002 item 5, T-011) is the first consumer.
 *
 * **Dedupe key is `(confirmationId, confirmedAt)`** (file 02 § Domain Events). UC-007 correction
 * (M-005) re-emits with the same `confirmationId` per Option A in-place mutation, so subscribers
 * must dedupe on the pair, not on `confirmationId` alone.
 *
 * Carries Confidential [status] per file 13 § Data Classification, so its `toString()` is
 * **redacted** (ADR-049 rule (ii) — wired in T-010): the auto-generated `data class` `toString()`
 * would dump the field values into logcat / crash evidence (ADR-031). The asserted scope of rule
 * (ii) (ADR-087 resolution) covers `:core::event` `data class`es that carry Confidential fields,
 * which is exactly this type; [ReminderFired] (only `DoseId` + `Instant` + a non-Confidential
 * variant) stays exempt.
 */
data class DoseConfirmed(
    val doseId: DoseId,
    val confirmationId: ConfirmationId,
    val status: ConfirmationStatus,
    val confirmedAt: Instant,
    val source: ConfirmationSource,
) : DomainEvent {
    /** Redacted per ADR-049 rule (ii) / ADR-031 — no Confidential field values reach logcat. */
    override fun toString(): String = "DoseConfirmed(REDACTED)"
}
