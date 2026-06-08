package io.nemopill.core.confirm

/**
 * How a Confirmation was produced (file 02 row `Confirmation`, field `source`).
 *
 * Lives in `:core::confirm` (T-010 seam ADR) for the same reason as [ConfirmationStatus]:
 * `:core::event::DoseConfirmed` carries it, and `:core` may not depend on `:adherence-tracking`.
 *
 * Only [NOTIFICATION_ACTION] is produced in T-010 (the F-006 confirm-from-notification path).
 * [IN_APP] (UC-005) and [RETROACTIVE] (UC-006) are declared because the enum is **additive-only**
 * and their owning use cases land in M-005; no T-010 code path produces them.
 */
enum class ConfirmationSource {
    NOTIFICATION_ACTION,
    IN_APP,
    RETROACTIVE,
}
