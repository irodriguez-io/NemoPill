package io.nemopill.app

import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * AC-003 (T-007) — verifies the Robolectric runner resolves on the :app classpath.
 * Deleted or replaced by first real Robolectric test in M-002.
 */
@RunWith(RobolectricTestRunner::class)
class SmokeRobolectricTest {

    @Test
    fun smoke_robolectric_runner_resolves() {
        assertTrue(true)
    }
}
