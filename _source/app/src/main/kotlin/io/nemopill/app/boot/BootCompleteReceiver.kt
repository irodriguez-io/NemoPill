package io.nemopill.app.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.VisibleForTesting
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import io.nemopill.core.result.Result
import io.nemopill.scheduling.application.ReArmDemoReminderUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.cancellation.CancellationException

/**
 * Boot-survival ingress (T-012, file 06 § F-011 happy path; M-002 Done-When item (6)). Manifest-
 * declared in `:app` with **`exported="true"`** — required only because the OS delivers the
 * system-protected `BOOT_COMPLETED` broadcast (file 05 § OS permissions; `_context/13` THR-003).
 *
 * `AlarmManager` exact alarms are cleared on reboot, so on `BOOT_COMPLETED` this receiver re-arms
 * the persisted demo Reminder by invoking [ReArmDemoReminderUseCase], which re-reads the target
 * from the `:scheduling` [io.nemopill.scheduling.application.PendingReminderStore] and re-registers
 * it through the existing `SchedulerPort` **if it is still in the future** (a past-due target is a
 * no-op — the F-008 missed-sweep is M-004). The re-arm reuses `AlarmManagerSchedulerAdapter`'s
 * `FLAG_IMMUTABLE` `PendingIntent` (ADR-023) — this receiver builds **no new `PendingIntent`**.
 *
 * **Entry-point parsing (file 05 § Input validation; `_context/13` boundary):** the only inbound
 * check is the action-string match — the receiver reads **only `intent.action`**, parses no
 * untrusted extras, and does no Patient-data work. Anything that is not `ACTION_BOOT_COMPLETED`
 * returns early.
 *
 * **Injection (mirrors T-009 `ReminderAlarmReceiver` / T-010 `ConfirmFromNotificationReceiver`):** a
 * manifest `BroadcastReceiver` is OS-instantiated and cannot be constructor-injected, and the
 * `@AndroidEntryPoint` field-injection path is unavailable for a `BroadcastReceiver` (its
 * `super.onReceive` hook targets the abstract `BroadcastReceiver.onReceive`). The use case is pulled
 * from the application's `SingletonComponent` via [EntryPointAccessors] and the [BootEntryPoint]
 * below. The provider is exposed as a `@VisibleForTesting` seam so a test can substitute a fake/real
 * use case (mirroring `ConfirmFromNotificationReceiver.gatewayProvider`).
 *
 * **`goAsync()` lifetime (file 06 § Timeout rule):** the re-arm runs on a bounded coroutine scope
 * under [REARM_TIMEOUT_MS] (a 5 s cap — the work is a fast `SharedPreferences` read + an idempotent
 * `scheduleReminder`, no Room write; well under the 8 s receiver ceiling in `_context/10` /
 * `_context/12` and the OS ~10 s receiver kill — see the T-012 `withTimeout`-cap ADR).
 * `PendingResult.finish()` runs in `finally` on every path. Log lines are static and non-PII
 * (ADR-031 / ADR-049 rule (i)).
 */
class BootCompleteReceiver : BroadcastReceiver() {
    /** Hilt access to the `:scheduling` boot re-arm use case in the application's SingletonComponent. */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BootEntryPoint {
        fun reArmDemoReminder(): ReArmDemoReminderUseCase
    }

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pending = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope.launch {
            var outcome: Result<Unit, Result.Err> = Result.Err.Unexpected(REARM_FAILURE_MESSAGE)
            try {
                outcome =
                    withTimeout(REARM_TIMEOUT_MS) {
                        useCaseProvider(context).invoke()
                    }
                if (outcome is Result.Err) {
                    Log.w(TAG, "Boot re-arm did not complete cleanly")
                }
            } catch (te: TimeoutCancellationException) {
                Log.w(TAG, "Boot re-arm timed out")
                outcome = Result.Err.Unexpected(REARM_TIMEOUT_MESSAGE)
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                Log.w(TAG, "Boot re-arm failed")
                outcome = Result.Err.Unexpected(REARM_FAILURE_MESSAGE)
            } finally {
                // finish() runs on EVERY path. `goAsync()` returns non-null for a real system
                // broadcast (always the case in production); the null-guard covers a unit test that
                // invokes onReceive() directly, where no PendingResult is tracked.
                pending?.finish()
                onOutcomeForTest?.invoke(outcome)
            }
        }
    }

    companion object {
        private const val TAG = "NemoPill.app.BootCompleteReceiver"
        private const val REARM_TIMEOUT_MESSAGE = "Boot re-arm timed out"
        private const val REARM_FAILURE_MESSAGE = "Boot re-arm failed"

        /**
         * `goAsync()` cap — **5 000 ms** (mirrors `ReminderAlarmReceiver.PUBLISH_TIMEOUT_MS`). The
         * boot re-arm is trivial work (a `SharedPreferences` read + an idempotent `scheduleReminder`,
         * no Room write), so it uses a tighter cap than the 8 s ceiling stated generically in
         * `_context/10 § Availability` / `_context/12 § Configuration row (c)`; 8 s is the ceiling,
         * not a floor (T-012 `withTimeout`-cap ADR). Well under the OS ~10 s receiver lifetime.
         */
        private const val REARM_TIMEOUT_MS = 5_000L

        /**
         * Resolves [ReArmDemoReminderUseCase] from the application's SingletonComponent (the
         * production seam — [EntryPointAccessors], no `@Inject` fields, per the receiver-injection
         * rationale). Overridable in tests to inject a fake/real use case.
         */
        @VisibleForTesting
        var useCaseProvider: (Context) -> ReArmDemoReminderUseCase = { context ->
            EntryPointAccessors
                .fromApplication(context.applicationContext, BootEntryPoint::class.java)
                .reArmDemoReminder()
        }

        /**
         * Test hook invoked in the `finally` **after** `PendingResult.finish()` on every path —
         * observing an outcome therefore proves the bounded scope completed and `finish()` ran.
         * Null in production.
         */
        @VisibleForTesting
        var onOutcomeForTest: ((Result<Unit, Result.Err>) -> Unit)? = null
    }
}
