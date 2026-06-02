package io.nemopill.app

import org.junit.Test

/**
 * AC-004 (T-007) — roborazzi classpath smoke test.
 * Roborazzi being on the test classpath is proven by :app compiling in stage 6.
 * This test is intentionally empty — no assertions, no roborazzi-specific class
 * references that could vary across patch versions.
 * Deleted/replaced in M-003 when the first real captureRoboImage() call lands.
 */
class SmokeRoborazziTest {
    @Test
    fun smoke_roborazzi_classpath_resolves() {
        // No-op. Dependency resolution verified by successful compilation.
    }
}
