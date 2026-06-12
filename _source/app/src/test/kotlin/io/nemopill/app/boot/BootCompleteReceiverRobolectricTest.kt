package io.nemopill.app.boot

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import com.google.common.truth.Truth.assertThat
import io.nemopill.core.id.DoseId
import io.nemopill.core.port.ClockPort
import io.nemopill.core.result.Result
import io.nemopill.scheduling.application.PendingReminderStore
import io.nemopill.scheduling.application.ReArmDemoReminderUseCase
import io.nemopill.scheduling.application.SchedulerPort
import io.nemopill.scheduling.infrastructure.AlarmManagerSchedulerAdapter
import io.nemopill.scheduling.infrastructure.SharedPreferencesPendingReminderStore
import kotlinx.coroutines.runBlocking
import org.junit.After
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
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * AC-005 — the M-002 item (6) artifact. Drives the boot-survival path below the OS edge: delivers
 * an `Intent(ACTION_BOOT_COMPLETED)` to a real [BootCompleteReceiver] and asserts it invokes
 * [ReArmDemoReminderUseCase] (observed via the receiver's `@VisibleForTesting` `useCaseProvider`
 * seam + the downstream [SchedulerPort] effect), while a non-`BOOT_COMPLETED` action is a no-op
 * (the action guard). The receiver's production seam (`useCaseProvider` = `EntryPointAccessors`) is
 * overridden with a fake/real use case; the async outcome is observed through `onOutcomeForTest`,
 * which the receiver invokes in its `finally` **after** `PendingResult.finish()` — so observing an
 * outcome proves the bounded `goAsync()` / `withTimeout` scope completed and `finish()` ran.
 *
 * The final test is the action-item-(5) end-to-end Robolectric integration test (handoff
 * 2026-06-11T23:30Z), wiring **real** `BootCompleteReceiver -> ReArmDemoReminderUseCase ->
 * AlarmManagerSchedulerAdapter -> ShadowAlarmManager` to satisfy `_context/05 § Test Portfolio`
 * Integration row (b) ("the BOOT_COMPLETED re-registration path") directly:
 *
 *   Scenario: a reboot during the demo Reminder's wait window re-arms the alarm (F-011 happy path)
 *     Given the demo Reminder's target was persisted to the PendingReminderStore
 *       And the target is still in the future
 *     When the device finishes booting and delivers BOOT_COMPLETED
 *     Then the alarm is re-armed at its original scheduled time
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
class BootCompleteReceiverRobolectricTest {
    private val demoDoseId = DoseId("demo-dose")
    private val now = Instant.parse("2026-06-05T12:00:00Z")
    private val futureTarget = now.plus(Duration.ofMinutes(7))

    private lateinit var context: Context
    private val outcomes = LinkedBlockingQueue<Result<Unit, Result.Err>>()

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        BootCompleteReceiver.onOutcomeForTest = { outcomes.put(it) }
    }

    @After
    fun tearDown() {
        // Reset the static test seams so they cannot leak across tests.
        BootCompleteReceiver.useCaseProvider = DEFAULT_USE_CASE_PROVIDER
        BootCompleteReceiver.onOutcomeForTest = null
    }

    private fun bootIntent(): Intent = Intent(Intent.ACTION_BOOT_COMPLETED)

    private fun awaitOutcome(): Result<Unit, Result.Err>? = outcomes.poll(AWAIT_SECONDS, TimeUnit.SECONDS)

    @Test
    fun `a BOOT_COMPLETED broadcast re-arms a future persisted target through the scheduler`() {
        val scheduler = RecordingSchedulerPort()
        val store = FakePendingReminderStore().apply { runBlocking { savePendingReminder(demoDoseId, futureTarget) } }
        BootCompleteReceiver.useCaseProvider = { ReArmDemoReminderUseCase(FixedClock(now), scheduler, store) }

        BootCompleteReceiver().onReceive(context, bootIntent())

        val outcome = awaitOutcome()
        assertThat(outcome).isEqualTo(Result.Ok(Unit))
        assertThat(scheduler.scheduleCallCount).isEqualTo(1)
        assertThat(scheduler.activeRegistrations).containsExactly(demoDoseId to futureTarget)
    }

    @Test
    fun `a non-BOOT_COMPLETED action is ignored - the use case is never invoked`() {
        val scheduler = RecordingSchedulerPort()
        val store = FakePendingReminderStore().apply { runBlocking { savePendingReminder(demoDoseId, futureTarget) } }
        BootCompleteReceiver.useCaseProvider = { ReArmDemoReminderUseCase(FixedClock(now), scheduler, store) }

        BootCompleteReceiver().onReceive(context, Intent(Intent.ACTION_TIME_CHANGED))

        // The action guard returns before launching, so no outcome is ever published and no
        // re-arm is dispatched.
        assertThat(awaitOutcome()).isNull()
        assertThat(scheduler.scheduleCallCount).isEqualTo(0)
    }

    @Test
    fun `end-to-end - real receiver, use case, adapter and ShadowAlarmManager re-arm at the persisted time`() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarmManager: ShadowAlarmManager = shadowOf(alarmManager)
        val store = SharedPreferencesPendingReminderStore(context)
        runBlocking { store.savePendingReminder(demoDoseId, futureTarget) }
        val adapter = AlarmManagerSchedulerAdapter(context, alarmManager)
        BootCompleteReceiver.useCaseProvider = { ReArmDemoReminderUseCase(FixedClock(now), adapter, store) }

        BootCompleteReceiver().onReceive(context, bootIntent())

        assertThat(awaitOutcome()).isEqualTo(Result.Ok(Unit))
        assertThat(shadowAlarmManager.scheduledAlarms).hasSize(1)
        assertThat(shadowAlarmManager.scheduledAlarms.first().triggerAtMs)
            .isEqualTo(futureTarget.toEpochMilli())
    }

    private companion object {
        const val AWAIT_SECONDS = 5L
        val DEFAULT_USE_CASE_PROVIDER = BootCompleteReceiver.useCaseProvider
    }
}

/** Local [ClockPort] fake (the `:scheduling` `FakeClock` lives in that module's test source set). */
private class FixedClock(
    private val instant: Instant,
) : ClockPort {
    override fun now(): Instant = instant
}

/** Local recording [SchedulerPort] (the `:scheduling` test fake is not on the `:app` test classpath). */
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

/** Local in-memory [PendingReminderStore] fake for the fake-EntryPoint dispatch tests. */
private class FakePendingReminderStore : PendingReminderStore {
    private val entries = mutableMapOf<DoseId, Instant>()

    override suspend fun savePendingReminder(
        doseId: DoseId,
        at: Instant,
    ) {
        entries[doseId] = at
    }

    override suspend fun pendingReminders(): Map<DoseId, Instant> = entries.toMap()

    override suspend fun clear(doseId: DoseId) {
        entries.remove(doseId)
    }
}
