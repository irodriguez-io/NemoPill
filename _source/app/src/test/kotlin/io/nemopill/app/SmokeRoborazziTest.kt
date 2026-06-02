package io.nemopill.app

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * AC-004 (T-007) — verifies the roborazzi artifact is on the :app test classpath.
 * Uses reflection to avoid unused-import and overload-ambiguity issues with the
 * captureRoboImage top-level functions. Deleted/replaced in M-003 when the
 * first real Compose snapshot test lands.
 */
class SmokeRoborazziTest {
    @Test
    fun smoke_roborazzi_classpath_resolves() {
        val found =
            runCatching {
                Class.forName("io.github.takahirom.roborazzi.RoborazziOptions")
            }.isSuccess
        assertTrue("roborazzi not found on test classpath", found)
    }
}
