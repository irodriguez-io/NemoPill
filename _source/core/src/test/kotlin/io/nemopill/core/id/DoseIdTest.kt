package io.nemopill.core.id

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DoseIdTest {
    @Test
    fun `wraps and exposes its value`() {
        assertThat(DoseId("demo-dose").value).isEqualTo("demo-dose")
    }

    @Test
    fun `is equal by value`() {
        assertThat(DoseId("a")).isEqualTo(DoseId("a"))
        assertThat(DoseId("a")).isNotEqualTo(DoseId("b"))
    }
}
