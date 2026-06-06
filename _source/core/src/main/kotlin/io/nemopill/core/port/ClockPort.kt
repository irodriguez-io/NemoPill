package io.nemopill.core.port

import java.time.Instant

/**
 * Reads the current instant (file 04 § ClockPort). Domain stays pure; "now" enters via
 * this port as a parameter rather than being read from the system clock directly.
 */
interface ClockPort {
    fun now(): Instant
}
