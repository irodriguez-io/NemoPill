package io.nemopill.app.presentation

import com.google.common.truth.Truth.assertThat
import io.nemopill.adherencetracking.application.ConfirmationRepository
import io.nemopill.adherencetracking.application.ObserveConfirmedDoseCountUseCase
import io.nemopill.adherencetracking.domain.Confirmation
import io.nemopill.core.id.DoseId
import io.nemopill.core.port.ClockPort
import io.nemopill.core.result.Result
import io.nemopill.scheduling.application.PendingReminderStore
import io.nemopill.scheduling.application.ScheduleDemoReminderUseCase
import io.nemopill.scheduling.application.SchedulerPort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * AC-004 — the M-002 Done-When item (5) `StateFlow` proof for [DemoViewModel]. Asserts the second
 * `StateFlow`, `confirmedCount`, starts at the `stateIn` initial value `0` and **recomposes to 1**
 * when the observed Taken-count `Flow` emits `1`, and that the T-008 scheduling leg
 * (`uiState`/`schedule()`) is unchanged (a regression guard).
 *
 * Pure-JVM (no Robolectric): `Dispatchers.setMain(UnconfinedTestDispatcher())` backs
 * `viewModelScope` so `stateIn(WhileSubscribed)` collects eagerly while a subscriber is active.
 * The `confirmedCount` `Flow` is driven deterministically through a fake [ConfirmationRepository]
 * and read via `.value` after activating the sharing — **no Turbine** (QA note 1). The Compose
 * recomposition that this `StateFlow` feeds is proven separately by `DemoScreenCounterRobolectricTest`
 * (AC-005) + `MainActivity`'s `collectAsState`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DemoViewModelTest {
    private val mainDispatcher = UnconfinedTestDispatcher()
    private val now = Instant.parse("2026-06-10T09:00:00Z")

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(repository: ConfirmationRepository): DemoViewModel =
        DemoViewModel(
            ScheduleDemoReminderUseCase(FixedClock(now), NoOpScheduler(), NoOpPendingReminderStore()),
            ObserveConfirmedDoseCountUseCase(repository),
        )

    @Test
    fun `confirmedCount starts at 0 - the stateIn initial value`() =
        runTest {
            val viewModel = viewModel(FakeCountRepository())

            assertThat(viewModel.confirmedCount.value).isEqualTo(0)
        }

    @Test
    fun `confirmedCount recomposes to 1 when the observed Taken count emits 1`() =
        runTest {
            val repository = FakeCountRepository()
            val viewModel = viewModel(repository)

            // stateIn(WhileSubscribed) only collects upstream while subscribed — activate it.
            backgroundScope.launch(mainDispatcher) { viewModel.confirmedCount.collect {} }
            assertThat(viewModel.confirmedCount.value).isEqualTo(0)

            repository.count.value = 1
            advanceUntilIdle()

            assertThat(viewModel.confirmedCount.value).isEqualTo(1)
        }

    @Test
    fun `schedule() still moves uiState to Scheduled on success - scheduling leg unchanged`() =
        runTest {
            val viewModel = viewModel(FakeCountRepository())
            assertThat(viewModel.uiState.value).isEqualTo(DemoUiState.Idle)

            viewModel.schedule()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value).isInstanceOf(DemoUiState.Scheduled::class.java)
        }

    /** Fake [ConfirmationRepository] with a controllable Taken-count `Flow`; the write path is a no-op. */
    private class FakeCountRepository : ConfirmationRepository {
        val count = MutableStateFlow(0)

        override suspend fun recordFromNotification(confirmation: Confirmation): Result<Unit, Result.Err> = Result.Ok(Unit)

        override fun observeConfirmedDoseCount(): Flow<Int> = count
    }

    private class NoOpScheduler : SchedulerPort {
        override suspend fun scheduleReminder(
            doseId: DoseId,
            at: Instant,
        ) = Unit

        override suspend fun cancel(doseId: DoseId) = Unit

        override suspend fun cancelAll() = Unit
    }

    /** T-012: the scheduling use case now persists the target; the demo-screen tests don't assert it. */
    private class NoOpPendingReminderStore : PendingReminderStore {
        override suspend fun savePendingReminder(
            doseId: DoseId,
            at: Instant,
        ) = Unit

        override suspend fun pendingReminders(): Map<DoseId, Instant> = emptyMap()

        override suspend fun clear(doseId: DoseId) = Unit
    }

    private class FixedClock(private val instant: Instant) : ClockPort {
        override fun now(): Instant = instant
    }
}
