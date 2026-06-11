package io.nemopill.core.konsist

import com.google.common.truth.Truth.assertThat
import com.lemonappdev.konsist.api.Konsist
import org.junit.Test

/**
 * Feature roots subject to the rule. A `data class` in any of their `.domain` sub-packages is a
 * Domain entity and is treated as Confidential-data-bearing by default (file 13 § Data
 * Classification: Medication / Schedule / Dose / Confirmation state are all `Confidential`).
 */
private val featureRoots =
    listOf(
        "io.nemopill.medicationmanagement",
        "io.nemopill.scheduling",
        "io.nemopill.notifications",
        "io.nemopill.adherencetracking",
    )

/**
 * The ADR-087 resolution's **exempt allow-list**: `:core::event` `data class`es that carry **no**
 * Confidential field. [io.nemopill.core.event.ReminderFired] carries only `DoseId` + `Instant` + a
 * non-Confidential `ReminderVariant` (file 02 / file 13 — opaque IDs and clock instants are
 * Internal, not Confidential), so it is exempt and need not override `toString()`.
 *
 * The other non-PII `:core` `data class`es — `Result.Ok`, `Result.Err.Unexpected`,
 * `Result.Err.UnexpectedNotificationPayload` — fall outside scope automatically (they live in
 * `io.nemopill.core.result`, which is neither a feature `.domain` package nor `:core::event`).
 */
private val exemptFullyQualified =
    setOf(
        "io.nemopill.core.event.ReminderFired",
    )

/**
 * Decides whether a `data class` is in scope for the ADR-049 rule (ii) redaction requirement
 * (the ADR-087 resolution): in scope iff it is a feature-module Domain `data class` **or** a
 * `:core::event` `data class`, minus the [exemptFullyQualified] non-PII allow-list. Extracted as a
 * pure function so the negative test can pin the scope decision without a Konsist scan.
 */
internal fun isConfidentialBearing(
    packageName: String?,
    fullyQualifiedName: String?,
): Boolean {
    val pkg = packageName ?: return false
    if (fullyQualifiedName != null && fullyQualifiedName in exemptFullyQualified) return false
    val isFeatureDomain =
        featureRoots.any { pkg.startsWith(it) } &&
            (pkg.contains(".domain.") || pkg.endsWith(".domain"))
    val isCoreEvent = pkg == "io.nemopill.core.event"
    return isFeatureDomain || isCoreEvent
}

/**
 * Architecture rule — **ADR-049 rule (ii)** (the long-deferred companion to rule (i); ADR-081 /
 * ADR-087). Every Confidential-data-bearing `data class` must override `toString()`, because the
 * Kotlin-generated `data class` `toString()` dumps every field value — for Confidential data that
 * would leak Patient state into logcat / crash evidence (ADR-031, file 13 §§ Observability /
 * Information Disclosure). The override is the redaction.
 *
 * Wired in T-010 per ADR-081 (a code-surface-dependent Konsist rule is wired by the task that
 * first introduces the relevant code surface): `Confirmation` is the project's first
 * `Confidential`-class Domain `data class`, and `:core::event::DoseConfirmed` is the first
 * `:core` event carrying a Confidential field. Runs in `arch-conformance` (stage 5) alongside
 * rule (i)'s `NoDynamicThrowMessageRule` (untouched — rule (i)'s file stays as-is).
 *
 * **Scope (ADR-087 resolution):** feature `.domain` `data class`es + `:core::event` `data class`es,
 * minus the non-PII allow-list ([exemptFullyQualified]). Non-PII shared-kernel types
 * (`Result.Ok` / `Result.Err.*`, `ReminderFired`) are exempt — see [isConfidentialBearing].
 *
 * Implementation note: file-scoped scan (mirrors the proven `.files` + `.path` pattern of the
 * sibling rules), then `data class` enumeration via the class API. Presence of an explicit
 * `override fun toString()` is the redaction signal; the actual field-free content is asserted at
 * runtime by `DoseConfirmedTest` / `ConfirmationTest`.
 */
