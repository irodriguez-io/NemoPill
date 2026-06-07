package io.nemopill.notifications.infrastructure

import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import io.nemopill.notifications.CHANNEL_REMINDER_ON_TIME
import io.nemopill.notifications.R

/**
 * Creates the notification channels owned by `:notifications` (file 04 § :notifications;
 * file 06 § Section 2). T-009 creates the `reminder_on_time` channel only; `reminder_late` /
 * `reminder_missed` / `reminder_summary` land with their BR-010 variants (M-004).
 *
 * **Importance posture (T-009 ADR):** on-time Reminders are time-critical (BR-004), so the
 * channel is created at [NotificationManagerCompat.IMPORTANCE_HIGH] per file 11 § BR-010
 * channel-importance, which deliberately diverges from M3's quieter notification-style default.
 * The channel **ID** is the contract; importance is a creation-time default the Patient may
 * later override (file 11 / file 06 § Section 2).
 *
 * Display name and description are sourced from string resources — no inline literals
 * (file 11 § Notification channel display names). Idempotent: re-creating an existing channel
 * is a no-op for the ID, so calling this at every cold start is safe.
 */
object NotificationChannels {
    fun ensureReminderOnTimeChannel(context: Context) {
        val channel =
            NotificationChannelCompat.Builder(
                CHANNEL_REMINDER_ON_TIME,
                NotificationManagerCompat.IMPORTANCE_HIGH,
            )
                .setName(context.getString(R.string.notification_channel_reminder_on_time_name))
                .setDescription(
                    context.getString(R.string.notification_channel_reminder_on_time_description),
                )
                .build()

        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }
}
