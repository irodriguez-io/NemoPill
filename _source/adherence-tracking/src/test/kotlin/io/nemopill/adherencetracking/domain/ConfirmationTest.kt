package io.nemopill.adherencetracking.domain

import com.google.common.truth.Truth.assertThat
import io.nemopill.core.confirm.ConfirmationSource
import io.nemopill.core.confirm.ConfirmationStatus
import io.nemopill.core.id.ConfirmationId
import io.nemopill.core.id.DoseId
import org.junit.Test
import java.time.Instant

/** Unit coverage for the [Confirmation] Domain entity, including its ADR-049 rule (ii) redaction. */
class ConfirmationTest {
    private val confirmation =
        Confirmation(
            confirmationId = ConfirmationId("c-1"),
            doseId = DoseId("demo-dose"),
            status = ConfirmationStatus.TAKEN,
            confirmedAt = Instant.parse("2026-06-07T12:10:00Z"),
            source = ConfirmationSource.NOTIFICATION_ACTION,
        )

    @Test
    fun `carries its fields`() {
        assertThat(confirmation.doseId).isEqualTo(DoseId("demo-dose"))
        assertThat(confirmation.status).isEqualTo(ConfirmationStatus.TAKEN)
        assertThat(confirmation.source).isEqualTo(ConfirmationSource.NOTIFICATION_ACTION)
        assertThat(confirmation.confirmedAt).isEqualTo(Instant.parse("2026-06-07T12:10:00Z"))
    }

    @Test
    fun `toString is redacted and leaks no Confidential field values (ADR-049 rule ii)`() {
        val rendered = confirmation.toString()
        assertThat(rendered).isEqualTo("Confirmation(REDACTED)")
        assertThat(rendered).doesNotContain("demo-dose")
        assertThat(rendered).doesNotContain("c-1")
        assertThat(rendered).doesNotContain("TAKEN")
        assertThat(rendered).doesNotContain("2026")
    }
}
