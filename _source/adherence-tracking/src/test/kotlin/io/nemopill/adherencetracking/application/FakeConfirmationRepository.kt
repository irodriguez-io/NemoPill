package io.nemopill.adherencetracking.application

import io.nemopill.adherencetracking.domain.Confirmation
import io.nemopill.core.result.Result

/**
 * Test [ConfirmationRepository] for the use-case unit test. Records calls and models the port's
 * **1:1-on-`doseId` idempotency** (file 06 § Idempotency rule) by keying stored confirmations on
 * `doseId` — a repeated record for the same Dose leaves a single logical entry.
 *
 * [failWith] simulates an adapter-boundary exception (exercises the use-case
 * [Result.Err.Unexpected] mapping and the `CancellationException` rethrow); [resultErr] simulates
 * the repository returning a typed failure (the transaction-failure path) without throwing.
 */
class FakeConfirmationRepository(
    private val failWith: Throwable? = null,
    private val resultErr: Result.Err? = null,
) : ConfirmationRepository {
    val storedByDoseId = linkedMapOf<String, Confirmation>()
    var recordCallCount = 0
        private set

    override suspend fun recordFromNotification(confirmation: Confirmation): Result<Unit, Result.Err> {
        recordCallCount++
        failWith?.let { throw it }
        resultErr?.let { return it }
        storedByDoseId[confirmation.doseId.value] = confirmation
        return Result.Ok(Unit)
    }
}
