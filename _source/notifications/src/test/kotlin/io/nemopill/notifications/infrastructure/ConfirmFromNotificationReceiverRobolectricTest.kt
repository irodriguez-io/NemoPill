package io.nemopill.notifications.infrastructure

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.room.Room
import com.google.common.truth.Truth.assertThat
import io.nemopill.adherencetracking.application.ConfirmDoseFromReminderUseCase
import io.nemopill.adherencetracking.application.ConfirmationRepository
import io.nemopill.adherencetracking.domain.Confirmation
import io.nemopill.adherencetracking.infrastructure.AdherenceConfirmationGateway
import io.nemopill.adherencetracking.infrastructure.RoomConfirmationRepository
import io.nemopill.core.confirm.ConfirmationSource
import io.nemopill.core.confirm.ConfirmationStatus
import io.nemopill.core.confirm.NotificationConfirmationGateway
import io.nemopill.core.event.InProcessEventBus
import io.nemopill.core.id.DoseId
import io.nemopill.core.port.SystemClockAdapter
import io.nemopill.core.result.Result
import io.nemopill.notifications.ACTION_CONFIRM_FROM_NOTIFICATION
import io.nemopill.notifications.CHANNEL_REMINDER_ON_TIME
import io.nemopill.notifications.EXTRA_DOSE_ID
import io.nemopill.notifications.EXTRA_STATUS
import io.nemopill.notifications.R
import io.nemopill.notifications.STATUS_TAKEN
import io.nemopill.notifications.notificationIdFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNotificationManager
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * AC-004 / AC-004a — the M-002 item (4) end-to-end integration test. Drives the notification-action
 * path below the OS edge: constructs the confirm action `Intent`, delivers it to a real
 * [ConfirmFromNotificationReceiver], and asserts the full receiver → `:core` gateway → use case →
 * `RoomConfirmationRepository` → in-memory Room chain plus the dismiss / not-dismiss behavior and
 * the `goAsync()` `withTimeout` / always-`finish()` lifecycle.
 *
 * The receiver's production seam (`gatewayProvider` = `EntryPointAccessors`) is overridden with a
 * real-Room or failing gateway via the `@VisibleForTesting` hooks; the async outcome is observed
 * through `onOutcomeForTest`, which the receiver invokes in its `finally` **after**
 * `PendingResult.finish()` — so observing an outcome proves `finish()` ran (AC-004a).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
class ConfirmFromNotificationReceiverRobolectricTest {
    private val doseId = "demo-dose"

    private lateinit var context: Context
    private lateinit var db: ReceiverTestConfirmationDatabase
    private lateinit var notificationManager: NotificationManager
    private lateinit var shadowNotificationManager: ShadowNotificationManager
    private val outcomes = LinkedBlockingQueue<Result<Unit, Result.Err>>()

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        db =
            Room
                .inMemoryDatabaseBuilder(context, ReceiverTestConfirmationDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        shadowNotificationManager = shadowOf(notificationManager)
        NotificationChannels.ensureReminderOnTimeChannel(context)

        ConfirmFromNotificationReceiver.onOutcomeForTest = { outcomes.put(it) }
    }

    @After
    fun tearDown() {
        // Reset the static test seams so they cannot leak across tests.
        ConfirmFromNotificationReceiver.gatewayProvider = DEFAULT_GATEWAY_PROVIDER
        ConfirmFromNotificationReceiver.onOutcomeForTest = null
        ConfirmFromNotificationReceiver.confirmTimeoutMillis = DEFAULT_TIMEOUT_MS
        db.close()
    }

    private fun realRoomGateway(): NotificationConfirmationGateway {
        val repository = RoomConfirmationRepository(db, db.confirmationDao())
        val useCase = ConfirmDoseFromReminderUseCase(repository, SystemClockAdapter(), InProcessEventBus())
        return AdherenceConfirmationGateway(useCase)
    }

    private fun confirmIntent(status: String): Intent =
        Intent(ACTION_CONFIRM_FROM_NOTIFICATION).apply {
            putExtra(EXTRA_DOSE_ID, doseId)
            putExtra(EXTRA_STATUS, status)
        }

    private fun postSourceNotification() {
        val notification =
            NotificationCompat
                .Builder(context, CHANNEL_REMINDER_ON_TIME)
                .setSmallIcon(R.drawable.ic_notification_reminder)
                .setContentTitle("reminder")
                .build()
        NotificationManagerCompat.from(context).notify(notificationIdFor(DoseId(doseId)), notification)
    }

