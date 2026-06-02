package io.nemopill.app

import io.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test

/**
 * AC-004 (T-007) — verifies the roborazzi-compose import resolves on the :app test classpath.
 * No snapshot is captured here. Deleted or replaced by first real snapshot test in M-003.
 */
class SmokeRoborazziTest {

    @Suppress("UnusedPrivateMember")
    @Test
    fun smoke_roborazzi_import_resolves() {
        // Import-resolution smoke test only. captureRoboImage referenced via
        // function reference to confirm the symbol is on the classpath without
        // triggering an actual Compose render in this empty test body.
        val _ = ::captureRoboImage
    }
}
