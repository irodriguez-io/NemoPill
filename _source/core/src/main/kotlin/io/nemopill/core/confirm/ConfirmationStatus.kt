package io.nemopill.core.confirm

/**
 * The bounded outcome of confirming a Dose (file 02 § `BR-005`): a Confirmation's `status` is
 * `taken` or `skipped` — **never** `pending` / `missed` (those are `Dose.status` values, not
 * Confirmation outcomes).
 *
 * Lives in `:core::confirm` (T-010 seam ADR), not in `:adherence-tracking::domain`, because two
 * modules need it: the `:notifications` `ConfirmFromNotificationReceiver` types the inbound
 * notification-action `status` extra into this enum at the IPC entry point (file 05 § Input
 * validation), and `:core::event::DoseConfirmed` carries it. It is genuinely shared-kernel
 * vocabulary; `:adherence-tracking::domain::Confirmation` re-uses it.
 *
 * An `enum`, not a `data class` — so it does not itself trigger ADR-049 rule (ii). Its value is
 * Confidential per file 13 § Data Classification, so any `data class` that carries it (e.g.
 * [io.nemopill.core.event.DoseConfirmed], `Confirmation`) must redact its `toString()`.
 */
enum class ConfirmationStatus {
    TAKEN,
    SKIPPED,
}
