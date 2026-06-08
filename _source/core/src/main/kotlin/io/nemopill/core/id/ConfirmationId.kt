package io.nemopill.core.id

import java.util.UUID

/**
 * Stable identity of a [io.nemopill.core.event.DoseConfirmed] / `Confirmation` (file 02 row
 * `Confirmation`; file 04 § shared-kernel ID newtypes).
 *
 * Lives in `:core::id` alongside [DoseId] (T-010 ADR — the seam choice): the `:core::event`
 * [io.nemopill.core.event.DoseConfirmed] references it, and `:core` may not depend on a feature
 * module, so the newtype cannot live in `:adherence-tracking::domain`. Symmetry with [DoseId]
 * (also in `:core::id`) settles the "Developer picks" question from the T-010 packet.
 *
 * A value class, not a data class — so it does not itself trigger ADR-049 rule (ii)
 * (Domain `data class` `toString()` redaction).
 */
@JvmInline
value class ConfirmationId(val value: String) {
    companion object {
        /** UUID-backed factory (mirrors the file-02 identity convention; opaque, non-PII). */
        fun random(): ConfirmationId = ConfirmationId(UUID.randomUUID().toString())
    }
}
