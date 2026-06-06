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

/** AC-002 — Application-layer unit test for [ScheduleDemoReminderUseCase]. */
class ScheduleDemoReminderUseCaseTest {
    private val now = Instant.parse("2026-06-05T12:00:00Z")
    private val expected = now.plus(Duration.ofMinutes(10))

    @Test
    fun `schedules the demo reminder ten minutes from now and returns Ok`() =
        runTest {
            val scheduler = RecordingSchedulerPort()
            val useCase = ScheduleDemoReminderUseCase(FakeClock(now), scheduler)

            val result = useCase()

            assertThat(result).isEqualTo(Result.Ok(expected))
            assertThat(scheduler.scheduleCallCount).isEqualTo(1)
            assertThat(scheduler.activeRegistrations).containsExactly(DEMO_DOSE_ID to expected)
        }

    @Test
    fun `second invocation is idempotent - a single logical registration`() =
        runTest {
            val scheduler = RecordingSchedulerPort()
            val useCase = ScheduleDemoReminderUseCase(FakeClock(now), scheduler)

            useCase()
            useCase()

            assertThat(scheduler.scheduleCallCount).isEqualTo(2)
            assertThat(scheduler.activeRegistrations).containsExactly(DEMO_DOSE_ID to expected)
        }

    @Test
    fun `maps an adapter-boundary failure to Result Err Unexpected`() =
        runTest {
            val useCase = ScheduleDemoReminderUseCase(FakeClock(now), ThrowingSchedulerPort())

            val result = useCase()

            assertThat(result).isInstanceOf(Result.Err.Unexpected::class.java)
        }
}

/** Fake [SchedulerPort] that records calls and models the port's idempotent-by-`doseId` contract. */
private class RecordingSchedulerPort : SchedulerPort {
    var scheduleCallCount = 0
        private set
    val activeRegistrations = mutableListOf<Pair<DoseId, Instant>>()

    override suspend fun scheduleReminder(
        doseId: DoseId,
        at: Instant,
    ) {
        scheduleCallCount++
        activeRegistrations.removeAll { it.first == doseId }
        activeRegistrations.add(doseId to at)
    }

    override suspend fun cancel(doseId: DoseId) {
        activeRegistrations.removeAll { it.first == doseId }
    }

    override suspend fun cancelAll() {
        activeRegistrations.clear()
    }
}

/** Fake [SchedulerPort] whose `scheduleReminder` throws, to exercise the unexpected-failure tier. */
private class ThrowingSchedulerPort : SchedulerPort {
    override suspend fun scheduleReminder(
        doseId: DoseId,
        at: Instant,
    ): Unit = throw IllegalStateException("simulated adapter failure")

    override suspend fun cancel(doseId: DoseId) = Unit

    override suspend fun cancelAll() = Unit
}
