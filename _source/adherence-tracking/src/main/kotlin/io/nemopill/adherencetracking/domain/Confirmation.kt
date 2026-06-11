package io.nemopill.adherencetracking.domain

import io.nemopill.core.confirm.ConfirmationSource
import io.nemopill.core.confirm.ConfirmationStatus
import io.nemopill.core.id.ConfirmationId
import io.nemopill.core.id.DoseId
import java.time.Instant

/**
 * A Patient's confirmation that a Dose was [ConfirmationStatus.TAKEN] or
 * [ConfirmationStatus.SKIPPED] (file 02 row `Confirmation`). The first `:adherence-tracking`
 * Domain entity, and the project's **first `Confidential`-class Domain `data class`** (file 13 §
 * Data Classification — "Confirmation states `taken` / `skipped`") — the trigger for ADR-049
 * rule (ii).
 *
 * Pure Kotlin, non-suspending, no Android imports (Clean-Architecture Domain boundary, ADR-009).
 * The shared identity / vocabulary newtypes ([ConfirmationId], [DoseId], [ConfirmationStatus],
 * [ConfirmationSource]) live in `:core` (T-010 seam ADR) so the `:core::event::DoseConfirmed`
 * event and the `:notifications` typed parse can also reference them; this entity re-uses them.
 *
 * `confirmedAt` enters from `ClockPort.now()` at the use-case boundary — the Domain never reads
 * the system clock directly (file 04 § ClockPort).
 */
data class Confirmation(
    val confirmationId: ConfirmationId,
    val doseId: DoseId,
    val status: ConfirmationStatus,
    val confirmedAt: Instant,
    val source: ConfirmationSource,
) {
    /**
     * Redacted per ADR-049 rule (ii) / ADR-031 — the Kotlin-generated `data class` `toString()`
     * would dump the Confidential `doseId` / `status` / `confirmedAt` into logcat / crash evidence.
     * No field values are emitted.
     */
    override fun toString(): String = "Confirmation(REDACTED)"
}
