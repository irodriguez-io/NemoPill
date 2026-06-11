package io.nemopill.adherencetracking.infrastructure

import androidx.room.Room
import com.google.common.truth.Truth.assertThat
import io.nemopill.adherencetracking.domain.Confirmation
import io.nemopill.core.confirm.ConfirmationSource
import io.nemopill.core.confirm.ConfirmationStatus
import io.nemopill.core.id.ConfirmationId
import io.nemopill.core.id.DoseId
import io.nemopill.core.result.Result
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.Instant

/**
 * AC-003 — Room integration test for [RoomConfirmationRepository] against an in-memory
 * [TestConfirmationDatabase] (Robolectric). Asserts a real `Confirmation` write is readable by
 * `doseId`, the unique-index 1:1 idempotency on a repeated write, and that `Instant` + both enums
 * round-trip through the [ConfirmationConverters].
 *
 * T-011 adds the **read/observe leg**: [RoomConfirmationRepository.observeConfirmedDoseCount]
 * surfaces a Room `Flow<Int>` counting Taken confirmations. The observe cases assert the count is
 * `0` on an empty table, becomes `1` after a `TAKEN` write, returns to `0` when the same `doseId`
 * is rewritten `SKIPPED` (the T-010 1:1 REPLACE idempotency — still one row), and back to `1` on a
 * subsequent `TAKEN`. The `Flow` is read with `flow.first()`-after-write (no Turbine — QA note 1);
 * each `first()` re-queries Room, proving the count tracks the persisted table state and the
 * `status` enum round-trips through the converter in the `WHERE` predicate.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
class RoomConfirmationRepositoryRobolectricTest {
    private val doseId = DoseId("demo-dose")
    private val now = Instant.parse("2026-06-07T12:10:00Z")

    private lateinit var db: TestConfirmationDatabase
    private lateinit var dao: ConfirmationDao
    private lateinit var repository: RoomConfirmationRepository

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        db =
            Room
                .inMemoryDatabaseBuilder(context, TestConfirmationDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        dao = db.confirmationDao()
        repository = RoomConfirmationRepository(db, dao)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun confirmation(
        status: ConfirmationStatus = ConfirmationStatus.TAKEN,
        id: String = "c-1",
        at: Instant = now,
    ) = Confirmation(
        confirmationId = ConfirmationId(id),
        doseId = doseId,
        status = status,
        confirmedAt = at,
        source = ConfirmationSource.NOTIFICATION_ACTION,
    )

    @Test
    fun `recordFromNotification persists one row readable by doseId`() =
        runTest {
            val result = repository.recordFromNotification(confirmation())

            assertThat(result).isEqualTo(Result.Ok(Unit))
            assertThat(dao.count()).isEqualTo(1)
            val row = dao.findByDoseId(doseId.value)
            assertThat(row).isNotNull()
            assertThat(row!!.confirmationId).isEqualTo("c-1")
            assertThat(row.doseId).isEqualTo("demo-dose")
        }

    @Test
    fun `a second write for the same doseId leaves exactly one row (unique-index idempotency)`() =
        runTest {
            repository.recordFromNotification(confirmation(status = ConfirmationStatus.TAKEN, id = "c-1"))
            repository.recordFromNotification(confirmation(status = ConfirmationStatus.SKIPPED, id = "c-2"))

            assertThat(dao.count()).isEqualTo(1)
            val row = dao.findByDoseId(doseId.value)
            assertThat(row!!.status).isEqualTo(ConfirmationStatus.SKIPPED)
            assertThat(row.confirmationId).isEqualTo("c-2")
        }

    @Test
    fun `Instant and both enums round-trip through the type converters`() =
        runTest {
            val at = Instant.parse("2026-06-07T08:30:15Z")
            repository.recordFromNotification(confirmation(status = ConfirmationStatus.SKIPPED, at = at))

            val row = dao.findByDoseId(doseId.value)!!
            assertThat(row.confirmedAt).isEqualTo(at)
            assertThat(row.status).isEqualTo(ConfirmationStatus.SKIPPED)
            assertThat(row.source).isEqualTo(ConfirmationSource.NOTIFICATION_ACTION)
        }

    @Test
    fun `observeConfirmedDoseCount emits 0 on an empty table`() =
        runTest {
            assertThat(repository.observeConfirmedDoseCount().first()).isEqualTo(0)
        }

    @Test
    fun `Taken count tracks the table - TAKEN then SKIPPED then TAKEN for the same dose moves it 1 to 0 to 1`() =
        runTest {
            assertThat(repository.observeConfirmedDoseCount().first()).isEqualTo(0)

            repository.recordFromNotification(confirmation(status = ConfirmationStatus.TAKEN, id = "c-1"))
            assertThat(repository.observeConfirmedDoseCount().first()).isEqualTo(1)

            // Same doseId rewritten SKIPPED — the unique-index REPLACE keeps one row, now non-Taken.
            repository.recordFromNotification(confirmation(status = ConfirmationStatus.SKIPPED, id = "c-2"))
            assertThat(dao.count()).isEqualTo(1)
            assertThat(repository.observeConfirmedDoseCount().first()).isEqualTo(0)

            // A subsequent TAKEN for the same dose flips the Taken count back to 1.
            repository.recordFromNotification(confirmation(status = ConfirmationStatus.TAKEN, id = "c-3"))
            assertThat(dao.count()).isEqualTo(1)
            assertThat(repository.observeConfirmedDoseCount().first()).isEqualTo(1)
        }
}
