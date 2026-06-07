package io.nemopill.notifications.infrastructure

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import com.google.common.truth.Truth.assertThat
import io.nemopill.core.event.ReminderVariant
import io.nemopill.core.id.DoseId
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNotificationManager

/**
 * AC-004 — the M-002 item (3) integration test. Drives the notification-rendering leg below the OS
 * edge: the real [NotificationManagerNotificationAdapter] (with the real builder + action factory)
 * presents an on-time Reminder against Robolectric's [ShadowNotificationManager].
 *
 * Asserts: exactly one notification posts on the `reminder_on_time` channel; the channel exists
 * with the pinned ID; the notification has two inline actions labeled "Take" and "Skip"; and both
 * action PendingIntents are immutable broadcasts targeting [ConfirmFromNotificationReceiver],
 * carrying the confirm action and the `{ doseId, status }` extras with `status ∈ {TAKEN, SKIPPED}`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
class NotificationManagerNotificationAdapterRobolectricTest {
    private val doseId = DoseId("demo-dose")

    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager
    private lateinit var shadowNotificationManager: ShadowNotificationManager
    private lateinit var adapter: NotificationManagerNotificationAdapter

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        shadowNotificationManager = shadowOf(notificationManager)

        NotificationChannels.ensureReminderOnTimeChannel(context)

        val actionFactory = ConfirmActionPendingIntentFactory(context)
        val builder = ReminderNotificationBuilder(context, actionFactory)
        adapter = NotificationManagerNotificationAdapter(context, builder)
    }

    @Test
    fun `channel exists with the pinned reminder_on_time id`() {
        val channel = notificationManager.getNotificationChannel(CHANNEL_ID)
        assertThat(channel).isNotNull()
        assertThat(channel.id).isEqualTo(CHANNEL_ID)
    }

    @Test
    fun `presents exactly one notification on the reminder_on_time channel`() =
        runTest {
            adapter.presentReminder(doseId, ReminderVariant.ON_TIME)

            val posted = shadowNotificationManager.allNotifications
            assertThat(posted).hasSize(1)
            assertThat(posted.single().channelId).isEqualTo(CHANNEL_ID)
        }

    @Test
    fun `notification has Take and Skip inline actions`() =
        runTest {
            adapter.presentReminder(doseId, ReminderVariant.ON_TIME)

            val notification = shadowNotificationManager.allNotifications.single()
            assertThat(notification.actions).hasLength(2)
            assertThat(notification.actions.map { it.title.toString() })
                .containsExactly("Take", "Skip")
                .inOrder()
        }

    @Test
    fun `both action PendingIntents are immutable broadcasts targeting the confirm receiver`() =
        runTest {
            adapter.presentReminder(doseId, ReminderVariant.ON_TIME)

            val notification = shadowNotificationManager.allNotifications.single()
            notification.actions.forEach { action ->
                val shadowPendingIntent = shadowOf(action.actionIntent)

                assertThat(shadowPendingIntent.isBroadcastIntent).isTrue()
                assertThat(shadowPendingIntent.flags and PendingIntent.FLAG_IMMUTABLE).isNotEqualTo(0)

                val savedIntent = shadowPendingIntent.savedIntent
                assertThat(savedIntent.action).isEqualTo(ACTION_CONFIRM)
                assertThat(savedIntent.component?.className).isEqualTo(RECEIVER_FQCN)
                assertThat(savedIntent.getStringExtra(EXTRA_DOSE_ID_KEY)).isEqualTo("demo-dose")
                assertThat(savedIntent.getStringExtra(EXTRA_STATUS_KEY)).isAnyOf("TAKEN", "SKIPPED")
            }
        }

    private companion object {
        const val CHANNEL_ID = "reminder_on_time"
        const val ACTION_CONFIRM = "io.nemopill.notifications.ACTION_CONFIRM_FROM_NOTIFICATION"
        const val RECEIVER_FQCN = "io.nemopill.notifications.infrastructure.ConfirmFromNotificationReceiver"
        const val EXTRA_DOSE_ID_KEY = "io.nemopill.notifications.extra.DOSE_ID"
        const val EXTRA_STATUS_KEY = "io.nemopill.notifications.extra.STATUS"
    }
}
