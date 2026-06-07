package io.nemopill.notifications.infrastructure

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import io.nemopill.core.id.DoseId
import io.nemopill.notifications.ACTION_CONFIRM_FROM_NOTIFICATION
import io.nemopill.notifications.EXTRA_DOSE_ID
import io.nemopill.notifications.EXTRA_STATUS
import io.nemopill.notifications.STATUS_SKIPPED
import io.nemopill.notifications.STATUS_TAKEN
import io.nemopill.notifications.skipRequestCode
import io.nemopill.notifications.takeRequestCode
import javax.inject.Inject

/**
 * Builds the two inline-action `PendingIntent`s carried by an on-time Reminder (file 11 §
 * Notification (inline action); file 06 § Section 2): **"Take"** (`status = TAKEN`) and **"Skip"**
 * (`status = SKIPPED`).
 *
 * Both are broadcasts targeting [ConfirmFromNotificationReceiver], carrying the
 * [ACTION_CONFIRM_FROM_NOTIFICATION] action and `{ doseId, status }` extras (aggregate ID only —
 * no Patient data, ADR-031). Every PendingIntent uses **`FLAG_IMMUTABLE`** (ADR-023 security
 * guardrail, exercised by the `PendingIntentFlagImmutableRule` Konsist check), blocking
 * intent-redirection on Android 12+. Request codes are stable per `(doseId, action)` so the two
 * intents never collide.
 */
class ConfirmActionPendingIntentFactory
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        fun takeAction(doseId: DoseId): PendingIntent = build(doseId, STATUS_TAKEN, takeRequestCode(doseId))

        fun skipAction(doseId: DoseId): PendingIntent = build(doseId, STATUS_SKIPPED, skipRequestCode(doseId))

        private fun build(
            doseId: DoseId,
            status: String,
            requestCode: Int,
        ): PendingIntent {
            val intent =
                Intent(context, ConfirmFromNotificationReceiver::class.java).apply {
                    action = ACTION_CONFIRM_FROM_NOTIFICATION
                    putExtra(EXTRA_DOSE_ID, doseId.value)
                    putExtra(EXTRA_STATUS, status)
                }
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }
    }
