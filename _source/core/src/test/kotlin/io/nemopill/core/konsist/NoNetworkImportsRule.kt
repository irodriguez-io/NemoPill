package io.nemopill.core.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withImport
import org.junit.Test

/**
 * Architecture rule: no network-layer imports anywhere in production code (ADR-021).
 *
 * Banned import prefixes:
 *   - java.net.*       — JDK networking stack
 *   - okhttp3.*        — OkHttp HTTP client
 *   - retrofit2.*      — Retrofit REST adapter
 *
 * The rule scans all Kotlin source files reachable from the project root,
 * excluding test source sets and fixture files.
 *
 * Positive test:   no production file imports a banned prefix.
 * Negative test:   [NoNetworkImportsRuleNegativeTest] confirms the detector
 *                  fires on a fixture file that contains a banned import.
 */
class NoNetworkImportsRule {

    @Test
    fun `production code contains no network imports`() {
        val bannedPrefixes = listOf("java.net.", "okhttp3.", "retrofit2.")

        Konsist
            .scopeFromProject()
            .files
            .filter { file ->
                // Exclude test source sets and fixture files
                !file.path.contains("/test/") &&
                    !file.path.contains("/androidTest/") &&
                    !file.path.contains("/fixtures/")
            }
            .withImport { import ->
                bannedPrefixes.any { prefix -> import.name.startsWith(prefix) }
            }
            .forEach { file ->
                val offendingImports = file.imports
                    .filter { import ->
                        bannedPrefixes.any { prefix -> import.name.startsWith(prefix) }
                    }
                    .map { it.name }

                assert(false) {
                    "SECURITY VIOLATION (ADR-021): ${file.path} contains banned " +
                        "network import(s): $offendingImports. " +
                        "Medication data must never leave the device. " +
                        "No network access is permitted."
                }
            }
    }
}

/**
 * Negative test — confirms the rule fires when a banned import is present.
 */
class NoNetworkImportsRuleNegativeTest {

    @Test
    fun `detector recognises java_net import as a violation`() {
        val bannedPrefixes = listOf("java.net.", "okhttp3.", "retrofit2.")
        val fixtureImport = "java.net.URL"

        val detected = bannedPrefixes.any { prefix -> fixtureImport.startsWith(prefix) }
        assert(detected) {
            "Negative-test contract broken: 'java.net.URL' must be flagged as banned."
        }
    }

    @Test
    fun `detector recognises okhttp3 import as a violation`() {
        val bannedPrefixes = listOf("java.net.", "okhttp3.", "retrofit2.")
        val fixtureImport = "okhttp3.OkHttpClient"

        val detected = bannedPrefixes.any { prefix -> fixtureImport.startsWith(prefix) }
        assert(detected) {
            "Negative-test contract broken: 'okhttp3.OkHttpClient' must be flagged as banned."
        }
    }

    @Test
    fun `detector recognises retrofit2 import as a violation`() {
        val bannedPrefixes = listOf("java.net.", "okhttp3.", "retrofit2.")
        val fixtureImport = "retrofit2.Retrofit"

        val detected = bannedPrefixes.any { prefix -> fixtureImport.startsWith(prefix) }
        assert(detected) {
            "Negative-test contract broken: 'retrofit2.Retrofit' must be flagged as banned."
        }
    }
}
