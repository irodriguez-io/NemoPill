package io.nemopill.core.port

import java.time.Clock
import java.time.Instant

/**
 * Default [ClockPort] backed by [Clock.systemUTC]. Lives in :core (file 04): both
 * `Clock.systemUTC()` and `ZoneId.systemDefault()` are JVM-standard, so the default
 * adapter belongs alongside the port in the pure-Kotlin shared kernel.
 */
class SystemClockAdapter(
    private val clock: Clock = Clock.systemUTC(),
) : ClockPort {
    override fun now(): Instant = clock.instant()
}
