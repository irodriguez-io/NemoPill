package io.nemopill.adherencetracking.application

import io.nemopill.adherencetracking.domain.Confirmation
import io.nemopill.core.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Test [ConfirmationRepository] for the use-case unit tests. Records calls and models the port's
 * **1:1-on-`doseId` idempotency** (file 06 § Idempotency rule) by keying stored confirmations on
 * `doseId` — a repeated record for the same Dose leaves a single logical entry.
 *
 * [failWith] simulates an adapter-boundary exception (exercises the use-case
 * [Result.Err.Unexpected] mapping and the `CancellationException` rethrow); [resultErr] simulates
 * the repository returning a typed failure (the transaction-failure path) without throwing.
 *
 * T-011 adds the **observe leg**: [observeConfirmedDoseCount] returns a controllable
 * [MutableStateFlow] so the use-case unit test (AC-002) can drive the Taken-count `Flow`
 * deterministically (`flow.first()`-after-write per the gatekeeper's chosen approach — no Turbine).
 * [setConfirmedCount] pushes a new value to collectors.
 */
class FakeConfirmationRepository(
    private val failWith: Throwable? = null,
    private val resultErr: Result.Err? = null,
) : ConfirmationRepository {
    val storedByDoseId = linkedMapOf<String, Confirmation>()
    var recordCallCount = 0
        private set

    private val confirmedCount = MutableStateFlow(0)

    /** Drives the observed Taken count (the value collectors of [observeConfirmedDoseCount] see). */
    fun setConfirmedCount(value: Int) {
        confirmedCount.value = value
    }

    override suspend fun recordFromNotification(confirmation: Confirmation): Result<Unit, Result.Err> {
        recordCallCount++
        failWith?.let { throw it }
        resultErr?.let { return it }
        storedByDoseId[confirmation.doseId.value] = confirmation
        return Result.Ok(Unit)
    }

    override fun observeConfirmedDoseCount(): Flow<Int> = confirmedCount.asStateFlow()
}
