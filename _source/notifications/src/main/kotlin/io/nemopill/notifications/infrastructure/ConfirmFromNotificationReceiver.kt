package io.nemopill.notifications.infrastructure

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import io.nemopill.core.confirm.ConfirmationStatus
import io.nemopill.core.confirm.NotificationConfirmationGateway
import io.nemopill.core.id.DoseId
import io.nemopill.core.result.Result
import io.nemopill.notifications.ACTION_CONFIRM_FROM_NOTIFICATION
import io.nemopill.notifications.EXTRA_DOSE_ID
import io.nemopill.notifications.EXTRA_STATUS
import io.nemopill.notifications.STATUS_SKIPPED
import io.nemopill.notifications.STATUS_TAKEN
import io.nemopill.notifications.notificationIdFor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.cancellation.CancellationException

/**
 * Ingress for the inline "Take" / "Skip" confirm taps (file 06 § F-006). Manifest-declared in
 * `:app` with **`exported="false"`** (file 05 / file 13 § THR-002) — only the OS, holding our
 * `FLAG_IMMUTABLE` `PendingIntent`, can fire it.
 *
 * **T-010 replaces the T-009 logging-and-dismiss stub (ADR-095)** with the real dispatch:
 *  1. **Typed parse at the entry point** (file 05 § Input validation / file 13 boundary): the
 *     `doseId` extra (any non-empty string) is read and the `status` extra is typed into
 *     [ConfirmationStatus]. An unknown `status` (impossible from an app-generated `FLAG_IMMUTABLE`
 *     PendingIntent; defensive against intent redirection) yields
 *     [Result.Err.UnexpectedNotificationPayload] — logged, **notification left** so the Patient can
 *     retry from in-app (file 06 § Retry rule). `Result.Err.UnknownDose` (missing-Dose resolution)
 *     is deferred — there is no `Dose` table in M-002 (the demo-vs-full-F-006 gap, T-010 ADR).
 *  2. **Dispatch** via the `:core` [NotificationConfirmationGateway] seam (T-010 ADR — option C):
 *     `:notifications` depends only on the `:core` port; `:adherence-tracking` provides the impl;
 *     `:app` binds it. The gateway is obtained from the SingletonComponent via [EntryPointAccessors]
 *     (constructor injection / provision entry point — **not** `@Inject` fields, per ADR-092).
 *  3. On [Result.Ok] the source notification is **dismissed** by its stable per-Dose ID; on any
 *     [Result.Err] (parse-reject, transaction failure, timeout) the notification is **not**
 *     dismissed (file 06 § Retry rule — the Patient retry affordance).
 *
 * **`goAsync()` lifetime (file 06 § Timeout rule; file 10 / file 12 § Configuration row (c)):** the
 * dispatch is a suspend Room write, so the receiver now uses `goAsync()` (the deferred-from-T-009
 * `goAsync`, ADR-095) on a bounded coroutine scope under [confirmTimeoutMillis] = **8 000 ms** (the
 * pinned 2-second margin under the OS ~10-second hard kill). `PendingResult.finish()` runs in a
 * `finally` on **every** path; on timeout overrun the outcome is [Result.Err.Unexpected] (static
 * message — ADR-049 rule (i) stays green) and the notification is left undismissed.
 */
class ConfirmFromNotificationReceiver : BroadcastReceiver() {
    /** Hilt access to the `:core` confirm seam binding in `:app` (mirrors the T-009 receiver pattern). */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ConfirmEntryPoint {
        fun notificationConfirmationGateway(): NotificationConfirmationGateway
    }

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != ACTION_CONFIRM_FROM_NOTIFICATION) return

        val pending = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope.launch {
            var outcome: Result<Unit, Result.Err> = Result.Err.UnexpectedNotificationPayload
            try {
                val rawDoseId = intent.getStringExtra(EXTRA_DOSE_ID)
                val status = parseStatus(intent.getStringExtra(EXTRA_STATUS))
                outcome =
                    when {
                        rawDoseId.isNullOrEmpty() -> {
                            Log.w(TAG, "Confirmation tap received without a dose id; ignoring")
                            Result.Err.UnexpectedNotificationPayload
                        }
                        status == null -> {
                            Log.w(TAG, "Confirmation tap received with an unknown status; ignoring")
                            Result.Err.UnexpectedNotificationPayload
                        }
                        else -> {
                            val result =
                                withTimeout(confirmTimeoutMillis) {
                                    gatewayProvider(context).confirm(DoseId(rawDoseId), status)
                                }
                            if (result is Result.Ok) {
                                NotificationManagerCompat.from(context).cancel(notificationIdFor(DoseId(rawDoseId)))
                            } else {
                                Log.w(TAG, "Confirmation persistence failed; notification left for retry")
                            }
                            result
                        }
                    }
            } catch (te: TimeoutCancellationException) {
                Log.w(TAG, "Confirmation dispatch timed out; notification left for retry")
                outcome = Result.Err.Unexpected(CONFIRM_TIMEOUT_MESSAGE)
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                Log.w(TAG, "Confirmation dispatch failed; notification left for retry")
                outcome = Result.Err.Unexpected(CONFIRM_FAILURE_MESSAGE)
            } finally {
                // finish() runs on EVERY path (success, parse-reject, transaction-failure, timeout)
                // — file 06 § Timeout rule / AC-004a. `goAsync()` returns non-null for a real system
                // broadcast (always the case in production); the null-guard covers a unit test that
                // invokes onReceive() directly, where no PendingResult is tracked.
                pending?.finish()
                onOutcomeForTest?.invoke(outcome)
            }
        }
    }

    private fun parseStatus(raw: String?): ConfirmationStatus? =
        when (raw) {
            STATUS_TAKEN -> ConfirmationStatus.TAKEN
            STATUS_SKIPPED -> ConfirmationStatus.SKIPPED
            else -> null
        }

    companion object {
        private const val TAG = "NemoPill.notifications.ConfirmFromNotificationReceiver"
        private const val CONFIRM_TIMEOUT_MESSAGE = "Confirmation dispatch timed out"
        private const val CONFIRM_FAILURE_MESSAGE = "Confirmation dispatch failed"

        /**
         * `goAsync()` internal cap — **8 000 ms** (file 12 § Configuration And Secrets row (c);
         * file 10 § Resource limits): 2-second margin under Android's ~10-second hard receiver
         * kill. Overridable only for tests (so the timeout path is asserted without an 8-second
         * wall-clock wait).
         */
        @VisibleForTesting
        var confirmTimeoutMillis: Long = 8_000L

        /**
         * Resolves the `:core` [NotificationConfirmationGateway] from the application's
         * SingletonComponent (the production seam — ADR-092 `EntryPointAccessors`, no `@Inject`
         * fields). Overridable in tests to inject a real-Room or failing gateway.
         */
        @VisibleForTesting
        var gatewayProvider: (Context) -> NotificationConfirmationGateway = { context ->
            EntryPointAccessors
                .fromApplication(context.applicationContext, ConfirmEntryPoint::class.java)
                .notificationConfirmationGateway()
        }

        /**
         * Test hook invoked in the `finally` **after** `PendingResult.finish()` on every path —
         * observing an outcome therefore proves the `finally` ran and `finish()` was called
         * (AC-004a). Null in production.
         */
        @VisibleForTesting
        var onOutcomeForTest: ((Result<Unit, Result.Err>) -> Unit)? = null
    }
}
