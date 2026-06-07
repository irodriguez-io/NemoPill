package io.nemopill.notifications

import io.nemopill.core.id.DoseId

/*
 * Shared `:notifications` constants and ID derivations. `internal` so the infrastructure adapter,
 * the action factory, and the confirm receiver agree on one set of channel IDs, Intent action /
 * extra keys, and the per-Dose notification-ID derivation.
 */

/** On-time Reminder channel ID — pinned in file 06 § Section 2; ID-stable (renaming loses Patient settings). */
internal const val CHANNEL_REMINDER_ON_TIME: String = "reminder_on_time"

/** Intent action for the inline "Take" / "Skip" confirm taps (file 06 § Section 2). */
internal const val ACTION_CONFIRM_FROM_NOTIFICATION: String =
    "io.nemopill.notifications.ACTION_CONFIRM_FROM_NOTIFICATION"

/** Confirm-Intent extra: aggregate [DoseId] only (ADR-031 — no Patient data in the payload). */
internal const val EXTRA_DOSE_ID: String = "io.nemopill.notifications.extra.DOSE_ID"

/** Confirm-Intent extra: the confirmation status, bounded to [STATUS_TAKEN] / [STATUS_SKIPPED]. */
internal const val EXTRA_STATUS: String = "io.nemopill.notifications.extra.STATUS"

/** Confirmation status values (file 06 § Section 2; BR-005 enum bounds). */
internal const val STATUS_TAKEN: String = "TAKEN"
internal const val STATUS_SKIPPED: String = "SKIPPED"

private const val NOTIFICATION_ID_BASE: Int = 0x4E60
private const val REQUEST_CODE_TAKE_BASE: Int = 0x4E70
private const val REQUEST_CODE_SKIP_BASE: Int = 0x4E80
private const val REQUEST_CODE_CONTENT_TAP_BASE: Int = 0x4E90

/**
 * Stable per-Dose notification ID (dedupe key is [DoseId] — file 06 § Idempotency rule). A
 * replayed [io.nemopill.core.event.ReminderFired] for the same Dose re-renders the same
 * notification rather than stacking a duplicate; the confirm receiver cancels by the same ID.
 */
internal fun notificationIdFor(doseId: DoseId): Int = NOTIFICATION_ID_BASE + doseId.value.hashCode()

/** Distinct, stable request codes per Dose so the two action PendingIntents do not collide. */
internal fun takeRequestCode(doseId: DoseId): Int = REQUEST_CODE_TAKE_BASE + doseId.value.hashCode()

internal fun skipRequestCode(doseId: DoseId): Int = REQUEST_CODE_SKIP_BASE + doseId.value.hashCode()

internal fun contentTapRequestCode(doseId: DoseId): Int = REQUEST_CODE_CONTENT_TAP_BASE + doseId.value.hashCode()
