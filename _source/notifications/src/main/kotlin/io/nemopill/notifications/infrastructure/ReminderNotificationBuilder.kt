package io.nemopill.notifications.infrastructure

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.nemopill.core.id.DoseId
import io.nemopill.notifications.CHANNEL_REMINDER_ON_TIME
import io.nemopill.notifications.R
import io.nemopill.notifications.contentTapRequestCode
import javax.inject.Inject

/**
 * Builds the on-time Reminder [Notification] (file 11 § Notification (inline action)).
 *
 * Title / body come from the `reminder_on_time` copy template resolved with **hardcoded, non-PII
 * demo placeholders** (file 11 § notification copy; T-009 carries no real Patient data). The two
 * inline "Take" / "Skip" actions come from [ConfirmActionPendingIntentFactory]; the content-tap
 * `PendingIntent` opens the app via the launcher Intent resolved at runtime — avoiding a direct
 * `MainActivity` reference so `:notifications` never imports `:app` (mirrors the T-008
 * `AlarmManagerSchedulerAdapter` pattern). All PendingIntents use `FLAG_IMMUTABLE` (ADR-023).
 * Copy is sentence case with no trailing period on the title (file 11 § Capitalization).
 */
class ReminderNotificationBuilder
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val actionFactory: ConfirmActionPendingIntentFactory,
    ) {
        fun buildOnTime(doseId: DoseId): Notification {
            val medicationName = context.getString(R.string.demo_medication_name)
            val doseDescriptor = context.getString(R.string.demo_dose_descriptor)
            val doseTime = context.getString(R.string.demo_dose_time)

            val builder =
                NotificationCompat.Builder(context, CHANNEL_REMINDER_ON_TIME)
                    .setSmallIcon(R.drawable.ic_notification_reminder)
                    .setContentTitle(
                        context.getString(R.string.notification_reminder_on_time_title, medicationName),
                    )
                    .setContentText(
                        context.getString(
                            R.string.notification_reminder_on_time_body,
                            doseDescriptor,
                            doseTime,
                        ),
                    )
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setAutoCancel(true)
                    .addAction(
                        NotificationCompat.Action(
                            0,
                            context.getString(R.string.notification_action_take),
                            actionFactory.takeAction(doseId),
                        ),
                    )
                    .addAction(
                        NotificationCompat.Action(
                            0,
                            context.getString(R.string.notification_action_skip),
                            actionFactory.skipAction(doseId),
                        ),
                    )

            contentTapIntent(doseId)?.let(builder::setContentIntent)

            return builder.build()
        }

        private fun contentTapIntent(doseId: DoseId): PendingIntent? {
            val launch =
                context.packageManager.getLaunchIntentForPackage(context.packageName)
                    ?: return null
            return PendingIntent.getActivity(
                context,
                contentTapRequestCode(doseId),
                launch,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }
    }
