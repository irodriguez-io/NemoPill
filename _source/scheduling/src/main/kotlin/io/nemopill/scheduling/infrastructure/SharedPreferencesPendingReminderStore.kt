package io.nemopill.scheduling.infrastructure

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import io.nemopill.core.id.DoseId
import io.nemopill.scheduling.application.PendingReminderStore
import java.time.Instant
import javax.inject.Inject

/**
 * App-private `SharedPreferences`-backed [PendingReminderStore] (T-012, file 06 § F-011 boot path).
 *
 * Stores `doseId.value -> Instant.toEpochMilli()` as a `Long` in a dedicated app-private prefs file.
 * This is the project's first at-rest scheduling state; the value is a **non-PII** `DoseId` +
 * epoch-millis trigger time (`_context/13` `Internal` / trigger-metadata class — not `Confidential`),
 * so the file-05 `EncryptedSharedPreferences` rule and ADR-049 rule (ii) do not engage (ADR-031). It
 * is **not Room** (no schema change, no `2.json`, no migration — AC-006); the real persisted-`Dose`
 * re-arm is M-004's Room work.
 *
 * Injected `@ApplicationContext context` (the established [AlarmManagerSchedulerAdapter] pattern).
 * Writes use `apply()` (the durable write happens off the calling thread; reads on the boot
 * receiver's bounded background scope reflect the in-memory edit immediately). The prefs file is
 * app-private and removed on uninstall (ADR-034).
 */
class SharedPreferencesPendingReminderStore
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : PendingReminderStore {
        private val prefs: SharedPreferences
            get() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        override suspend fun savePendingReminder(
            doseId: DoseId,
            at: Instant,
        ) {
            prefs.edit().putLong(doseId.value, at.toEpochMilli()).apply()
        }

        override suspend fun pendingReminders(): Map<DoseId, Instant> =
            prefs.all.entries
                .mapNotNull { (key, value) ->
                    (value as? Long)?.let { DoseId(key) to Instant.ofEpochMilli(it) }
                }.toMap()

        override suspend fun clear(doseId: DoseId) {
            prefs.edit().remove(doseId.value).apply()
        }

        private companion object {
            /** App-private prefs file holding pending Reminder targets (file 06 § F-011). */
            const val PREFS_NAME = "io.nemopill.scheduling.pending_reminders"
        }
    }
