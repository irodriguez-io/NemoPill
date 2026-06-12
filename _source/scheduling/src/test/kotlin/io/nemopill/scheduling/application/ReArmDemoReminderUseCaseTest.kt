package io.nemopill.scheduling.application

import com.google.common.truth.Truth.assertThat
import io.nemopill.core.result.Result
import io.nemopill.scheduling.DEMO_DOSE_ID
import io.nemopill.scheduling.FakeClock
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Duration
import java.time.Instant

/**
 * AC-002 — Application-layer unit test for [ReArmDemoReminderUseCase], the T-012 boot re-arm use
 * case. Drives the use case with a [FakePendingReminderStore], a [RecordingSchedulerPort], and a
 * controllable [FakeClock] (file 04 § ClockPort). Covers the four required cases:
 *  (a) a **future** persisted target re-arms exactly once with the persisted [Instant];
 *  (b) an **empty** store re-arms nothing;
 *  (c) a **past** target (`target <= now`) re-arms nothing — the F-008 missed-sweep is M-004;
 *  (d) the use case is **idempotent** — two successive invokes re-arm the same target and never
 *      throw (BR-004 idempotent-by-`DoseId`; safe to invoke on every boot).
 */
class ReArmDemoReminderUseCaseTest {
    private val now = Instant.parse("2026-06-05T12:00:00Z")
    private val futureTarget = now.plus(Duration.ofMinutes(7))
    private val pastTarget = now.minus(Duration.ofMinutes(7))

    @Test
    fun `re-arms a future persisted target exactly once at the persisted time`() =
        runTest {
            val store = FakePendingReminderStore()
            store.savePendingReminder(DEMO_DOSE_ID, futureTarget)
            val scheduler = RecordingSchedulerPort()
            val useCase = ReArmDemoReminderUseCase(FakeClock(now), scheduler, store)

            val result = useCase()

            assertThat(result).isEqualTo(Result.Ok(Unit))
            assertThat(scheduler.scheduleCallCount).isEqualTo(1)
            assertThat(scheduler.activeRegistrations).containsExactly(DEMO_DOSE_ID to futureTarget)
        }

    @Test
    fun `re-arms nothing when the store is empty`() =
        runTest {
            val scheduler = RecordingSchedulerPort()
            val useCase = ReArmDemoReminderUseCase(FakeClock(now), scheduler, FakePendingReminderStore())

            val result = useCase()

            assertThat(result).isEqualTo(Result.Ok(Unit))
            assertThat(scheduler.scheduleCallCount).isEqualTo(0)
            assertThat(scheduler.activeRegistrations).isEmpty()
        }

    @Test
    fun `does not re-arm a past-due target - the missed-sweep is M-004`() =
        runTest {
            val store = FakePendingReminderStore()
            store.savePendingReminder(DEMO_DOSE_ID, pastTarget)
            val scheduler = RecordingSchedulerPort()
            val useCase = ReArmDemoReminderUseCase(FakeClock(now), scheduler, store)

            val result = useCase()

            assertThat(result).isEqualTo(Result.Ok(Unit))
            assertThat(scheduler.scheduleCallCount).isEqualTo(0)
            assertThat(scheduler.activeRegistrations).isEmpty()
        }

    @Test
    fun `does not re-arm a target exactly equal to now - boundary is strictly future`() =
        runTest {
            val store = FakePendingReminderStore()
            store.savePendingReminder(DEMO_DOSE_ID, now)
            val scheduler = RecordingSchedulerPort()
            val useCase = ReArmDemoReminderUseCase(FakeClock(now), scheduler, store)

            useCase()

            assertThat(scheduler.scheduleCallCount).isEqualTo(0)
        }

    @Test
    fun `is idempotent - two successive invocations re-arm the same target without throwing`() =
        runTest {
            val store = FakePendingReminderStore()
            store.savePendingReminder(DEMO_DOSE_ID, futureTarget)
            val scheduler = RecordingSchedulerPort()
            val useCase = ReArmDemoReminderUseCase(FakeClock(now), scheduler, store)

            useCase()
            useCase()

            assertThat(scheduler.scheduleCallCount).isEqualTo(2)
            assertThat(scheduler.activeRegistrations).containsExactly(DEMO_DOSE_ID to futureTarget)
        }

    @Test
    fun `maps an adapter-boundary failure to Result Err Unexpected`() =
        runTest {
            val store = FakePendingReminderStore()
            store.savePendingReminder(DEMO_DOSE_ID, futureTarget)
            val useCase = ReArmDemoReminderUseCase(FakeClock(now), ThrowingSchedulerPort(), store)

            val result = useCase()

            assertThat(result).isInstanceOf(Result.Err.Unexpected::class.java)
        }
}
