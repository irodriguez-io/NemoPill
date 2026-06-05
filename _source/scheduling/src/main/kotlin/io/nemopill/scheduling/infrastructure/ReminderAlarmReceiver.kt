package io.nemopill.scheduling.infrastructure

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Alarm entry point for a Dose's Reminder. Relocated to `:scheduling::infrastructure` (from
 * `:app`) per file 04 so [AlarmManagerSchedulerAdapter] can target it without a
 * `:scheduling -> :app` dependency.
 *
 * T-008 keeps `onReceive` a no-op stub — notification rendering is the next M-002 slice
 * (item 3), explicitly out of scope here. The single log line is static (ADR-031: no dynamic
 * content / no Patient data in failure evidence). Declared `exported="false"` in `:app`'s
 * manifest (security guardrail; only `BootCompleteReceiver` is the sanctioned `exported="true"`).
 */
class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        Log.d(TAG, "Reminder alarm received")
    }

    private companion object {
        const val TAG = "NemoPill.scheduling.ReminderAlarmReceiver"
    }
}
