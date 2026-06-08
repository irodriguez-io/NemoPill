package io.nemopill.adherencetracking.infrastructure

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import io.nemopill.core.confirm.ConfirmationSource
import io.nemopill.core.confirm.ConfirmationStatus
import java.time.Instant

/**
 * Room row for a `Confirmation` (file 04 § Local persistence). The **unique index on `doseId`**
 * enforces the 1:1-with-Dose invariant and powers the idempotent insert-or-replace upsert
 * (file 04 § ConfirmationRepository; file 06 § Idempotency rule).
 *
 * Columns map the Domain fields one-to-one; `Instant` and the two enums are persisted via the
 * [ConfirmationConverters] `TypeConverter`s registered on `NemoPillDatabase`. An infrastructure
 * adapter detail — no Domain / Application code references it (enforced by the
 * `NoUpwardLayerDependencyRule` Konsist check).
 */
@Entity(
    tableName = "confirmation",
    indices = [Index(value = ["doseId"], unique = true)],
)
data class ConfirmationEntity(
    @PrimaryKey val confirmationId: String,
    val doseId: String,
    val status: ConfirmationStatus,
    val confirmedAt: Instant,
    val source: ConfirmationSource,
)
