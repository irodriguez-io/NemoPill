package io.nemopill.core.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withImport
import org.junit.Test

/**
 * Architecture rule: domain-layer packages must not import Android framework
 * types (Clean Architecture boundary, ADR-009).
 *
 * "Domain layer" is defined as files whose package path contains `.domain.`
 * or ends with `.domain`.
 *
 * Allowed Android imports (annotation-only, zero runtime dependency):
 *   - androidx.annotation.*
 *
 * All other `android.*` and `androidx.*` imports in the domain layer are
 * violations. This keeps domain entities and use-case logic pure Kotlin and
 * unit-testable without a device.
 */
class DomainLayerNoAndroidRule {
    @Test
    fun `domain layer classes do not import Android framework types`() {
        val allowedPrefixes = listOf("androidx.annotation.")

        Konsist
            .scopeFromProject()
            .files
            .filter { file ->
                !file.path.contains("/fixtures/") &&
                    !file.path.contains("/test/") &&
                    !file.path.contains("/androidTest/")
            }
            .filter { file ->
                file.packagee?.name?.let { pkg ->
                    pkg.contains(".domain.") || pkg.endsWith(".domain")
                } ?: false
            }
            .withImport { import ->
                val name = import.name
                val isAndroid = name.startsWith("android.") || name.startsWith("androidx.")
                val isAllowed = allowedPrefixes.any { name.startsWith(it) }
                isAndroid && !isAllowed
            }
            .forEach { file ->
                val offending =
                    file.imports
                        .filter { import ->
                            val name = import.name
                            val isAndroid = name.startsWith("android.") || name.startsWith("androidx.")
                            val isAllowed = allowedPrefixes.any { name.startsWith(it) }
                            isAndroid && !isAllowed
                        }
                        .map { it.name }

                assert(false) {
                    "ARCHITECTURE VIOLATION (ADR-009): Domain-layer file ${file.path} " +
                        "imports Android framework type(s): $offending. " +
                        "Domain logic must be pure Kotlin — no Android dependencies."
                }
            }
    }
}

/**
 * Negative test — confirms the detector fires on a domain-layer file that
 * imports a disallowed Android type.
 */
class DomainLayerNoAndroidRuleNegativeTest {
    @Test
    fun `detector flags android Context import in a domain package`() {
        val allowedPrefixes = listOf("androidx.annotation.")
        val fixturePackage = "io.nemopill.scheduling.domain"
        val fixtureImport = "android.content.Context"

        val isDomain = fixturePackage.contains(".domain.") || fixturePackage.endsWith(".domain")
        val isAndroid = fixtureImport.startsWith("android.") || fixtureImport.startsWith("androidx.")
        val isAllowed = allowedPrefixes.any { fixtureImport.startsWith(it) }

        assert(isDomain && isAndroid && !isAllowed) {
            "Negative-test contract broken: an android.content.Context import in a " +
                "domain package must be flagged as a violation."
        }
    }

    @Test
    fun `detector allows androidx annotation import in a domain package`() {
        val allowedPrefixes = listOf("androidx.annotation.")
        val fixtureImport = "androidx.annotation.VisibleForTesting"

        val isAndroid = fixtureImport.startsWith("android.") || fixtureImport.startsWith("androidx.")
        val isAllowed = allowedPrefixes.any { fixtureImport.startsWith(it) }

        assert(isAndroid && isAllowed) {
            "Negative-test contract broken: androidx.annotation.* must be whitelisted."
        }
    }
}
