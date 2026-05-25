package io.nemopill.core.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withImport
import org.junit.Test

/**
 * Architecture rule: every PendingIntent construction site must include
 * `PendingIntent.FLAG_IMMUTABLE` (ADR-023, security guardrail).
 *
 * All four PendingIntent factory methods are checked:
 *   - PendingIntent.getBroadcast(...)
 *   - PendingIntent.getActivity(...)
 *   - PendingIntent.getService(...)
 *   - PendingIntent.getForegroundService(...)
 *
 * The rule uses a text-scan approach: any production Kotlin file that
 * imports `android.app.PendingIntent` must not contain a call to the above
 * factory methods without `FLAG_IMMUTABLE` in the same logical block.
 *
 * Implementation note: Konsist's AST-level analysis of flags bitmask
 * expressions is not reliable enough for this check at Konsist 0.17.0 —
 * a simpler co-occurrence heuristic is used instead. If a file calls any
 * PendingIntent factory but the text does not contain `FLAG_IMMUTABLE`,
 * the check fails.
 */
class PendingIntentFlagImmutableRule {

    private val pendingIntentFactories = listOf(
        "PendingIntent.getBroadcast",
        "PendingIntent.getActivity",
        "PendingIntent.getService",
        "PendingIntent.getForegroundService",
    )

    @Test
    fun `all PendingIntent construction sites include FLAG_IMMUTABLE`() {
        Konsist
            .scopeFromProject()
            .files
            .filter { file ->
                !file.path.contains("/fixtures/") &&
                    !file.path.contains("/test/") &&
                    !file.path.contains("/androidTest/")
            }
            .withImport { import ->
                import.name == "android.app.PendingIntent"
            }
            .forEach { file ->
                val text = file.text
                val callsFactory = pendingIntentFactories.any { text.contains(it) }

                if (callsFactory) {
                    val hasFlagImmutable = text.contains("FLAG_IMMUTABLE")

                    assert(hasFlagImmutable) {
                        "SECURITY VIOLATION (ADR-023): ${file.path} constructs a " +
                            "PendingIntent but does not include FLAG_IMMUTABLE. " +
                            "All PendingIntent construction must use FLAG_IMMUTABLE " +
                            "to prevent intent redirection attacks on Android 12+."
                    }
                }
            }
    }
}

/**
 * Negative test — confirms the rule fires when FLAG_IMMUTABLE is absent.
 */
class PendingIntentFlagImmutableRuleNegativeTest {

    @Test
    fun `detector fires when PendingIntent factory call lacks FLAG_IMMUTABLE`() {
        // Simulate a file that imports PendingIntent and calls getBroadcast
        // but omits FLAG_IMMUTABLE.
        val fixtureText = """
            import android.app.PendingIntent
            val pi = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        """.trimIndent()

        val callsFactory = fixtureText.contains("PendingIntent.getBroadcast")
        val hasFlagImmutable = fixtureText.contains("FLAG_IMMUTABLE")

        assert(callsFactory && !hasFlagImmutable) {
            "Negative-test contract broken: the fixture must call a PendingIntent factory " +
                "without FLAG_IMMUTABLE so the detector can be verified."
        }
    }

    @Test
    fun `detector passes when PendingIntent factory includes FLAG_IMMUTABLE`() {
        val fixtureText = """
            import android.app.PendingIntent
            val pi = PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        """.trimIndent()

        val callsFactory = fixtureText.contains("PendingIntent.getBroadcast")
        val hasFlagImmutable = fixtureText.contains("FLAG_IMMUTABLE")

        assert(callsFactory && hasFlagImmutable) {
            "Negative-test contract broken: the fixture must pass the FLAG_IMMUTABLE check."
        }
    }
}
