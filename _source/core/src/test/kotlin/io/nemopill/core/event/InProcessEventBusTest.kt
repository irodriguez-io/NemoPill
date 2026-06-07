package io.nemopill.core.event

import com.google.common.truth.Truth.assertThat
import io.nemopill.core.id.DoseId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant

/**
 * AC-002 — unit test for the [InProcessEventBus] (file 04 § DomainEventPublisher).
 *
 * Covers: a published [ReminderFired] reaches an active subscriber; the replay buffer delivers
 * to a late-registering subscriber (the cold-start ordering case, file 06 § Ordering rule); and
 * the [ReminderFired] shape carries `doseId` / `firedAt` / `variant` correctly.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InProcessEventBusTest {
    private val doseId = DoseId("demo-dose")
    private val firedAt = Instant.parse("2026-06-05T12:10:00Z")
    private val event = ReminderFired(doseId, firedAt, ReminderVariant.ON_TIME)

    @Test
    fun `an active subscriber receives a published event`() =
        runTest {
            val bus = InProcessEventBus()
            val received = mutableListOf<DomainEvent>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                bus.events.collect { received.add(it) }
            }

            bus.publish(event)

            assertThat(received).containsExactly(event)
        }

    @Test
    fun `the replay buffer delivers to a late-registering subscriber`() =
        runTest {
            val bus = InProcessEventBus()

            // Publish before anyone is subscribed — the cold-start race.
            bus.publish(event)

            // A subscriber that registers afterwards still sees the replayed event.
            val replayed = bus.events.first()

            assertThat(replayed).isEqualTo(event)
        }

    @Test
    fun `ReminderFired carries doseId firedAt and variant`() =
        runTest {
            val bus = InProcessEventBus()
            bus.publish(event)

            val replayed = bus.events.first() as ReminderFired

            assertThat(replayed.doseId).isEqualTo(doseId)
            assertThat(replayed.firedAt).isEqualTo(firedAt)
            assertThat(replayed.variant).isEqualTo(ReminderVariant.ON_TIME)
        }

    @Test
    fun `the replay buffer preserves recent events in publish order`() =
        runTest {
            val bus = InProcessEventBus(replay = 4)
            val onTime = ReminderFired(doseId, firedAt, ReminderVariant.ON_TIME)
            val late = ReminderFired(DoseId("other-dose"), firedAt, ReminderVariant.LATE)

            bus.publish(onTime)
            bus.publish(late)

            // take(2) bounds the otherwise-infinite SharedFlow; the replay buffer satisfies it.
            val replayed = bus.events.take(2).toList()

            assertThat(replayed).containsExactly(onTime, late).inOrder()
        }

    @Test
    fun `ReminderVariant declares the BR-010 on-time and late variants`() {
        assertThat(ReminderVariant.entries)
            .containsExactly(ReminderVariant.ON_TIME, ReminderVariant.LATE)
    }
}
