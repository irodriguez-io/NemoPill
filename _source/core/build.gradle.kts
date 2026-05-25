plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktlint)
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
