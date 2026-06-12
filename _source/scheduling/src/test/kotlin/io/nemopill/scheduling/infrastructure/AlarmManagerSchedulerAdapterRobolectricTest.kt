package io.nemopill.scheduling.infrastructure

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import com.google.common.truth.Truth.assertThat
import io.nemopill.scheduling.DEMO_DOSE_ID
import io.nemopill.scheduling.FakeClock
import io.nemopill.scheduling.application.FakePendingReminderStore
import io.nemopill.scheduling.application.ScheduleDemoReminderUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
import java.time.Duration
import java.time.Instant

/**
 * AC-003 — integration test for [AlarmManagerSchedulerAdapter] against Robolectric's
 * [ShadowAlarmManager]. Retires the Robolectric half of M-002 assumption (1) for the
 * scheduling leg: drives the real adapter from the use case with a [FakeClock] and asserts
 * the scheduled trigger time, that the operation PendingIntent is immutable, and that it
 * targets [ReminderAlarmReceiver].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
class AlarmManagerSchedulerAdapterRobolectricTest {
    private val now = Instant.parse("2026-06-05T12:00:00Z")
    private val expected = now.plus(Duration.ofMinutes(10))

    private lateinit var context: Context
    private lateinit var alarmManager: AlarmManager
    private lateinit var shadowAlarmManager: ShadowAlarmManager
    private lateinit var adapter: AlarmManagerSchedulerAdapter

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        shadowAlarmManager = shadowOf(alarmManager)
        adapter = AlarmManagerSchedulerAdapter(context, alarmManager)
    }

    @Test
    fun `use case schedules exactly one alarm at now plus ten minutes`() =
        runTest {
            val useCase = ScheduleDemoReminderUseCase(FakeClock(now), adapter, FakePendingReminderStore())

            useCase()

            assertThat(shadowAlarmManager.scheduledAlarms).hasSize(1)
            assertThat(shadowAlarmManager.scheduledAlarms.first().triggerAtMs)
                .isEqualTo(expected.toEpochMilli())
        }

    @Test
    fun `operation PendingIntent is immutable and targets ReminderAlarmReceiver`() =
        runTest {
            adapter.scheduleReminder(DEMO_DOSE_ID, expected)

            val scheduled = shadowAlarmManager.scheduledAlarms.single()
            val shadowOperation = shadowOf(scheduled.operation)
            assertThat(shadowOperation.isBroadcastIntent).isTrue()
            assertThat(shadowOperation.flags and PendingIntent.FLAG_IMMUTABLE).isNotEqualTo(0)
            assertThat(shadowOperation.savedIntent.component?.className)
                .isEqualTo("io.nemopill.scheduling.infrastructure.ReminderAlarmReceiver")
        }

    @Test
    fun `re-scheduling the same dose is idempotent - a single alarm`() =
        runTest {
            val useCase = ScheduleDemoReminderUseCase(FakeClock(now), adapter, FakePendingReminderStore())

            useCase()
            useCase()

            assertThat(shadowAlarmManager.scheduledAlarms).hasSize(1)
        }
}
