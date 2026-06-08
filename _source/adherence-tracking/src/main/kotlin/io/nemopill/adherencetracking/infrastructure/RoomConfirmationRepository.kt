package io.nemopill.adherencetracking.infrastructure

import androidx.room.RoomDatabase
import androidx.room.withTransaction
import io.nemopill.adherencetracking.application.ConfirmationRepository
import io.nemopill.adherencetracking.domain.Confirmation
import io.nemopill.core.result.Result
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/** Static, non-PII failure message for the unexpected-failure tier (ADR-031, ADR-049 rule (i)). */
private const val UNEXPECTED_PERSIST_FAILURE = "Failed to persist the confirmation"

/**
 * Room-backed [ConfirmationRepository] (file 04 § ConfirmationRepository). Wraps the DAO write in
 * `db.withTransaction { }` — the single-transaction unit (file 06 § Ordering rule). In T-010 the
 * block writes only the `Confirmation` row (there is no persisted `Dose` to co-update in M-002);
 * the block is still used so the M-004 / M-005 `Dose.status` co-update drops in without
 * restructuring (the demo-vs-full-F-006 gap — T-010 ADR).
 *
 * Takes the base [RoomDatabase] (not the concrete `NemoPillDatabase`, which lives in `:app` and
 * cannot be imported here — `:adherence-tracking` may not depend on `:app`) plus its
 * [ConfirmationDao]; `:app` DI provides both from the single `NemoPillDatabase` instance (T-010
 * ADR — base-type injection keeps the feature module independent of the `@Database` aggregate).
 *
 * The unique `doseId` index makes the upsert idempotent (file 06 § Idempotency rule). Room
 * exceptions are caught at the boundary and mapped to [Result.Err.Unexpected] with a static,
 * non-PII message (file 04 § Error handling); the `CancellationException` rethrow keeps structured
 * concurrency correct.
 */
class RoomConfirmationRepository
    @Inject
    constructor(
        private val db: RoomDatabase,
        private val dao: ConfirmationDao,
    ) : ConfirmationRepository {
        override suspend fun recordFromNotification(confirmation: Confirmation): Result<Unit, Result.Err> =
            try {
                db.withTransaction {
                    dao.upsert(
                        ConfirmationEntity(
                            confirmationId = confirmation.confirmationId.value,
                            doseId = confirmation.doseId.value,
                            status = confirmation.status,
                            confirmedAt = confirmation.confirmedAt,
                            source = confirmation.source,
                        ),
                    )
                }
                Result.Ok(Unit)
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                Result.Err.Unexpected(UNEXPECTED_PERSIST_FAILURE)
            }
    }
