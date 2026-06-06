package io.nemopill.scheduling

import io.nemopill.core.port.ClockPort
import java.time.Instant

/**
 * Test [ClockPort] with a settable instant (file 04 § ClockPort test fake). Scoped to the
 * `:scheduling` test source set for this slice; promote to `:core` `testFixtures` if a
 * second module needs it later.
 */
class FakeClock(
    var instant: Instant,
) : ClockPort {
    override fun now(): Instant = instant
}
