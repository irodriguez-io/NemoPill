package io.nemopill.scheduling.infrastructure

import android.content.Context
import com.google.common.truth.Truth.assertThat
import io.nemopill.scheduling.DEMO_DOSE_ID
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.Duration
import java.time.Instant

/**
 * AC-003 — integration test for [SharedPreferencesPendingReminderStore] against Robolectric's real
 * app-private `SharedPreferences`. Proves the project's first persisted scheduling state round-trips
 * the `DoseId -> Instant` target exactly (epoch-millis), overwrites by `DoseId`, and clears.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
class SharedPreferencesPendingReminderStoreRobolectricTest {
    private val target = Instant.parse("2026-06-05T12:10:00Z")

    private lateinit var context: Context
    private lateinit var store: SharedPreferencesPendingReminderStore

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        store = SharedPreferencesPendingReminderStore(context)
    }

    @Test
    fun `an empty store returns no entries`() =
        runTest {
            assertThat(store.pendingReminders()).isEmpty()
        }

    @Test
    fun `save then read round-trips the target exactly`() =
        runTest {
            store.savePendingReminder(DEMO_DOSE_ID, target)

            assertThat(store.pendingReminders()).containsExactly(DEMO_DOSE_ID, target)
        }

    @Test
    fun `a second save for the same dose overwrites the target`() =
        runTest {
            store.savePendingReminder(DEMO_DOSE_ID, target)
            val later = target.plus(Duration.ofMinutes(5))
            store.savePendingReminder(DEMO_DOSE_ID, later)

            assertThat(store.pendingReminders()).containsExactly(DEMO_DOSE_ID, later)
        }

    @Test
    fun `clear removes the entry`() =
        runTest {
            store.savePendingReminder(DEMO_DOSE_ID, target)

            store.clear(DEMO_DOSE_ID)

            assertThat(store.pendingReminders()).isEmpty()
        }
}
