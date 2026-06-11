package io.nemopill.adherencetracking

import io.nemopill.core.port.ClockPort
import java.time.Instant

/**
 * Test [ClockPort] with a settable instant (file 04 § ClockPort test fake). Scoped to the
 * `:adherence-tracking` test source set, mirroring the `:scheduling` `FakeClock`; promote to a
 * shared `:core` `testFixtures` if a third module needs it.
 */
class FakeClock(
    var instant: Instant,
) : ClockPort {
    override fun now(): Instant = instant
}
