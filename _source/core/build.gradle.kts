plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.kover)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // Core has no Android, network, or persistence dependencies by design — it is the
    // shared domain kernel imported by every feature module. The one production dependency
    // is Kotlin Coroutines core: the InProcessEventBus is a MutableSharedFlow<DomainEvent>
    // (file 04 § DomainEventPublisher). coroutines-core is pure-JVM/multiplatform — it adds
    // no Android types, so the "no Android imports" build guarantee holds (T-009 ADR).
    implementation(libs.kotlinx.coroutines.core)

    // Architecture conformance tests — Konsist runs in the test source set
    // of :core so it can inspect the entire project classpath from one place.
    testImplementation(libs.konsist)
    testImplementation(libs.junit4)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
}

// Kover per-package coverage gate (ADR-078 deferral resolved at T-008 — real code now
// present). :core shared kernel must hold >= 90% line coverage on io.nemopill.core.*.
//
// Kover 0.8.x forbids per-rule filters; filters are set report-wide (they scope both the
// report and the verify gate). For :core this is exact — the whole module is io.nemopill.core.
kover {
    reports {
        filters {
            includes {
                classes("io.nemopill.core.*")
            }
        }
        verify {
            rule("io.nemopill.core line coverage >= 90%") {
                bound {
                    minValue = 90
                }
            }
        }
    }
}