    private fun awaitOutcome(): Result<Unit, Result.Err>? = outcomes.poll(AWAIT_SECONDS, TimeUnit.SECONDS)

    @Test
    fun `a TAKEN tap persists one Confirmation and dismisses the source notification`() {
        ConfirmFromNotificationReceiver.gatewayProvider = { realRoomGateway() }
        postSourceNotification()
        assertThat(shadowNotificationManager.allNotifications).hasSize(1)

        ConfirmFromNotificationReceiver().onReceive(context, confirmIntent(STATUS_TAKEN))

        val outcome = awaitOutcome()
        assertThat(outcome).isEqualTo(Result.Ok(Unit))

        val row = runBlocking { db.confirmationDao().findByDoseId(doseId) }
        assertThat(row).isNotNull()
        assertThat(row!!.status).isEqualTo(ConfirmationStatus.TAKEN)
        assertThat(row.source).isEqualTo(ConfirmationSource.NOTIFICATION_ACTION)
        assertThat(runBlocking { db.confirmationDao().count() }).isEqualTo(1)

        // Dismissed on success (file 06 § F-006 step 6).
        assertThat(shadowNotificationManager.allNotifications).isEmpty()
    }

    @Test
    fun `an unknown status yields UnexpectedNotificationPayload, writes no row, and leaves the notification`() {
        ConfirmFromNotificationReceiver.gatewayProvider = { realRoomGateway() }
        postSourceNotification()

        ConfirmFromNotificationReceiver().onReceive(context, confirmIntent("BOGUS"))

        val outcome = awaitOutcome()
        assertThat(outcome).isEqualTo(Result.Err.UnexpectedNotificationPayload)
        assertThat(runBlocking { db.confirmationDao().count() }).isEqualTo(0)
        // Not dismissed — the Patient can retry from in-app (file 06 § Retry rule).
        assertThat(shadowNotificationManager.allNotifications).hasSize(1)
    }

    @Test
    fun `a transaction failure writes no row and leaves the notification (file 06 Retry rule)`() {
        val failingGateway =
            AdherenceConfirmationGateway(
                ConfirmDoseFromReminderUseCase(FailingConfirmationRepository, SystemClockAdapter(), InProcessEventBus()),
            )
        ConfirmFromNotificationReceiver.gatewayProvider = { failingGateway }
        postSourceNotification()

        ConfirmFromNotificationReceiver().onReceive(context, confirmIntent(STATUS_TAKEN))

        val outcome = awaitOutcome()
        assertThat(outcome).isInstanceOf(Result.Err.Unexpected::class.java)
        assertThat(runBlocking { db.confirmationDao().count() }).isEqualTo(0)
        assertThat(shadowNotificationManager.allNotifications).hasSize(1)
    }

    @Test
    fun `a dispatch over the timeout maps to Unexpected, leaves the notification, and still finishes`() {
        ConfirmFromNotificationReceiver.confirmTimeoutMillis = 50L
        ConfirmFromNotificationReceiver.gatewayProvider = {
            object : NotificationConfirmationGateway {
                override suspend fun confirm(
                    doseId: DoseId,
                    status: ConfirmationStatus,
                ): Result<Unit, Result.Err> {
                    delay(1_000)
                    return Result.Ok(Unit)
                }
            }
        }
        postSourceNotification()

        ConfirmFromNotificationReceiver().onReceive(context, confirmIntent(STATUS_TAKEN))

        // Observing an outcome at all proves PendingResult.finish() ran (it precedes the hook in
        // the receiver's finally) — i.e. finish() is invoked on the timeout path too (AC-004a).
        val outcome = awaitOutcome()
        assertThat(outcome).isInstanceOf(Result.Err.Unexpected::class.java)
        assertThat(shadowNotificationManager.allNotifications).hasSize(1)
    }

    /** A [ConfirmationRepository] that always reports the transaction-failure tier. */
    private object FailingConfirmationRepository : ConfirmationRepository {
        override suspend fun recordFromNotification(confirmation: Confirmation): Result<Unit, Result.Err> =
            Result.Err.Unexpected("simulated transaction failure")
    }

    private companion object {
        const val AWAIT_SECONDS = 5L
        const val DEFAULT_TIMEOUT_MS = 8_000L
        val DEFAULT_GATEWAY_PROVIDER = ConfirmFromNotificationReceiver.gatewayProvider
    }
}
