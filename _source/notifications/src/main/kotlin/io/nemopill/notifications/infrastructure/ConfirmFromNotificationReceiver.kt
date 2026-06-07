package io.nemopill.notifications.infrastructure

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import io.nemopill.core.id.DoseId
import io.nemopill.notifications.ACTION_CONFIRM_FROM_NOTIFICATION
import io.nemopill.notifications.EXTRA_DOSE_ID
import io.nemopill.notifications.EXTRA_STATUS
import io.nemopill.notifications.STATUS_SKIPPED
import io.nemopill.notifications.STATUS_TAKEN
import io.nemopill.notifications.notificationIdFor

/**
 * Ingress for the inline "Take" / "Skip" confirm taps (file 06 § Section 2). Manifest-declared in
 * `:app` with **`exported="false"`** (file 05 / file 13 § THR-002) — only the OS, holding our
 * `PendingIntent`, can fire it.
 *
 * **T-009 is a logging-and-dismiss stub.** It validates the action at the entry point, logs a
 * static, non-PII line keyed on the bounded status, and dismisses the source notification by its
 * stable per-Dose ID. It deliberately does **not** invoke any Confirmation use case and does
 * **not** write to Room — the typed parse → `ConfirmDoseFromReminderUseCase` dispatch and the Room
 * `Confirmation` write are the next slice (T-010, M-002 item 4). Because the stub does no async
 * work, it stays synchronous; `goAsync()` + a bounded scope land in T-010 with the suspend dispatch
 * that actually needs them (file 06 § Timeout rule).
 */
class ConfirmFromNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != ACTION_CONFIRM_FROM_NOTIFICATION) return
        val rawDoseId = intent.getStringExtra(EXTRA_DOSE_ID) ?: return

        when (intent.getStringExtra(EXTRA_STATUS)) {
            STATUS_TAKEN -> Log.i(TAG, "Confirmation tap received: TAKEN (stub — no write yet)")
            STATUS_SKIPPED -> Log.i(TAG, "Confirmation tap received: SKIPPED (stub — no write yet)")
            else -> {
                Log.w(TAG, "Confirmation tap received with an unknown status; ignoring")
                return
            }
        }

        NotificationManagerCompat.from(context).cancel(notificationIdFor(DoseId(rawDoseId)))
    }

    private companion object {
        const val TAG = "NemoPill.notifications.ConfirmFromNotificationReceiver"
    }
}