class DataClassRedactedToStringRule {
    @Test
    fun `confidential-bearing data classes override toString with a redacted form`() {
        Konsist
            .scopeFromProject()
            .files
            .filter { file ->
                !file.path.contains("/fixtures/") &&
                    !file.path.contains("/test/") &&
                    !file.path.contains("/androidTest/")
            }
            .flatMap { it.classes(includeNested = true) }
            .filter { it.hasDataModifier }
            .filter { isConfidentialBearing(it.packagee?.name, it.fullyQualifiedName) }
            .forEach { clazz ->
                val overridesToString = clazz.functions().any { fn -> fn.name == "toString" }
                assert(overridesToString) {
                    "REDACTION VIOLATION (ADR-049 rule (ii) / ADR-087): ${clazz.fullyQualifiedName} " +
                        "is a Confidential-data-bearing data class but does not override toString(). " +
                        "The Kotlin-generated data-class toString() dumps every field value — for " +
                        "Confidential data that leaks Patient state into logcat / crash evidence " +
                        "(ADR-031). Override toString() with a redacted, field-free form (e.g. " +
                        "\"${clazz.name}(REDACTED)\")."
                }
            }
    }
}

/**
 * Negative test — confirms the scope decision and the redaction signal behave as specified:
 * (a) a Confidential-bearing data class lacking a `toString()` override would be flagged;
 * (b) the real [io.nemopill.adherencetracking.domain.Confirmation] /
 *     [io.nemopill.core.event.DoseConfirmed] are in scope **and** override `toString()` (pass);
 * (c) non-PII `:core` types (`Result.Ok`, `ReminderFired`) are exempt (ADR-087 scope).
 */
class DataClassRedactedToStringRuleNegativeTest {
    @Test
    fun `an unredacted confidential-bearing data class would be flagged`() {
        // A feature-domain data class is in scope; without a toString override the rule fires.
        val pkg = "io.nemopill.adherencetracking.domain"
        val fqn = "io.nemopill.adherencetracking.domain.SampleSensitive"
        val inScope = isConfidentialBearing(pkg, fqn)
        val overridesToString = false // the "deliberately unredacted" sample
        assert(inScope && !overridesToString) {
            "Negative-test contract broken: an in-scope sensitive data class with no toString " +
                "override must be a rule violation."
        }
    }

    @Test
    fun `non-PII core shared-kernel types are exempt (ADR-087 resolution)`() {
        assertThat(isConfidentialBearing("io.nemopill.core.result", "io.nemopill.core.result.Result.Ok")).isFalse()
        assertThat(
            isConfidentialBearing("io.nemopill.core.result", "io.nemopill.core.result.Result.Err.Unexpected"),
        ).isFalse()
        assertThat(isConfidentialBearing("io.nemopill.core.event", "io.nemopill.core.event.ReminderFired")).isFalse()
    }

    @Test
    fun `the real Confirmation and DoseConfirmed are in scope and override toString`() {
        val dataClasses =
            Konsist
                .scopeFromProject()
                .files
                .filter { !it.path.contains("/test/") }
                .flatMap { it.classes(includeNested = true) }
                .filter { it.hasDataModifier }

        val confirmation =
            dataClasses.firstOrNull {
                it.fullyQualifiedName == "io.nemopill.adherencetracking.domain.Confirmation"
            }
        val doseConfirmed =
            dataClasses.firstOrNull { it.fullyQualifiedName == "io.nemopill.core.event.DoseConfirmed" }

        assertThat(confirmation).isNotNull()
        assertThat(doseConfirmed).isNotNull()
        assertThat(isConfidentialBearing(confirmation!!.packagee?.name, confirmation.fullyQualifiedName)).isTrue()
        assertThat(isConfidentialBearing(doseConfirmed!!.packagee?.name, doseConfirmed.fullyQualifiedName)).isTrue()
        assertThat(confirmation.functions().any { it.name == "toString" }).isTrue()
        assertThat(doseConfirmed.functions().any { it.name == "toString" }).isTrue()
    }
}
