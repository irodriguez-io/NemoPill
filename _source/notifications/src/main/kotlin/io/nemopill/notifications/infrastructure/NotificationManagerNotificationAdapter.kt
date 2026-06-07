package io.nemopill.notifications.infrastructure

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.nemopill.core.event.ReminderVariant
import io.nemopill.core.id.DoseId
import io.nemopill.notifications.application.NotificationPort
import io.nemopill.notifications.notificationIdFor
import javax.inject.Inject

/**
 * The [NotificationPort] implementation backed by [NotificationManagerCompat] (file 04 §
 * NotificationPort; file 06 § Section 1). Builds the notification via [ReminderNotificationBuilder]
 * and posts it under a stable per-Dose ID ([notificationIdFor]) so a replayed `ReminderFired`
 * re-renders rather than stacks (dedupe key = `doseId`, file 06 § Idempotency rule).
 *
 * T-009 renders the on-time variant only; [ReminderVariant.LATE] presentation is M-004.
 *
 * **POST_NOTIFICATIONS (Android 13+):** the manifest declares the permission; for the T-009 demo
 * we rely on a manual grant. If the grant is absent the post is a guarded no-op (the
 * `Result.Err.NotificationsPermissionRevoked` UX affordance is deferred to M-004 — file 06 §
 * F-005). The static log line carries no Patient data (ADR-031).
 */
class NotificationManagerNotificationAdapter
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val builder: ReminderNotificationBuilder,
    ) : NotificationPort {
        // areNotificationsEnabled() guards the post; the deferred runtime-permission UX is M-004.
        @SuppressLint("MissingPermission")
        override suspend fun presentReminder(
            doseId: DoseId,
            variant: ReminderVariant,
        ) {
            val manager = NotificationManagerCompat.from(context)
            if (!manager.areNotificationsEnabled()) {
                Log.w(TAG, "Notifications disabled; on-time reminder not shown")
                return
            }
            val notification = builder.buildOnTime(doseId)
            manager.notify(notificationIdFor(doseId), notification)
        }

        private companion object {
            const val TAG = "NemoPill.notifications.NotificationManagerNotificationAdapter"
        }
    }
