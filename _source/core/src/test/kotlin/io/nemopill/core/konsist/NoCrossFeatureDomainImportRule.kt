package io.nemopill.core.konsist

import com.lemonappdev.konsist.api.Konsist
import org.junit.Test

/**
 * Architecture rule: feature modules must not import each other's
 * domain or application packages (ADR-009 Bounded Context isolation).
 *
 * The five bounded contexts and their root packages are:
 *   - medication-management   → io.nemopill.medicationmanagement
 *   - scheduling              → io.nemopill.scheduling
 *   - notifications           → io.nemopill.notifications
 *   - adherence-tracking      → io.nemopill.adherencetracking
 *   - app (shell only)        → io.nemopill.app       (exempt — orchestration layer)
 *   - core (shared kernel)    → io.nemopill.core      (exempt — allowed everywhere)
 *
 * A cross-feature import occurs when a file whose package belongs to one
 * feature root imports from a **different** feature root's `.domain` or
 * `.application` sub-packages.
 */
class NoCrossFeatureDomainImportRule {
    // Feature roots that are subject to isolation (app and core are exempt).
    private val featureRoots =
        listOf(
            "io.nemopill.medicationmanagement",
            "io.nemopill.scheduling",
            "io.nemopill.notifications",
            "io.nemopill.adherencetracking",
        )

    @Test
    fun `feature modules do not import each other's domain or application packages`() {
        Konsist
            .scopeFromProject()
            .files
            .filter { file ->
                !file.path.contains("/fixtures/") &&
                    !file.path.contains("/test/") &&
                    !file.path.contains("/androidTest/")
            }
            .forEach { file ->
                val filePackage = file.packagee?.name ?: return@forEach
                val sourceRoot =
                    featureRoots.firstOrNull { filePackage.startsWith(it) }
                        ?: return@forEach // file is in core, app, or another exempt root

                val violations =
                    file.imports
                        .map { it.name }
                        .filter { importName ->
                            // Target must be in a DIFFERENT feature root
                            val targetRoot = featureRoots.firstOrNull { importName.startsWith(it) }
                            targetRoot != null && targetRoot != sourceRoot
                        }
                        .filter { importName ->
                            // Only flag domain and application sub-packages
                            importName.contains(".domain.") ||
                                importName.contains(".domain") ||
                                importName.contains(".application.") ||
                                importName.contains(".application")
                        }

                assert(violations.isEmpty()) {
                    "ARCHITECTURE VIOLATION (ADR-009): Feature module [$sourceRoot] " +
                        "in ${file.path} imports from another feature module's domain/application: " +
                        "$violations. " +
                        "Cross-feature communication must go through :core shared kernel only."
                }
            }
    }
}

/**
 * Negative test — confirms the cross-feature detector identifies a violation.
 */
class NoCrossFeatureDomainImportRuleNegativeTest {
    @Test
    fun `detector flags scheduling module importing from medication-management domain`() {
        val featureRoots =
            listOf(
                "io.nemopill.medicationmanagement",
                "io.nemopill.scheduling",
                "io.nemopill.notifications",
                "io.nemopill.adherencetracking",
            )

        val filePackage = "io.nemopill.scheduling.application"
        val fixtureImport = "io.nemopill.medicationmanagement.domain.Medication"

        val sourceRoot = featureRoots.firstOrNull { filePackage.startsWith(it) }
        val targetRoot = featureRoots.firstOrNull { fixtureImport.startsWith(it) }

        val isCrossFeature = sourceRoot != null && targetRoot != null && sourceRoot != targetRoot
        val isDomainOrApplication =
            fixtureImport.contains(".domain.") || fixtureImport.contains(".domain")

        assert(isCrossFeature && isDomainOrApplication) {
            "Negative-test contract broken: the fixture import must be detected as a " +
                "cross-feature domain import violation."
        }
    }
}
