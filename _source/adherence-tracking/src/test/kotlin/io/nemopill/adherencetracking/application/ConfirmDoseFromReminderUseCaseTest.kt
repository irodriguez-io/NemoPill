package io.nemopill.adherencetracking.application

import com.google.common.truth.Truth.assertThat
import io.nemopill.adherencetracking.FakeClock
import io.nemopill.core.confirm.ConfirmationSource
import io.nemopill.core.confirm.ConfirmationStatus
import io.nemopill.core.event.DomainEvent
import io.nemopill.core.event.DomainEventPublisher
import io.nemopill.core.event.DoseConfirmed
import io.nemopill.core.id.DoseId
import io.nemopill.core.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant
import kotlin.coroutines.cancellation.CancellationException

/** AC-002 — Application-layer unit test for [ConfirmDoseFromReminderUseCase] (UC-004). */
class ConfirmDoseFromReminderUseCaseTest {
    private val now = Instant.parse("2026-06-07T12:10:00Z")
    private val doseId = DoseId("demo-dose")

    @Test
    fun `a TAKEN confirm records one Confirmation with NOTIFICATION_ACTION source and confirmedAt now`() =
        runTest {
            val repository = FakeConfirmationRepository()
            val publisher = RecordingEventPublisher()
            val useCase = ConfirmDoseFromReminderUseCase(repository, FakeClock(now), publisher)

            val result = useCase(doseId, ConfirmationStatus.TAKEN)

            assertThat(result).isEqualTo(Result.Ok(Unit))
            assertThat(repository.recordCallCount).isEqualTo(1)
            val stored = repository.storedByDoseId.getValue(doseId.value)
            assertThat(stored.status).isEqualTo(ConfirmationStatus.TAKEN)
            assertThat(stored.source).isEqualTo(ConfirmationSource.NOTIFICATION_ACTION)
            assertThat(stored.confirmedAt).isEqualTo(now)
        }

    @Test
    fun `a SKIPPED confirm records status SKIPPED`() =
        runTest {
            val repository = FakeConfirmationRepository()
            val useCase = ConfirmDoseFromReminderUseCase(repository, FakeClock(now), RecordingEventPublisher())

            val result = useCase(doseId, ConfirmationStatus.SKIPPED)

            assertThat(result).isEqualTo(Result.Ok(Unit))
            assertThat(repository.storedByDoseId.getValue(doseId.value).status)
                .isEqualTo(ConfirmationStatus.SKIPPED)
        }

    @Test
    fun `a repeated confirm for the same dose stays a single logical Confirmation (idempotent)`() =
        runTest {
            val repository = FakeConfirmationRepository()
            val useCase = ConfirmDoseFromReminderUseCase(repository, FakeClock(now), RecordingEventPublisher())

            useCase(doseId, ConfirmationStatus.TAKEN)
            useCase(doseId, ConfirmationStatus.TAKEN)

            assertThat(repository.recordCallCount).isEqualTo(2)
            assertThat(repository.storedByDoseId).hasSize(1)
        }

    @Test
    fun `publishes a DoseConfirmed on success carrying the doseId, status, source and confirmedAt`() =
        runTest {
            val repository = FakeConfirmationRepository()
            val publisher = RecordingEventPublisher()
            val useCase = ConfirmDoseFromReminderUseCase(repository, FakeClock(now), publisher)

            useCase(doseId, ConfirmationStatus.TAKEN)

            assertThat(publisher.published).hasSize(1)
            val event = publisher.published.single() as DoseConfirmed
            assertThat(event.doseId).isEqualTo(doseId)
            assertThat(event.status).isEqualTo(ConfirmationStatus.TAKEN)
            assertThat(event.source).isEqualTo(ConfirmationSource.NOTIFICATION_ACTION)
            assertThat(event.confirmedAt).isEqualTo(now)
            assertThat(event.confirmationId).isEqualTo(repository.storedByDoseId.getValue(doseId.value).confirmationId)
        }

    @Test
    fun `a transaction-failure Err is returned and no DoseConfirmed is published`() =
        runTest {
            val repository = FakeConfirmationRepository(resultErr = Result.Err.Unexpected("db down"))
            val publisher = RecordingEventPublisher()
            val useCase = ConfirmDoseFromReminderUseCase(repository, FakeClock(now), publisher)

            val result = useCase(doseId, ConfirmationStatus.TAKEN)

            assertThat(result).isEqualTo(Result.Err.Unexpected("db down"))
            assertThat(publisher.published).isEmpty()
        }

    @Test
    fun `maps an adapter-boundary exception to Result Err Unexpected`() =
        runTest {
            val repository = FakeConfirmationRepository(failWith = IllegalStateException("boom"))
            val useCase = ConfirmDoseFromReminderUseCase(repository, FakeClock(now), RecordingEventPublisher())

            val result = useCase(doseId, ConfirmationStatus.TAKEN)

            assertThat(result).isInstanceOf(Result.Err.Unexpected::class.java)
        }

    @Test
    fun `rethrows a CancellationException from the repository (structured concurrency)`() =
        runTest {
            val repository = FakeConfirmationRepository(failWith = CancellationException("cancelled"))
            val useCase = ConfirmDoseFromReminderUseCase(repository, FakeClock(now), RecordingEventPublisher())

            var thrown: Throwable? = null
            try {
                useCase(doseId, ConfirmationStatus.TAKEN)
            } catch (e: CancellationException) {
                thrown = e
            }

            assertThat(thrown).isInstanceOf(CancellationException::class.java)
        }
}

/** Records published [DomainEvent]s for assertion; the read side is unused in these tests. */
private class RecordingEventPublisher : DomainEventPublisher {
    val published = mutableListOf<DomainEvent>()

    override suspend fun publish(event: DomainEvent) {
        published.add(event)
    }

    override val events: Flow<DomainEvent> = emptyFlow()
}
