package io.nemopill.scheduling

import io.nemopill.core.id.DoseId
import java.time.Duration

/**
 * T-008 demo-slice constants. A single hardcoded Dose and a fixed `now + 10 min` offset.
 * Real Dose materialization (BR-009 3-day horizon, per-Dose IDs) lands in a later M-002 slice.
 * The `WorkManager` backoff, `goAsync()` 8-second cap, and exact-alarm permission-flavor
 * constants from `_context/12` are out of scope here (no WorkManager, no async receiver work).
 */
internal val DEMO_REMINDER_OFFSET: Duration = Duration.ofMinutes(10)

internal val DEMO_DOSE_ID: DoseId = DoseId("demo-dose")

/** Base for the stable, per-`doseId` operation PendingIntent request code (BR-004 idempotent replace). */
internal const val REQUEST_CODE_BASE: Int = 0x4E50

/** Fixed request code for the AlarmClockInfo `showIntent` PendingIntent. */
internal const val SHOW_INTENT_REQUEST_CODE: Int = 0x4E51

/**
 * Alarm-Intent extra carrying the aggregate [DoseId] only (ADR-031 — no Patient data in trigger
 * metadata). Written by `AlarmManagerSchedulerAdapter`, read by `ReminderAlarmReceiver` at the
 * entry point (file 06 § AlarmManager PendingIntent boundary). Shared so both sides agree on the
 * key.
 */
internal const val EXTRA_DOSE_ID: String = "io.nemopill.scheduling.extra.DOSE_ID"
