package io.nemopill.scheduling.application

import com.google.common.truth.Truth.assertThat
import io.nemopill.core.id.DoseId
import io.nemopill.core.result.Result
import io.nemopill.scheduling.DEMO_DOSE_ID
import io.nemopill.scheduling.FakeClock
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Duration
import java.time.Instant
import kotlin.coroutines.cancellation.CancellationException

/**
 * AC-002 / AC-004 — Application-layer unit test for [ScheduleDemoReminderUseCase].
 *
 * T-012 amends the use case to **persist the demo Reminder's target** to [PendingReminderStore]
 * after a successful `scheduleReminder` (so the boot re-arm leg has a durable target — alarms die
 * on reboot). These tests pin that persist-after-schedule behavior and the store-write failure
 * mapping, alongside the unchanged T-008 `now + 10 min` / idempotency / `CancellationException`-
 * rethrow regression guards.
 */
class ScheduleDemoReminderUseCaseTest {
    private val now = Instant.parse("2026-06-05T12:00:00Z")
    private val expected = now.plus(Duration.ofMinutes(10))

    @Test
    fun `schedules the demo reminder ten minutes from now and returns Ok`() =
        runTest {
            val scheduler = RecordingSchedulerPort()
            val useCase = ScheduleDemoReminderUseCase(FakeClock(now), scheduler, FakePendingReminderStore())

            val result = useCase()

            assertThat(result).isEqualTo(Result.Ok(expected))
            assertThat(scheduler.scheduleCallCount).isEqualTo(1)
            assertThat(scheduler.activeRegistrations).containsExactly(DEMO_DOSE_ID to expected)
        }

    @Test
    fun `persists the scheduled target to the pending-reminder store on success`() =
        runTest {
            val store = FakePendingReminderStore()
            val useCase = ScheduleDemoReminderUseCase(FakeClock(now), RecordingSchedulerPort(), store)

            val result = useCase()

            // The persisted target is exactly the Instant returned in Result.Ok (the re-arm leg
            // re-reads this on BOOT_COMPLETED — AC-002 / AC-005).
            assertThat(result).isEqualTo(Result.Ok(expected))
            assertThat(store.saveCallCount).isEqualTo(1)
            assertThat(store.pendingReminders()).containsExactly(DEMO_DOSE_ID, expected)
        }

    @Test
    fun `second invocation is idempotent - a single logical registration and one persisted target`() =
        runTest {
            val scheduler = RecordingSchedulerPort()
            val store = FakePendingReminderStore()
            val useCase = ScheduleDemoReminderUseCase(FakeClock(now), scheduler, store)

            useCase()
            useCase()

            assertThat(scheduler.scheduleCallCount).isEqualTo(2)
            assertThat(scheduler.activeRegistrations).containsExactly(DEMO_DOSE_ID to expected)
            assertThat(store.pendingReminders()).containsExactly(DEMO_DOSE_ID, expected)
        }

    @Test
    fun `maps an adapter-boundary failure to Result Err Unexpected`() =
        runTest {
            val useCase = ScheduleDemoReminderUseCase(FakeClock(now), ThrowingSchedulerPort(), FakePendingReminderStore())

            val result = useCase()

            assertThat(result).isInstanceOf(Result.Err.Unexpected::class.java)
        }

    @Test
    fun `maps a store-write failure to Result Err Unexpected`() =
        runTest {
            val useCase = ScheduleDemoReminderUseCase(FakeClock(now), RecordingSchedulerPort(), ThrowingPendingReminderStore())

            val result = useCase()

            assertThat(result).isInstanceOf(Result.Err.Unexpected::class.java)
        }

    @Test
    fun `rethrows CancellationException rather than mapping it to an error`() =
        runTest {
            val useCase = ScheduleDemoReminderUseCase(FakeClock(now), CancellingSchedulerPort(), FakePendingReminderStore())

            var rethrown = false
            try {
                useCase()
            } catch (ce: CancellationException) {
                rethrown = true
            }

            assertThat(rethrown).isTrue()
        }
}

/** [PendingReminderStore] whose `savePendingReminder` throws, exercising the store-failure tier. */
private class ThrowingPendingReminderStore : PendingReminderStore {
    override suspend fun savePendingReminder(
        doseId: DoseId,
        at: Instant,
    ): Unit = throw IllegalStateException("simulated store-write failure")

    override suspend fun pendingReminders(): Map<DoseId, Instant> = emptyMap()

    override suspend fun clear(doseId: DoseId) = Unit
}

/** [SchedulerPort] that throws [CancellationException], proving the use case rethrows rather than maps. */
private class CancellingSchedulerPort : SchedulerPort {
    override suspend fun scheduleReminder(
        doseId: DoseId,
        at: Instant,
    ): Unit = throw CancellationException("simulated structured-concurrency cancellation")

    override suspend fun cancel(doseId: DoseId) = Unit

    override suspend fun cancelAll() = Unit
}
