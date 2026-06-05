package io.nemopill.core.id

/**
 * Stable identity of a Dose (file 04 § shared-kernel ID newtypes).
 *
 * A value class, not a data class — so it does not trigger ADR-049 rule (ii)
 * (Domain `data class` `toString()` redaction). Other ID newtypes
 * (`MedicationId`, `DoseScheduleId`, `ConfirmationId`) land with their owning features.
 */
@JvmInline
value class DoseId(val value: String)
