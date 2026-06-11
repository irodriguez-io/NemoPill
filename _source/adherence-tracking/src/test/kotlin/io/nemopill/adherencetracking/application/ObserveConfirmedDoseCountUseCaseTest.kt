package io.nemopill.adherencetracking.application

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * AC-002 — Application-layer unit test for [ObserveConfirmedDoseCountUseCase], the
 * `:adherence-tracking` module's first **query** use case (M-002 Done-When item (5), the observe
 * leg). Drives a [FakeConfirmationRepository] backed by a `MutableStateFlow<Int>` and asserts the
 * use case is a faithful pass-through over the repository's Taken-count `Flow` (it adds no
 * transformation). Per the gatekeeper's chosen approach, the `Flow` is read with
 * `flow.first()`-after-write — **no Turbine** (QA note 1).
 */
class ObserveConfirmedDoseCountUseCaseTest {
    @Test
    fun `invoke emits the repository's current count`() =
        runTest {
            val repository = FakeConfirmationRepository()
            val useCase = ObserveConfirmedDoseCountUseCase(repository)

            assertThat(useCase().first()).isEqualTo(0)
        }

    @Test
    fun `emitting 0 then 1 from the repository propagates 0 then 1 through the use case`() =
        runTest {
            val repository = FakeConfirmationRepository()
            val useCase = ObserveConfirmedDoseCountUseCase(repository)

            assertThat(useCase().first()).isEqualTo(0)

            repository.setConfirmedCount(1)

            assertThat(useCase().first()).isEqualTo(1)
        }

    @Test
    fun `the use case adds no transformation - it relays each repository value verbatim`() =
        runTest {
            val repository = FakeConfirmationRepository()
            val useCase = ObserveConfirmedDoseCountUseCase(repository)

            repository.setConfirmedCount(3)
            assertThat(useCase().first()).isEqualTo(3)

            repository.setConfirmedCount(0)
            assertThat(useCase().first()).isEqualTo(0)

            repository.setConfirmedCount(7)
            assertThat(useCase().first()).isEqualTo(7)
        }
}
