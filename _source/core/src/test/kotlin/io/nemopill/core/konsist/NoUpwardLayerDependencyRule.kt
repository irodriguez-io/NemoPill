package io.nemopill.core.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withImport
import org.junit.Test

/**
 * Architecture rule: layer-direction dependencies must flow inward only
 * (Clean Architecture, ADR-009).
 *
 * Forbidden upward dependencies:
 *   - infrastructure layer  →  presentation layer
 *   - application layer     →  presentation layer
 *   - domain layer          →  application layer
 *   - domain layer          →  infrastructure layer
 *   - domain layer          →  presentation layer
 *
 * Layer membership is determined by package path segments:
 *   domain         → contains `.domain.`       or ends with `.domain`
 *   application    → contains `.application.`  or ends with `.application`
 *   infrastructure → contains `.infrastructure.` or ends with `.infrastructure`
 *   presentation   → contains `.presentation.` or ends with `.presentation`
 *                    or contains `.ui.`         or ends with `.ui`
 */
class NoUpwardLayerDependencyRule {
    private val presentationMarkers = listOf(".presentation.", ".presentation", ".ui.", ".ui")
    private val applicationMarkers = listOf(".application.", ".application")
    private val infrastructureMarkers = listOf(".infrastructure.", ".infrastructure")
    private val domainMarkers = listOf(".domain.", ".domain")

    private val isPresentation: String.() -> Boolean = {
        presentationMarkers.any { marker -> contains(marker) || endsWith(marker) }
    }

    private val isApplication: String.() -> Boolean = {
        applicationMarkers.any { marker -> contains(marker) || endsWith(marker) }
    }

    private val isInfrastructure: String.() -> Boolean = {
        infrastructureMarkers.any { marker -> contains(marker) || endsWith(marker) }
    }

    private val isDomain: String.() -> Boolean = {
        domainMarkers.any { marker -> contains(marker) || endsWith(marker) }
    }

    @Test
    fun `infrastructure layer does not import presentation layer`() {
        checkNoUpwardImport(
            sourceLayerCheck = isInfrastructure,
            bannedTargetCheck = isPresentation,
            ruleName = "infrastructure → presentation",
        )
    }

    @Test
    fun `application layer does not import presentation layer`() {
        checkNoUpwardImport(
            sourceLayerCheck = isApplication,
            bannedTargetCheck = isPresentation,
            ruleName = "application → presentation",
        )
    }

    @Test
    fun `domain layer does not import application layer`() {
        checkNoUpwardImport(
            sourceLayerCheck = isDomain,
            bannedTargetCheck = isApplication,
            ruleName = "domain → application",
        )
    }

    @Test
    fun `domain layer does not import infrastructure layer`() {
        checkNoUpwardImport(
            sourceLayerCheck = isDomain,
            bannedTargetCheck = isInfrastructure,
            ruleName = "domain → infrastructure",
        )
    }

    @Test
    fun `domain layer does not import presentation layer`() {
        checkNoUpwardImport(
            sourceLayerCheck = isDomain,
            bannedTargetCheck = isPresentation,
            ruleName = "domain → presentation",
        )
    }

    private fun checkNoUpwardImport(
        sourceLayerCheck: String.() -> Boolean,
        bannedTargetCheck: String.() -> Boolean,
        ruleName: String,
    ) {
        Konsist
            .scopeFromProject()
            .files
            .filter { file ->
                !file.path.contains("/fixtures/") &&
                    !file.path.contains("/test/") &&
                    !file.path.contains("/androidTest/")
            }
            .filter { file ->
                file.packagee?.name?.sourceLayerCheck() == true
            }
            .withImport { import ->
                import.name.bannedTargetCheck()
            }
            .forEach { file ->
                val offending =
                    file.imports
                        .filter { it.name.bannedTargetCheck() }
                        .map { it.name }

                assert(false) {
                    "ARCHITECTURE VIOLATION (ADR-009): Forbidden dependency [$ruleName] " +
                        "in ${file.path}. Offending imports: $offending. " +
                        "Dependencies must flow inward: presentation → application → domain."
                }
            }
    }
}

/**
 * Negative test — confirms the rule detects an upward dependency in a fixture.
 */
class NoUpwardLayerDependencyRuleNegativeTest {
    @Test
    fun `detector flags domain package importing from application package`() {
        val sourcePackage = "io.nemopill.scheduling.domain"
        val bannedImport = "io.nemopill.scheduling.application.SomeUseCase"

        val isDomain = sourcePackage.contains(".domain.") || sourcePackage.endsWith(".domain")
        val isApplication = bannedImport.contains(".application.") || bannedImport.endsWith(".application")

        assert(isDomain && isApplication) {
            "Negative-test contract broken: a domain package importing from application " +
                "must be detected as a violation."
        }
    }
}
