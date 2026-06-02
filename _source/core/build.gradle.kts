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
    // Core has no production dependencies by design — it is the shared
    // domain kernel imported by every feature module. Keep this section
    // minimal; do NOT add Android framework, network, or persistence
    // dependencies here.

    // Architecture conformance tests — Konsist runs in the test source set
    // of :core so it can inspect the entire project classpath from one place.
    testImplementation(libs.konsist)
    testImplementation(libs.junit4)
}

// ---------------------------------------------------------------------------
// Kover coverage thresholds — :core (ADR-044 / T-007)
// Threshold: ≥ 90 % line coverage on io.nemopill.core.* packages.
// Vacuously satisfied at T-007 because src/main/kotlin is empty.
// ---------------------------------------------------------------------------
koverReport {
    verify {
        rule("core line coverage ≥ 90 %") {
            filters {
                includes {
                    packages("io.nemopill.core")
                }
            }
            bound {
                minValue = 90
            }
        }
    }
}
