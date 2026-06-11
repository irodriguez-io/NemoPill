package io.nemopill.app.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.nemopill.app.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * AC-005 — the canonical M-002 Done-When item (5) UI artifact. Renders the stateless [DemoScreen]
 * with a fixed `confirmedCount` and asserts the counter `Text` shows that count (the resource
 * `R.string.demo_confirmed_count` resolved for the value), proving Compose renders the observed
 * dose count `0` and `1`. `createComposeRule` under Robolectric per `_context/05 § Test Portfolio`
 * (E2E row); no pixel snapshot baseline (deferred to M-004/M-006) — a text/semantics assertion.
 * The `StateFlow`→recomposition wiring that feeds this counter is proven by `DemoViewModelTest`
 * (AC-004) + `MainActivity`'s `collectAsState`.
 *
 * **Why `src/testDebug` (debug-only unit-test source set):** `createComposeRule()` launches the
 * `androidx.activity.ComponentActivity` declared by `androidx.compose.ui:ui-test-manifest`, which
 * is a `debugImplementation` dependency — so that `<activity>` is merged only into the **debug**
 * variant's manifest. `:app:test` runs both `testDebugUnitTest` and `testReleaseUnitTest`; placing
 * this test here scopes it to the debug variant (where the activity exists) instead of shipping a
 * test activity in the release APK or disabling release unit tests (T-011 ADR — observe-leg test
 * infrastructure). The pure-JVM `DemoViewModelTest` stays in `src/test` and runs on both variants.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [26])
class DemoScreenCounterRobolectricTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun counterText(count: Int): String = RuntimeEnvironment.getApplication().getString(R.string.demo_confirmed_count, count)

    @Test
    fun `renders the counter reading 1`() {
        composeRule.setContent {
            DemoScreen(state = DemoUiState.Idle, confirmedCount = 1, onSchedule = {})
        }

        composeRule.onNodeWithText(counterText(1)).assertIsDisplayed()
    }

    @Test
    fun `renders the counter reading 0`() {
        composeRule.setContent {
            DemoScreen(state = DemoUiState.Idle, confirmedCount = 0, onSchedule = {})
        }

        composeRule.onNodeWithText(counterText(0)).assertIsDisplayed()
    }
}
