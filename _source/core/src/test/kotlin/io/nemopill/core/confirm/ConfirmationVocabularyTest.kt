package io.nemopill.core.confirm

import com.google.common.truth.Truth.assertThat
import io.nemopill.core.id.ConfirmationId
import org.junit.Test

/**
 * Unit coverage for the `:core::confirm` shared-kernel vocabulary relocated here by the T-010 seam
 * ADR ([ConfirmationStatus], [ConfirmationSource]) and the [ConfirmationId] newtype in `:core::id`.
 */
class ConfirmationVocabularyTest {
    @Test
    fun `ConfirmationId wraps its value and mints unique random ids`() {
        assertThat(ConfirmationId("c-1").value).isEqualTo("c-1")
        assertThat(ConfirmationId("c-1")).isEqualTo(ConfirmationId("c-1"))

        val a = ConfirmationId.random()
        val b = ConfirmationId.random()
        assertThat(a.value).isNotEmpty()
        assertThat(a).isNotEqualTo(b)
    }

    @Test
    fun `ConfirmationStatus is bounded to TAKEN and SKIPPED only (BR-005)`() {
        assertThat(ConfirmationStatus.entries).containsExactly(
            ConfirmationStatus.TAKEN,
            ConfirmationStatus.SKIPPED,
        )
        assertThat(ConfirmationStatus.valueOf("TAKEN")).isEqualTo(ConfirmationStatus.TAKEN)
        assertThat(ConfirmationStatus.valueOf("SKIPPED")).isEqualTo(ConfirmationStatus.SKIPPED)
    }

    @Test
    fun `ConfirmationSource declares all three sources (additive-only)`() {
        assertThat(ConfirmationSource.entries).containsExactly(
            ConfirmationSource.NOTIFICATION_ACTION,
            ConfirmationSource.IN_APP,
            ConfirmationSource.RETROACTIVE,
        )
        assertThat(ConfirmationSource.valueOf("NOTIFICATION_ACTION"))
            .isEqualTo(ConfirmationSource.NOTIFICATION_ACTION)
        assertThat(ConfirmationSource.valueOf("IN_APP")).isEqualTo(ConfirmationSource.IN_APP)
        assertThat(ConfirmationSource.valueOf("RETROACTIVE")).isEqualTo(ConfirmationSource.RETROACTIVE)
    }
}
