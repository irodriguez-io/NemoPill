package io.nemopill.core.event

import io.nemopill.core.id.DoseId
import java.time.Instant

/**
 * A Dose's Reminder alarm has fired (file 02 § Domain Events; file 06 § F-005).
 *
 * Emitted by `:scheduling` when an `AlarmManager` alarm fires; consumed by `:notifications`
 * to render the Patient-visible Reminder. Carries **no Patient data** — [doseId] is an opaque
 * shared-kernel ID permitted in evidence (file 13), [firedAt] is a clock instant, and [variant]
 * is a bounded enum.
 *
 * **Dedupe key is [doseId]** (file 06 § Idempotency rule): subscribers derive a stable
 * presentation/notification identity from `doseId`, so a replayed event re-renders the same
 * notification rather than stacking duplicates.
 */
data class ReminderFired(
    val doseId: DoseId,
    val firedAt: Instant,
    val variant: ReminderVariant,
) : DomainEvent

/**
 * The BR-010 Reminder variants. Only [ON_TIME] is emitted in T-009; [LATE] is declared because
 * BR-010's late variant is a near-term M-004 need and this enum is additive-only (file 06). No
 * `LATE` code path is exercised yet.
 */
enum class ReminderVariant {
    ON_TIME,
    LATE,
}
