package io.nemopill.adherencetracking.application

import io.nemopill.adherencetracking.domain.Confirmation
import io.nemopill.core.result.Result
import kotlinx.coroutines.flow.Flow

/**
 * Outbound port for the [Confirmation] read/write surface (file 04 § ConfirmationRepository). T-010
 * surfaced only the confirm-from-notification write; T-011 adds the first **read/observe** method.
 * The full UC-005 / UC-006 / UC-007 (in-app, retroactive, correction) write surface and the
 * BR-008 Adherence read surface are M-005.
 *
 * **Write — [recordFromNotification]:** 1:1 unique on `doseId`, idempotent on `doseId` (file 04 §
 * ConfirmationRepository; file 06 § Idempotency rule — "1:1 unique on `doseId` … safe against
 * double-tap"). A repeated confirm for the same Dose leaves exactly one logical row. Returns the
 * two-tier [Result] (file 04 § Error handling): adapter-boundary exceptions are caught and mapped
 * to [Result.Err.Unexpected] with a static, non-PII message (ADR-031 / ADR-049 rule (i)).
 *
 * **Read — [observeConfirmedDoseCount]:** a cold [Flow] of the count of **Taken** confirmations,
 * re-emitting whenever the `confirmation` table changes (the Room observable-query contract). A SQL
 * `COUNT` is inherently idempotent, so there is no replay/dedupe concern. The read path surfaces
 * query errors to the collector rather than mapping to [Result] — a deliberate choice for the
 * walking-skeleton observe leg (T-011 ADR); the collecting ViewModel runs it in a supervised scope.
 * **This is the demo "doses Taken" tally (M-002 item (5)), not the BR-008 Adherence percentage (M-005).**
 */
interface ConfirmationRepository {
    suspend fun recordFromNotification(confirmation: Confirmation): Result<Unit, Result.Err>

    fun observeConfirmedDoseCount(): Flow<Int>
}
