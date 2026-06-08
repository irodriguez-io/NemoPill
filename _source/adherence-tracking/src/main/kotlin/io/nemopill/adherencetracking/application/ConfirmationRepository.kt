package io.nemopill.adherencetracking.application

import io.nemopill.adherencetracking.domain.Confirmation
import io.nemopill.core.result.Result

/**
 * Outbound port for persisting a [Confirmation] (file 04 § ConfirmationRepository). T-010 surfaces
 * only the confirm-from-notification write; the full UC-005 / UC-006 / UC-007 (in-app, retroactive,
 * correction) surface is M-005.
 *
 * **1:1 unique on `doseId`, idempotent on `doseId`** (file 04 § ConfirmationRepository; file 06 §
 * Idempotency rule — "`ConfirmationRepository` is 1:1 unique on `doseId` … safe against
 * double-tap"). A repeated confirm for the same Dose leaves exactly one logical row.
 *
 * Returns the two-tier [Result] (file 04 § Error handling): adapter-boundary exceptions are caught
 * and mapped to [Result.Err.Unexpected] with a static, non-PII message (ADR-031 / ADR-049 rule (i)).
 */
interface ConfirmationRepository {
    suspend fun recordFromNotification(confirmation: Confirmation): Result<Unit, Result.Err>
}
