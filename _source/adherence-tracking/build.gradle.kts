plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.kover)
}

android {
    namespace = "io.nemopill.adherencetracking"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)

    // Room: the @Dao / @Entity / @TypeConverter declarations + withTransaction (room-ktx) live
    // here. The production DAO impl is generated at the @Database site in :app (the single
    // NemoPillDatabase aggregate per file 04), so this module needs only the Room runtime, not the
    // Room compiler on the main source set (T-010 ADR — minimal KSP placement, mirroring how
    // multi-module Room generates DAO impls at the @Database compilation unit).
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)

    // Hilt library only (no Hilt Gradle plugin / KSP-Hilt here): ConfirmDoseFromReminderUseCase,
    // RoomConfirmationRepository, and AdherenceConfirmationGateway are @Inject-constructor types
    // aggregated into :app's Hilt graph (T-009 :notifications minimal-placement precedent;
    // ADR-083 / ADR-091). There is no broadcast receiver / @AndroidEntryPoint here.
    implementation(libs.hilt.android)

    // Coroutines — the use case and repository are suspend boundaries.
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)

    // Room codegen for the test-only TestConfirmationDatabase (the in-memory DB the AC-003
    // integration test drives). Scoped to the test source set only.
    kspTest(libs.androidx.room.compiler)

    androidTestImplementation(libs.junit.ext)
}

// Kover per-package coverage gate (ADR-086 report-level-filter form; Kover 0.8.x forbids per-rule
// filters). The report-wide filter scopes both the report and the verify gate to the Domain +
// Application layers; @DaggerGenerated factories (should any Hilt codegen land here in future) are
// excluded so they never dilute coverage (ADR-091). The packet's split (domain ≥ 90 %, application
// ≥ 80 %) collapses, under the report-level-filter constraint, to a single ≥ 90 % gate over the
// combined Domain + Application scope — stricter than (and therefore satisfying) the application
// ≥ 80 % floor. infrastructure.* (Room entity/dao/converters/repository, the gateway) carries no %
// threshold and is exercised by the Robolectric integration test (T-010 ADR).
kover {
    reports {
        filters {
            includes {
                classes(
                    "io.nemopill.adherencetracking.domain.*",
                    "io.nemopill.adherencetracking.application.*",
                )
            }
            excludes {
                annotatedBy("dagger.internal.DaggerGenerated")
            }
        }
        verify {
            rule("io.nemopill.adherencetracking domain+application line coverage >= 90%") {
                bound {
                    minValue = 90
                }
            }
        }
    }
}
