package io.nemopill.core.port

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class SystemClockAdapterTest {
    @Test
    fun `now returns the underlying clock instant`() {
        val fixed = Instant.parse("2026-06-05T12:00:00Z")
        val adapter = SystemClockAdapter(Clock.fixed(fixed, ZoneOffset.UTC))
        assertThat(adapter.now()).isEqualTo(fixed)
    }
}
