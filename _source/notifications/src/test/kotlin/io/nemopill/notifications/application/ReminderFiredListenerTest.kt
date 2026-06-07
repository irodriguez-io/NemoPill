package io.nemopill.notifications.application

import com.google.common.truth.Truth.assertThat
import io.nemopill.core.event.InProcessEventBus
import io.nemopill.core.event.ReminderFired
import io.nemopill.core.event.ReminderVariant
import io.nemopill.core.id.DoseId
import io.nemopill.core.result.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant

/**
 * AC-003 — unit test for [ReminderFiredListener] and [PresentReminderUseCase].
 *
 * Drives a real [InProcessEventBus] and a [FakeNotificationPort]: asserts the listener invokes
 * `presentReminder(demoDoseId, ON_TIME)` exactly once, that a replayed/duplicate `ReminderFired`
 * for the same `doseId` stays a single logical presentation (idempotent on the dedupe key), that a
 * `LATE` event is filtered out, and that an adapter-boundary exception maps to
 * `Result.Err.Unexpected`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReminderFiredListenerTest {
    private val demoDoseId = DoseId("demo-dose")
    private val firedAt = Instant.parse("2026-06-05T12:10:00Z")

    @Test
    fun `listener presents an on-time reminder exactly once`() =
        runTest {
            val bus = InProcessEventBus()
            val port = FakeNotificationPort()
            val listener = ReminderFiredListener(bus, PresentReminderUseCase(port))
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { listener.start() }

            bus.publish(ReminderFired(demoDoseId, firedAt, ReminderVariant.ON_TIME))

            assertThat(port.presentCalls)
                .containsExactly(demoDoseId to ReminderVariant.ON_TIME)
        }

    @Test
    fun `a replayed event for the same dose is idempotent`() =
        runTest {
            val bus = InProcessEventBus()
            val port = FakeNotificationPort()
            val listener = ReminderFiredListener(bus, PresentReminderUseCase(port))
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { listener.start() }

            bus.publish(ReminderFired(demoDoseId, firedAt, ReminderVariant.ON_TIME))
            bus.publish(ReminderFired(demoDoseId, firedAt.plusSeconds(1), ReminderVariant.ON_TIME))

            assertThat(port.presentCalls).hasSize(1)
        }

    @Test
    fun `a late variant event is filtered out`() =
        runTest {
            val bus = InProcessEventBus()
            val port = FakeNotificationPort()
            val listener = ReminderFiredListener(bus, PresentReminderUseCase(port))
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { listener.start() }

            bus.publish(ReminderFired(DoseId("late-dose"), firedAt, ReminderVariant.LATE))

            assertThat(port.presentCalls).isEmpty()
        }

    @Test
    fun `use case maps an adapter-boundary failure to Result Err Unexpected`() =
        runTest {
            val useCase = PresentReminderUseCase(FakeNotificationPort(failWith = IllegalStateException("boom")))

            val result = useCase(demoDoseId, ReminderVariant.ON_TIME)

            assertThat(result).isInstanceOf(Result.Err.Unexpected::class.java)
        }

    @Test
    fun `use case returns Ok when the port presents successfully`() =
        runTest {
            val port = FakeNotificationPort()
            val useCase = PresentReminderUseCase(port)

            val result = useCase(demoDoseId, ReminderVariant.ON_TIME)

            assertThat(result).isEqualTo(Result.Ok(Unit))
            assertThat(port.presentCalls).containsExactly(demoDoseId to ReminderVariant.ON_TIME)
        }
}
