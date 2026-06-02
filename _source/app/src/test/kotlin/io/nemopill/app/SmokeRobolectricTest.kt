package io.nemopill.app

import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * AC-003 (T-007) — verifies the Robolectric runner resolves on the :app classpath.
 * Config.NONE skips manifest loading; sdk = [26] pins the minimum SDK to avoid
 * downloading higher-API Robolectric JARs on a cold CI runner.
 * Deleted or replaced by first real Robolectric test in M-002.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [26])
class SmokeRobolectricTest {
    @Test
    fun smoke_robolectric_runner_resolves() {
        assertTrue(true)
    }
}
