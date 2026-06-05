package io.nemopill.core.konsist

import com.lemonappdev.konsist.api.Konsist
import org.junit.Test

/**
 * Architecture rule — ADR-049 rule (i) (aligned with ADR-031): no production `throw`
 * statement builds its message argument from a Kotlin string template (`${'$'}{...}`),
 * `String.format(...)`, or `StringBuilder.append(...)`.
 *
 * Rationale: an exception message assembled from dynamic content can leak Patient data
 * into crash reports / logcat (ADR-031 — "no Patient data in failure evidence"). Throw
 * messages must be static string literals, so failures are named by type and aggregate
 * ID only.
 *
 * Wired at T-008 per ADR-081 — the convention that a code-surface-dependent Konsist rule
 * is wired by the task that first introduces the relevant code surface. T-008 lands the
 * first production `throw` (the use-case boundary's `CancellationException` rethrow), which
 * this rule exercises (and passes, as `throw ce` carries no message).
 *
 * Implementation note: like [PendingIntentFlagImmutableRule], this uses a line-level
 * text-scan heuristic rather than Konsist AST analysis, which at 0.17.0 cannot reliably
 * parse throw-argument expressions. Each line bearing the `throw` keyword (comments
 * stripped) is checked for the three forbidden dynamic-content markers.
 */
class NoDynamicThrowMessageRule {
    private val forbiddenMarkers = listOf("\${", "String.format", ".append(")
    private val throwKeyword = Regex("""\bthrow\b""")

    @Test
    fun `no production throw builds its message from dynamic content`() {
        Konsist
            .scopeFromProject()
            .files
            .filter { file ->
                !file.path.contains("/fixtures/") &&
                    !file.path.contains("/test/") &&
                    !file.path.contains("/androidTest/")
            }
            .forEach { file ->
                file.text.lineSequence().forEach { line ->
                    val code = line.substringBefore("//")
                    if (throwKeyword.containsMatchIn(code)) {
                        val hasDynamicMessage = forbiddenMarkers.any { code.contains(it) }
                        assert(!hasDynamicMessage) {
                            "REDACTION VIOLATION (ADR-049 rule (i) / ADR-031): ${file.path} throws " +
                                "with a dynamically built message. Throw messages must be static " +
                                "string literals — no string templates, String.format, or " +
                                "StringBuilder.append — so Patient data cannot leak into failure evidence."
                        }
                    }
                }
            }
    }
}

/**
 * Negative test — confirms the detector fires when a throw message is dynamically built,
 * and passes when it is a static literal.
 */
class NoDynamicThrowMessageRuleNegativeTest {
    private val forbiddenMarkers = listOf("\${", "String.format", ".append(")
    private val throwKeyword = Regex("""\bthrow\b""")

    @Test
    fun `detector fires on a throw with a string-template message`() {
        val fixture = "throw IllegalStateException(\"bad dose \${doseId.value}\")"
        val isThrow = throwKeyword.containsMatchIn(fixture)
        val hasDynamicMessage = forbiddenMarkers.any { fixture.contains(it) }
        assert(isThrow && hasDynamicMessage) {
            "Negative-test contract broken: the fixture must be a throw with a dynamic message."
        }
    }

    @Test
    fun `detector passes a throw with a static literal message`() {
        val fixture = "throw IllegalStateException(\"scheduling failed\")"
        val isThrow = throwKeyword.containsMatchIn(fixture)
        val hasDynamicMessage = forbiddenMarkers.any { fixture.contains(it) }
        assert(isThrow && !hasDynamicMessage) {
            "Negative-test contract broken: the fixture must pass the redaction check."
        }
    }
}
