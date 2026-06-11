package io.nemopill.core.event

import com.google.common.truth.Truth.assertThat
import io.nemopill.core.confirm.ConfirmationSource
import io.nemopill.core.confirm.ConfirmationStatus
import io.nemopill.core.id.ConfirmationId
import io.nemopill.core.id.DoseId
import org.junit.Test
import java.time.Instant

/**
 * Unit coverage for [DoseConfirmed] — including the ADR-049 rule (ii) redaction contract: the
 * event carries Confidential `status`, so its `toString()` must leak no field values.
 */
class DoseConfirmedTest {
    private val event =
        DoseConfirmed(
            doseId = DoseId("demo-dose"),
            confirmationId = ConfirmationId("c-1"),
            status = ConfirmationStatus.TAKEN,
            confirmedAt = Instant.parse("2026-06-07T12:00:00Z"),
            source = ConfirmationSource.NOTIFICATION_ACTION,
        )

    @Test
    fun `is a DomainEvent carrying the confirmation fields`() {
        assertThat(event).isInstanceOf(DomainEvent::class.java)
        assertThat(event.doseId).isEqualTo(DoseId("demo-dose"))
        assertThat(event.status).isEqualTo(ConfirmationStatus.TAKEN)
        assertThat(event.source).isEqualTo(ConfirmationSource.NOTIFICATION_ACTION)
    }

    @Test
    fun `toString is redacted and leaks no Confidential field values (ADR-049 rule ii)`() {
        val rendered = event.toString()
        assertThat(rendered).isEqualTo("DoseConfirmed(REDACTED)")
        assertThat(rendered).doesNotContain("demo-dose")
        assertThat(rendered).doesNotContain("c-1")
        assertThat(rendered).doesNotContain("TAKEN")
        assertThat(rendered).doesNotContain("2026")
    }

    @Test
    fun `dedupe key is confirmationId plus confirmedAt`() {
        val sameKey = event.copy(source = ConfirmationSource.IN_APP)
        assertThat(sameKey.confirmationId).isEqualTo(event.confirmationId)
        assertThat(sameKey.confirmedAt).isEqualTo(event.confirmedAt)
    }
}
